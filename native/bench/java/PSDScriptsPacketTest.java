package com.lx862.jcm.test;

import com.lx862.jcm.mod.scripting.psd.*;
import com.lx862.mtrscripting.mod.network.packet.psd.PSDScriptsS2CPacket;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.*;
import java.util.*;

/** Tests the real packet codec and client replacement with MTR's UTF-16 wire. */
public final class PSDScriptsPacketTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        final MinecraftClient client = MinecraftClient.getInstance();
        client.world = new ClientWorld();
        PSDClientBindings.reset();
        PSDClientBindings.tick();
        final List<PSDBinding> bindings = new ArrayList<>();
        final String scriptId = "晏阳:屏蔽门🚉";
        for (int i = 0; i < 70_000; i++) {
            bindings.add(new PSDBinding(new BlockPos(i, 10, 0), scriptId, (i & 1) == 0, PSDWrapper.PSDType.GLASS));
        }
        final PacketBufferSender sender = new PacketBufferSender();
        new PSDScriptsS2CPacket("test/world", bindings).write(sender);
        check(sender.bytes().length > 1_048_576, "large-message fixture must cross a vanilla payload boundary");
        new PSDScriptsS2CPacket(new PacketBufferReceiver(sender.bytes())).runClient();
        check(PSDClientBindings.all().size() == 70_000, "legitimate large dimension snapshot was truncated or capped");
        final PSDBinding last = PSDClientBindings.get(new BlockPos(69_999, 10, 0));
        check(last != null && scriptId.equals(last.scriptId) && !last.pidsPreset, "UTF-16 supplementary characters or binding fields changed");
        final PacketBufferSender empty = new PacketBufferSender();
        new PSDScriptsS2CPacket("test/world", Collections.emptyList()).write(empty);
        new PSDScriptsS2CPacket(new PacketBufferReceiver(empty.bytes())).runClient();
        check(PSDClientBindings.isEmpty(), "empty full replacement retained removed bindings");
        final PacketBufferSender invalid = new PacketBufferSender();
        invalid.writeString("test/world");
        invalid.writeInt(-1);
        boolean rejected = false;
        try {
            new PSDScriptsS2CPacket(new PacketBufferReceiver(invalid.bytes()));
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        check(rejected, "negative count must not be accepted as an empty authoritative snapshot");
        System.out.println("PSD packet regression: 70,000 bindings, >1MiB logical payload, UTF-16 strings, empty replacement and negative-count rejection passed");
    }
}
