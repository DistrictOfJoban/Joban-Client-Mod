package com.lx862.jcm.test;

import com.google.gson.JsonParser;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.mtrscripting.mod.impl.mtr.server.PSDServerBindings;
import org.mtr.mapping.holder.*;
import org.mtr.mod.block.BlockPSDAPGGlassBase;
import java.nio.file.*;
import java.util.Comparator;

public final class PSDServerBindingsTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static int storedCount(Path file) throws Exception {
        return new JsonParser().parse(Files.readString(file)).getAsJsonObject().getAsJsonArray("bindings").size();
    }

    public static void main(String[] args) throws Exception {
        final Path save = Files.createTempDirectory("jcm-psd-bindings-");
        try {
            final MinecraftServer server = new MinecraftServer(save);
            final ServerWorld world = new ServerWorld();
            final ServerPlayerEntity player = new ServerPlayerEntity(new Object(), world);
            world.players.add(player);
            final Path file = save.resolve("mtr/test/world/psd-scripts/bindings.json");
            PSDServerBindings.resetForTest();
            PSDServerBindings.handleQuery(server, player, "../../untrusted");
            PSDServerBindings.handleQuery(server, new ServerPlayerEntity(player.data, world), "test/world");
            Networking.sent.clear();
            for (int i = 0; i < 5; i++) {
                world.blocks.put(new BlockPos(i, 0, 0), new BlockState(new BlockPSDAPGGlassBase()));
                PSDServerBindings.handleBind(server, player, i, 0, 0, "test:script", false, "GLASS");
            }
            check(Networking.sent.isEmpty(), "per-block edits must wait for one coalesced sync");
            check(!Files.exists(file), "per-block edits must not each synchronously rewrite the save");
            PSDServerBindings.onServerTick();
            check(Networking.sent.size() == 1, "one tick must produce one snapshot per online subscriber, despite wrapper aliases");
            check(Networking.sent.get(0).bindings().size() == 5 && storedCount(file) == 5, "coalesced state lost entries");
            Networking.sent.clear();
            PSDServerBindings.handleBind(server, player, 0, 0, 0, "test:script", false, "GLASS");
            PSDServerBindings.onServerTick();
            check(Networking.sent.isEmpty(), "no-op binding resent the dimension snapshot");

            final int reads = world.blockReads;
            PSDServerBindings.handleBind(server, player, 10000, 0, 0, "test:remote", false, "GLASS");
            world.loaded = false;
            PSDServerBindings.handleBind(server, player, 0, 0, 0, "test:unloaded", false, "GLASS");
            world.loaded = true;
            check(world.blockReads == reads, "remote/unloaded request touched block state and could load chunks");
            world.players.clear();
            PSDServerBindings.handleBind(server, player, 0, 0, 0, "", false, "GLASS");
            PSDServerBindings.onServerTick();
            check(Networking.sent.isEmpty(), "disconnected subscribers received a broadcast");
            check(storedCount(file) == 4, "unbinding did not persist");

            // Shutdown between receiving packets and the next tick still saves.
            PSDServerBindings.handleBind(server, player, 1, 0, 0, "", false, "GLASS");
            PSDServerBindings.onServerStopping(server);
            check(storedCount(file) == 3, "shutdown discarded a pending change");
            PSDServerBindings.handleQuery(server, player, "test/world");
            check(Networking.sent.get(0).bindings().size() == 3, "saved state failed to reload");
            PSDServerBindings.onServerStopping(server);
            System.out.println("PSD server regression: batching, persistence/reload, wrapper alias dedupe, disconnect and unloaded/remote guards passed");
        } finally {
            PSDServerBindings.resetForTest();
            try (var paths = Files.walk(save)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }
}
