package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.network.block.SubsidyMachineUpdatePacket;
import com.lx862.jcm.mod.network.gui.DemoScreenPacket;
import com.lx862.jcm.mod.network.gui.SubsidyMachineScreenPacket;
import org.mtr.mapping.holder.Identifier;

public class Packets {
    public static void register() {
        org.mtr.mapping.registry.Registry.setupPackets(new Identifier(Constants.MOD_ID, "packet"));

        // Block Entity Update
        org.mtr.mapping.registry.Registry.registerPacket(SubsidyMachineUpdatePacket.class, SubsidyMachineUpdatePacket::new);

        // GUI Screen
        org.mtr.mapping.registry.Registry.registerPacket(DemoScreenPacket.class, DemoScreenPacket::new);
        org.mtr.mapping.registry.Registry.registerPacket(SubsidyMachineScreenPacket.class, SubsidyMachineScreenPacket::new);
    }

    public static void registerClient() {
        org.mtr.mapping.registry.RegistryClient.setupPackets(new Identifier(Constants.MOD_ID, "packet"));
    }
}
