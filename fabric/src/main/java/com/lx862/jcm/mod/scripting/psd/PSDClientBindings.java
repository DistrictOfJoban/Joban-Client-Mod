package com.lx862.jcm.mod.scripting.psd;

import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.mtrscripting.mod.network.packet.psd.RequestPSDScriptsC2SPacket;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.World;
import org.mtr.mod.Init;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Client-side cache of the server's PSD script bindings for the CURRENT
 * dimension. Re-requested on join and on every dimension switch; a bind or
 * unbind from any player in the same dimension arrives as a full replacement
 * ({@code PSDScriptsS2CPacket}).
 *
 * <p>All access is on the main client thread (packet handlers, item use and
 * the render pass all run there).</p>
 */
public final class PSDClientBindings {

    private static final long RE_REQUEST_MILLIS = 10_000;

    private static final Map<BlockPos, PSDBinding> BINDINGS = new HashMap<>();
    private static String currentDimension;
    /* Raw world reference for identity-only change detection — getWorldMapped()
       allocates a wrapper and Init.getWorldId formats a string, neither of
       which belongs in a per-tick check. */
    private static Object lastWorldData;
    private static long lastRequestAt;
    private static boolean awaitingAnswer;

    private PSDClientBindings() {
    }

    /** Applies a full replacement answer from the server (main client thread). */
    public static void acceptAll(String dimension, List<PSDBinding> bindings) {
        if (dimension == null || !dimension.equals(currentDimension)) return; // stale answer for another dimension
        awaitingAnswer = false;
        BINDINGS.clear();
        for (PSDBinding binding : bindings) {
            BINDINGS.put(binding.pos, binding);
        }
    }

    /** Optimistic local apply after the editor sends a bind packet — the
        server's authoritative broadcast replaces it moments later. */
    public static void acceptLocalBind(BlockPos pos, String scriptId, boolean pidsPreset, PSDWrapper.PSDType type) {
        if (scriptId == null || scriptId.isEmpty()) {
            BINDINGS.remove(pos);
        } else {
            BINDINGS.put(pos, new PSDBinding(pos, scriptId, pidsPreset, type));
        }
    }

    /** The binding at a position, or null. */
    public static PSDBinding get(BlockPos pos) {
        return BINDINGS.get(pos);
    }

    /** True when the position's binding still runs the given script id (used by instance invalidation). */
    public static boolean isSameBinding(BlockPos pos, String scriptId) {
        final PSDBinding binding = BINDINGS.get(pos);
        return binding != null && binding.scriptId.equals(scriptId);
    }

    /** Iterates the current bindings without copying — the render pass calls
        this every frame. Main client thread only. */
    public static void forEach(Consumer<PSDBinding> consumer) {
        for (PSDBinding binding : BINDINGS.values()) {
            consumer.accept(binding);
        }
    }

    /** All current bindings (GUI use; allocates). */
    public static List<PSDBinding> all() {
        return new ArrayList<>(BINDINGS.values());
    }

    public static boolean isEmpty() {
        return BINDINGS.isEmpty();
    }

    /** Client tick: requests the current dimension's bindings on join, on
        dimension switch, and retries when no server ever answered (a vanilla
        server without this mod simply never replies). */
    public static void tick() {
        final World world = MinecraftClient.getInstance().getWorldMapped();
        if (world == null) {
            if (currentDimension != null || !BINDINGS.isEmpty()) {
                currentDimension = null;
                lastWorldData = null;
                BINDINGS.clear();
                awaitingAnswer = false;
            }
            return;
        }

        if (world.data != lastWorldData) {
            lastWorldData = world.data;
            currentDimension = Init.getWorldId(world);
            BINDINGS.clear();
            request(currentDimension);
            return;
        }

        final long now = System.currentTimeMillis();
        if (awaitingAnswer && now - lastRequestAt >= RE_REQUEST_MILLIS) {
            request(currentDimension);
        }
    }

    private static void request(String dimension) {
        lastRequestAt = System.currentTimeMillis();
        awaitingAnswer = true;
        try {
            com.lx862.jcm.mod.registry.Networking.sendPacketToServer(new RequestPSDScriptsC2SPacket(dimension));
        } catch (Exception e) {
            awaitingAnswer = false;
            JCMLogger.warn("Could not request PSD script bindings: {}", e.getMessage());
        }
    }

    public static void reset() {
        BINDINGS.clear();
        currentDimension = null;
        lastWorldData = null;
        awaitingAnswer = false;
    }
}
