package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.network.block.ButterflyLightUpdatePacket;
import com.lx862.jcm.mod.network.block.FareSaverUpdatePacket;
import com.lx862.jcm.mod.network.block.RVPIDSUpdatePacket;
import com.lx862.jcm.mod.network.block.SubsidyMachineUpdatePacket;
import com.lx862.jcm.mod.network.gui.ButterflyLightGUIPacket;
import com.lx862.jcm.mod.network.gui.FareSaverGUIPacket;
import com.lx862.jcm.mod.network.gui.RVPIDSGUIPacket;
import com.lx862.jcm.mod.network.gui.SubsidyMachineGUIPacket;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.PacketBuffer;
import org.mtr.mapping.holder.PlayerEntity;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.registry.Registry;
import org.mtr.mapping.registry.RegistryClient;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Networking {
    private static final List<String> registeredPackets = new ArrayList<>();
    public static void register() {
        JCMLogger.debug("Registering network packets...");
        org.mtr.mapping.registry.Registry.setupPackets(new Identifier(Constants.MOD_ID, "packet"));

        // Block Entity Update
        registerPacket(ButterflyLightUpdatePacket.class, ButterflyLightUpdatePacket::new);
        registerPacket(FareSaverUpdatePacket.class, FareSaverUpdatePacket::new);
        registerPacket(SubsidyMachineUpdatePacket.class, SubsidyMachineUpdatePacket::new);
        registerPacket(RVPIDSUpdatePacket.class, RVPIDSUpdatePacket::new);

        // GUI Screen
        registerPacket(ButterflyLightGUIPacket.class, ButterflyLightGUIPacket::new);
        registerPacket(FareSaverGUIPacket.class, FareSaverGUIPacket::new);
        registerPacket(SubsidyMachineGUIPacket.class, SubsidyMachineGUIPacket::new);
        registerPacket(RVPIDSGUIPacket.class, RVPIDSGUIPacket::new);
    }

    public static void registerClient() {
        org.mtr.mapping.registry.RegistryClient.setupPackets(new Identifier(Constants.MOD_ID, "packet"));
    }

    private static <T extends PacketHandler> void registerPacket(Class<T> classObject, Function<PacketBuffer, T> getInstance) {
        // Keep track of the registered packets, so we can warn if we try to send a non-registered packet
        // I believe Minecraft Mapping itself already keeps track of the registered packets, it just doesn't warn
        registeredPackets.add(classObject.getName());
        org.mtr.mapping.registry.Registry.registerPacket(classObject, getInstance);
    }

    public static <T extends PacketHandler> void sendPacketToServer(T data) {
        if(!registeredPackets.contains(data.getClass().getName())) {
            JCMLogger.warn("Non-registered packets is sent: " + data.getClass().getName());
            JCMLogger.warn("Consider registering in method: mod/registry/Networking.register()");
        }
        RegistryClient.sendPacketToServer(data);
    }
    public static <T extends PacketHandler> void sendPacketToClient(PlayerEntity player, T data) {
        if(!registeredPackets.contains(data.getClass().getName())) {
            JCMLogger.warn("Non-registered packets is sent: " + data.getClass().getName());
            JCMLogger.warn("Consider registering in method: mod/registry/Networking.register()");
        }
        Registry.sendPacketToClient(ServerPlayerEntity.cast(player), data);
    }
}
