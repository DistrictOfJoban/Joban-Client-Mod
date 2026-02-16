package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.network.block.*;
import com.lx862.jcm.mod.network.gui.*;
import com.lx862.jcm.mod.network.scripting.StopsDataS2CPacket;
import com.lx862.jcm.mod.network.scripting.MTRDataS2CPacket;
import com.lx862.jcm.mod.network.scripting.RequestStopsDataC2SPacket;
import com.lx862.jcm.mod.network.scripting.RequestMTRDataC2SPacket;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.mapping.holder.PlayerEntity;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Networking {
    private static final List<String> registeredPackets = new ArrayList<>();
    public static void register() {
        JCMLogger.debug("Registering network packets...");
        JCMRegistry.setupPacket();

        // Block Entity Update
        registerPacket(ButterflyLightUpdatePacket.class, ButterflyLightUpdatePacket::new);
        registerPacket(EnquiryUpdateGUIPacket.class, EnquiryUpdateGUIPacket::new);
        registerPacket(FareSaverUpdatePacket.class, FareSaverUpdatePacket::new);
        registerPacket(OperatorButtonUpdatePacket.class, OperatorButtonUpdatePacket::new);
        registerPacket(PIDSUpdatePacket.class, PIDSUpdatePacket::new);
        registerPacket(PIDSProjectorUpdatePacket.class, PIDSProjectorUpdatePacket::new);
        registerPacket(SoundLooperUpdatePacket.class, SoundLooperUpdatePacket::new);
        registerPacket(SubsidyMachineUpdatePacket.class, SubsidyMachineUpdatePacket::new);

        // GUI Screen
        registerPacket(ButterflyLightGUIPacket.class, ButterflyLightGUIPacket::new);
        registerPacket(FareSaverGUIPacket.class, FareSaverGUIPacket::new);
        registerPacket(OperatorButtonGUIPacket.class, OperatorButtonGUIPacket::new);
        registerPacket(PIDSGUIPacket.class, PIDSGUIPacket::new);
        registerPacket(PIDSProjectorGUIPacket.class, PIDSProjectorGUIPacket::new);
        registerPacket(SoundLooperGUIPacket.class, SoundLooperGUIPacket::new);
        registerPacket(SubsidyMachineGUIPacket.class, SubsidyMachineGUIPacket::new);

        // Scripting data
        registerPacket(RequestStopsDataC2SPacket.class, RequestStopsDataC2SPacket::new);
        registerPacket(StopsDataS2CPacket.class, StopsDataS2CPacket::new);
        registerPacket(RequestMTRDataC2SPacket.class, RequestMTRDataC2SPacket::new);
        registerPacket(MTRDataS2CPacket.class, MTRDataS2CPacket::new);
    }

    public static void registerClient() {
        JCMRegistryClient.setupPacketClient();
    }

    private static <T extends PacketHandler> void registerPacket(Class<T> classObject, Function<PacketBufferReceiver, T> getInstance) {
        // Keep track of the registered packets, so we can warn if we try to send a non-registered packet
        // I believe Minecraft Mapping itself already keeps track of the registered packets, it just doesn't warn
        registeredPackets.add(classObject.getName());
        JCMRegistry.registerPacket(classObject, getInstance);
    }

    public static <T extends PacketHandler> void sendPacketToServer(T data) {
        if(!registeredPackets.contains(data.getClass().getName())) {
            JCMLogger.warn("Non-registered packets is sent: " + data.getClass().getName());
            JCMLogger.warn("Consider registering in method: mod/registry/Networking.register()");
        }
        JCMRegistryClient.REGISTRY_CLIENT.sendPacketToServer(data);
    }

    public static <T extends PacketHandler> void sendPacketToClient(PlayerEntity player, T data) {
        if(!registeredPackets.contains(data.getClass().getName())) {
            JCMLogger.warn("Non-registered packets is sent: " + data.getClass().getName());
            JCMLogger.warn("Consider registering in method: mod/registry/Networking.register()");
        }
        JCMRegistry.REGISTRY.sendPacketToClient(ServerPlayerEntity.cast(player), data);
    }
}
