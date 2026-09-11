package com.lx862.mtrscripting.mod;

import com.lx862.jcm.mod.JCM;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.mtrscripting.core.api.MTRScriptingAPI;
import com.lx862.mtrscripting.mod.network.packet.EyecandyCustomConfigUpdateC2SPacket;
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
    }
}
