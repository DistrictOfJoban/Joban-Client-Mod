package com.lx862.mtrscripting.mod.impl.mtr.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lx862.jcm.mod.registry.Items;
import com.lx862.jcm.mod.scripting.psd.PSDBinding;
import com.lx862.jcm.mod.scripting.psd.PSDWrapper;
import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.mtrscripting.mod.network.packet.psd.PSDScriptsS2CPacket;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.PlayerEntity;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.holder.WorldSavePath;
import org.mtr.mod.Init;
import org.mtr.mod.block.BlockPSDAPGGlassBase;
import org.mtr.mod.block.BlockPSDTop;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The server half of the PSD script binding feature — storage, validation
 * and synchronization of "this PSD glass/top block runs that script".
 *
 * <h2>Storage</h2>
 * Per dimension, beside MTR's own data (same layout reasoning as
 * {@link DoorDirectionManager} — never inside MTR's purged {@code routes/}
 * folder):
 * <pre>
 *     &lt;world&gt;/mtr/&lt;dimension&gt;/psd-scripts/bindings.json
 * </pre>
 *
 * <h2>Model</h2>
 * The binding list is authoritative on the server; clients get the full list
 * of their current dimension on request (join / dimension switch) and after
 * every change. Binding requires holding the PSD Script Editor item, and the
 * block must actually be a PSD glass or top block of the matching surface —
 * the client's GUI already filters by surface, the server re-checks so a
 * hand-crafted packet cannot bind scripts to arbitrary blocks.</p>
 *
 * <p>All entry points run on the server thread (the packets submit onto it,
 * the world tick and stop hooks are already there).</p>
 */
public final class PSDServerBindings {

    private static final Map<String, DimensionState> DIMENSIONS = new HashMap<>();
    /* Players that asked for a dimension's bindings (join / dimension
       switch) — the broadcast targets. Refreshed on every request; stale
       entries (player left) are individually try-caught and the whole map
       drops on server stop. */
    private static final Map<String, List<ServerPlayerEntity>> SUBSCRIBERS = new HashMap<>();

    private PSDServerBindings() {
    }

    private static final class DimensionState {
        final String dimension;
        final Path file;
        final Map<BlockPos, PSDBinding> bindings = new HashMap<>();
        boolean dirty;

        DimensionState(String dimension, Path file) {
            this.dimension = dimension;
            this.file = file;
        }
    }

    /* ==================================================================== */
    /* Lifecycle                                                             */
    /* ==================================================================== */

    public static void onServerStopping(MinecraftServer minecraftServer) {
        for (DimensionState state : DIMENSIONS.values()) {
            if (state.dirty) save(state);
        }
        DIMENSIONS.clear();
        SUBSCRIBERS.clear();
    }

    /* ==================================================================== */
    /* Packet handlers (server thread)                                       */
    /* ==================================================================== */

    public static void handleQuery(MinecraftServer minecraftServer, ServerPlayerEntity player, String requestedDimension) {
        /* SECURITY: never trust the client-supplied dimension string — it is
           only used to build the storage path. The answer always describes
           the player's ACTUAL current dimension, derived server-side. */
        final String dimension = Init.getWorldId(player.getEntityWorld());
        final DimensionState state = ensureLoaded(minecraftServer, dimension);
        final List<ServerPlayerEntity> subscribers = SUBSCRIBERS.computeIfAbsent(dimension, key -> new ArrayList<>());
        if (!subscribers.contains(player)) {
            subscribers.add(player);
        }
        send(minecraftServer, PlayerEntity.cast(player), new PSDScriptsS2CPacket(dimension, snapshot(state)));
    }

    public static void handleBind(MinecraftServer minecraftServer, ServerPlayerEntity player, int x, int y, int z, String scriptId, boolean pidsPreset, String typeName) {
        final World world = player.getEntityWorld();
        final String dimension = Init.getWorldId(world);

        // The editor item is the permission token — it only exists as a creative tool.
        if (!player.isHolding(Items.PSD_SCRIPT_EDITOR.get())) {
            JCMLogger.warn("Player {} tried to bind a PSD script without holding the PSD Script Editor", player.getName().getString());
            return;
        }

        final PSDWrapper.PSDType type;
        try {
            type = PSDWrapper.PSDType.valueOf(typeName);
        } catch (IllegalArgumentException invalidType) {
            return;
        }

        final BlockPos pos = new BlockPos(x, y, z);
        final BlockState state = world.getBlockState(pos);
        final Object block = state.getBlock().data;
        final boolean blockMatches = type == PSDWrapper.PSDType.TOP
                ? block instanceof BlockPSDTop
                : block instanceof BlockPSDAPGGlassBase; // glass + glass end, PSD and APG alike
        if (!blockMatches) {
            return;
        }

        final DimensionState dimensionState = ensureLoaded(minecraftServer, dimension);
        if (scriptId == null || scriptId.isEmpty()) {
            dimensionState.bindings.remove(pos);
        } else {
            dimensionState.bindings.put(pos, new PSDBinding(pos, scriptId, pidsPreset, type));
        }
        dimensionState.dirty = true;
        save(dimensionState);
        broadcast(dimension, dimensionState);
    }

