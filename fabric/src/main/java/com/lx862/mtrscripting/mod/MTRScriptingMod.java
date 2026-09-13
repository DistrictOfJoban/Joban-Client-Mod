package com.lx862.mtrscripting.mod;

import com.lx862.jcm.mod.JCM;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.mtrscripting.core.api.MTRScriptingAPI;
import com.lx862.mtrscripting.mod.network.packet.EyecandyCustomConfigUpdateC2SPacket;
import com.lx862.mtrscripting.mod.network.packet.data.MTRDataS2CPacket;
import com.lx862.mtrscripting.mod.network.packet.data.RequestMTRDataC2SPacket;
import com.lx862.mtrscripting.mod.network.packet.data.RequestStopsDataC2SPacket;
import com.lx862.mtrscripting.mod.network.packet.data.StopsDataS2CPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mtr.mapping.holder.Identifier;

public class MTRScriptingMod {
    public static final Logger LOGGER = LogManager.getLogger("MTR Scripting via JCM");
    private static final String MOD_ID = "mtrscripting";

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    public static void init() {
        MTRScriptingAPI.registerAddonVersion(MOD_ID, JCM.buildMetadata.version);

        // Register packet
        Networking.registerPacket(EyecandyCustomConfigUpdateC2SPacket.class, EyecandyCustomConfigUpdateC2SPacket::new);
        Networking.registerPacket(RequestStopsDataC2SPacket.class, RequestStopsDataC2SPacket::new);
        Networking.registerPacket(StopsDataS2CPacket.class, StopsDataS2CPacket::new);
        Networking.registerPacket(RequestMTRDataC2SPacket.class, RequestMTRDataC2SPacket::new);
        Networking.registerPacket(MTRDataS2CPacket.class, MTRDataS2CPacket::new);
    }
}
