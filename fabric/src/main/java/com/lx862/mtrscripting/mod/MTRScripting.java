package com.lx862.mtrscripting.mod;

import com.lx862.jcm.mod.Constants;
import com.lx862.mtrscripting.api.ScriptingAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mtr.mapping.holder.Identifier;

public class MTRScripting {
    public static final Logger LOGGER = LogManager.getLogger("MTR Scripting via JCM");
    private static final String MOD_ID = "mtrscripting";

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    public static void init() {
        ScriptingAPI.registerAddonVersion(MOD_ID, Constants.MOD_VERSION);
    }
}