    /* ==================================================================== */
    /* Sync                                                                  */
    /* ==================================================================== */

    static void send(MinecraftServer minecraftServer, PlayerEntity player, PSDScriptsS2CPacket packet) {
        com.lx862.jcm.mod.registry.Networking.sendPacketToClient(player, packet);
    }

    private static void broadcast(String dimension, DimensionState state) {
        final List<ServerPlayerEntity> subscribers = SUBSCRIBERS.get(dimension);
        if (subscribers == null || subscribers.isEmpty()) return;
        final PSDScriptsS2CPacket packet = new PSDScriptsS2CPacket(dimension, snapshot(state));
        for (ServerPlayerEntity player : subscribers) {
            try {
                if (Init.getWorldId(player.getEntityWorld()).equals(dimension)) {
                    com.lx862.jcm.mod.registry.Networking.sendPacketToClient(PlayerEntity.cast(player), packet);
                }
            } catch (Throwable ignored) {
                // Player disconnected between subscribe and broadcast.
            }
        }
    }

    private static List<PSDBinding> snapshot(DimensionState state) {
        return new ArrayList<>(state.bindings.values());
    }

    /* ==================================================================== */
    /* Storage                                                               */
    /* ==================================================================== */

    private static DimensionState ensureLoaded(MinecraftServer minecraftServer, String dimension) {
        DimensionState state = DIMENSIONS.get(dimension);
        if (state == null) {
            final Path file = minecraftServer.getSavePath(WorldSavePath.getRootMapped())
                    .resolve("mtr").resolve(dimension).resolve("psd-scripts").resolve("bindings.json");
            state = new DimensionState(dimension, file);
            load(state);
            DIMENSIONS.put(dimension, state);
        }
        return state;
    }

    private static void load(DimensionState state) {
        if (!Files.isRegularFile(state.file)) return;
        try (BufferedReader reader = Files.newBufferedReader(state.file, StandardCharsets.UTF_8)) {
            final JsonElement rootElement = new JsonParser().parse(reader);
            if (!rootElement.isJsonObject()) return;
            final JsonElement bindingsElement = rootElement.getAsJsonObject().get("bindings");
            if (bindingsElement == null || !bindingsElement.isJsonArray()) return;

            for (JsonElement entryElement : bindingsElement.getAsJsonArray()) {
                try {
                    final JsonObject entry = entryElement.getAsJsonObject();
                    final BlockPos pos = new BlockPos(
                            entry.get("x").getAsInt(),
                            entry.get("y").getAsInt(),
                            entry.get("z").getAsInt());
                    final String scriptId = entry.get("script").getAsString();
                    final boolean pidsPreset = entry.has("pids") && entry.get("pids").getAsBoolean();
                    final PSDWrapper.PSDType type = PSDWrapper.PSDType.valueOf(entry.get("type").getAsString());
                    state.bindings.put(pos, new PSDBinding(pos, scriptId, pidsPreset, type));
                } catch (Exception e) {
                    JCMLogger.warn("Skipping malformed PSD script binding entry: {}", e.getMessage());
                }
            }
            JCMLogger.info("Loaded {} PSD script binding(s) for dimension {}", state.bindings.size(), state.dimension);
        } catch (Exception e) {
            JCMLogger.warn("Could not load PSD script bindings from {}: {}", state.file, e.getMessage());
        }
    }

    private static void save(DimensionState state) {
        try {
            Files.createDirectories(state.file.getParent());
            final JsonObject root = new JsonObject();
            final JsonArray bindings = new JsonArray();
            for (PSDBinding binding : state.bindings.values()) {
                final JsonObject entry = new JsonObject();
                entry.addProperty("x", binding.pos.getX());
                entry.addProperty("y", binding.pos.getY());
                entry.addProperty("z", binding.pos.getZ());
                entry.addProperty("script", binding.scriptId);
                entry.addProperty("pids", binding.pidsPreset);
                entry.addProperty("type", binding.type.name());
                bindings.add(entry);
            }
            root.add("bindings", bindings);

            final Path tmp = state.file.resolveSibling(state.file.getFileName() + ".tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                writer.write(root.toString());
            }
            Files.move(tmp, state.file, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            state.dirty = false;
        } catch (IOException e) {
            JCMLogger.warn("Could not save PSD script bindings to {}: {}", state.file, e.getMessage());
        }
    }

    /** Test/integration hook: forget all in-memory dimension state. */
    public static void resetForTest() {
        DIMENSIONS.clear();
    }
}
