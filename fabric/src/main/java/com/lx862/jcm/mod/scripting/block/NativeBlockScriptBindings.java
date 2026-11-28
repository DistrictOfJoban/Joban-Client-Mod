package com.lx862.jcm.mod.scripting.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.jcm.nativeapi.NativeScriptManager;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.ClientWorld;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mod.Init;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NativeBlockScriptBindings — client-local bindings between block
 * positions and the three ABI 16 block-script kinds (level crossing /
 * fare machine / announcement), edited with the PSD Script Editor item
 * (right-click a FangSu crossing / ticket machine / speaker block).
 *
 * <p>Unlike the PSD bindings (server-stored, packet-synced) these are a
 * <b>rendering concern only</b>: the scripts draw extra models and play
 * sounds around the anchor blocks, nothing about the world state
 * changes. A per-dimension JSON file under
 * {@code <config>/jcm/native_block_scripts.json} is therefore the whole
 * persistence story — installed per player exactly like any other
 * client-side visual preference.</p>
 *
 * <p>All access is on the main client thread (editor GUI, driver tick).</p>
 */
public final class NativeBlockScriptBindings {

    public static final String KIND_LEVEL_CROSSING = "level_crossing";
    public static final String KIND_FARE_MACHINE = "fare_machine";
    public static final String KIND_ANNOUNCEMENT = "announcement";

    /** Script id cap, matching PSDBinding's storage bound. */
    public static final int MAX_SCRIPT_ID_LENGTH = 256;

    public record Binding(BlockPos pos, String kind, String scriptId) {
    }

    private static final Map<String, Map<BlockPos, Binding>> BY_DIMENSION = new HashMap<>();
    private static String currentDimension;
    private static Object lastWorldData;
    private static boolean dirty;

    private NativeBlockScriptBindings() {
    }

    /** The bindings of the current dimension, live view (never null). */
    public static List<Binding> all() {
        final Map<BlockPos, Binding> map = current();
        synchronized (BY_DIMENSION) {
            return new ArrayList<>(map.values());
        }
    }

    public static Binding get(BlockPos pos) {
        final Map<BlockPos, Binding> map = current();
        synchronized (BY_DIMENSION) {
            return map.get(pos);
        }
    }

    /** Bind (or unbind with an empty/invalid id) one block position. */
    public static void set(BlockPos pos, String kind, String scriptId) {
        final Map<BlockPos, Binding> map = current();
        synchronized (BY_DIMENSION) {
            if (scriptId == null || scriptId.isEmpty() || scriptId.length() > MAX_SCRIPT_ID_LENGTH) {
                map.remove(pos);
            } else {
                map.put(pos, new Binding(pos, kind, scriptId));
            }
            dirty = true;
        }
        saveIfDirty();
    }

    /** Public facade over the FangSu anchor probe: 0 = not an anchor,
     *  1 = level crossing, 2 = fare machine, 3 = announcement speaker.
     *  Always 0 without the FangSu addon. */
    public static int anchorKindOf(BlockPos pos) {
        return FangSuBlockProbe.anchorKindOf(pos);
    }

    /* ------------------------------------------------------------------ */

    private static Map<BlockPos, Binding> current() {
        final String dimension = currentDimension != null ? currentDimension : "";
        synchronized (BY_DIMENSION) {
            return BY_DIMENSION.computeIfAbsent(dimension, k -> new HashMap<>());
        }
    }

    /** Client tick: dimension switching reloads the file lazily. */
    public static void tick() {
        final ClientWorld clientWorld = MinecraftClient.getInstance().getWorldMapped();
        if (clientWorld == null) {
            if (currentDimension != null) {
                currentDimension = null;
                lastWorldData = null;
            }
            saveIfDirty();
            return;
        }
        if (clientWorld.data != lastWorldData) {
            lastWorldData = clientWorld.data;
            saveIfDirty();
            currentDimension = Init.getWorldId(org.mtr.mapping.holder.World.cast(clientWorld));
            load();
        }
    }

    /* ------------------------------------------------------------------ */

    private static Path file() {
        final Path dir = com.lx862.jcm.mapping.LoaderImpl.getConfigPath().resolve("jcm");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            JCMLogger.warn("Could not create {}: {}", dir, e.getMessage());
        }
        return dir.resolve("native_block_scripts.json");
    }

    private static void load() {
        final Path file = file();
        if (!Files.isRegularFile(file)) return;
        try {
            final String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            final JsonObject root = parseJson(text).getAsJsonObject();
            synchronized (BY_DIMENSION) {
                for (Map.Entry<String, JsonElement> dimensionEntry : root.entrySet()) {
                    final Map<BlockPos, Binding> map = new HashMap<>();
                    for (JsonElement element : dimensionEntry.getValue().getAsJsonArray()) {
                        try {
                            final JsonObject entry = element.getAsJsonObject();
                            final BlockPos pos = new BlockPos(
                                    entry.get("x").getAsInt(),
                                    entry.get("y").getAsInt(),
                                    entry.get("z").getAsInt());
                            final String kind = entry.get("kind").getAsString();
                            final String scriptId = entry.get("script").getAsString();
                            /* The id may resolve only after a resource pack
                               reload — accept it now, the driver skips ids
                               with no loaded modules. */
                            if (validKind(kind) && !scriptId.isEmpty()) {
                                map.put(pos, new Binding(pos, kind, scriptId));
                            }
                        } catch (RuntimeException ignored) {
                            /* Skip malformed entries — a hand-edited file
                               must not take the whole driver down. */
                        }
                    }
                    BY_DIMENSION.put(dimensionEntry.getKey(), map);
                }
            }
        } catch (Exception e) {
            JCMLogger.warn("Could not load native block script bindings: {}", e.getMessage());
        }
    }

    private static boolean validKind(String kind) {
        return KIND_LEVEL_CROSSING.equals(kind) || KIND_FARE_MACHINE.equals(kind) || KIND_ANNOUNCEMENT.equals(kind);
    }

    /* MC 1.17.1 bundles Gson 2.8.0, which predates the static
     * JsonParser.parseString (added in Gson 2.8.6); every other target
     * version ships a newer Gson. Manifold picks the right call per
     * version — same pattern as NativeApiSession.parseJson. */
    private static JsonElement parseJson(String json) {
        #if MC_VERSION <= "11701"
        return new JsonParser().parse(json);
        #else
        return JsonParser.parseString(json);
        #endif
    }

    private static void saveIfDirty() {
        if (!dirty) return;
        dirty = false;
        final JsonObject root = new JsonObject();
        synchronized (BY_DIMENSION) {
            for (Map.Entry<String, Map<BlockPos, Binding>> dimensionEntry : BY_DIMENSION.entrySet()) {
                final JsonArray array = new JsonArray();
                for (Binding binding : dimensionEntry.getValue().values()) {
                    final JsonObject entry = new JsonObject();
                    entry.addProperty("x", binding.pos.getX());
                    entry.addProperty("y", binding.pos.getY());
                    entry.addProperty("z", binding.pos.getZ());
                    entry.addProperty("kind", binding.kind);
                    entry.addProperty("script", binding.scriptId);
                    array.add(entry);
                }
                root.add(dimensionEntry.getKey(), array);
            }
        }
        try {
            Files.write(file(), root.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            JCMLogger.warn("Could not save native block script bindings: {}", e.getMessage());
        }
    }

    /** Runtime reset (native reload) — keeps the bindings, drops nothing. */
    public static void reset() {
        saveIfDirty();
    }
}
