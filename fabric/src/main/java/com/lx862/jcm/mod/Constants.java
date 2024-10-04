package com.lx862.jcm.mod;

import org.mtr.mapping.holder.Identifier;

public class Constants {
    public static final String MOD_NAME = "Joban Client Mod";
    public static final String MOD_ID = "jsblock";
    public static final String LOGGING_PREFIX = "[JCM] ";
    public static final String MOD_VERSION = "2.0.0 Beta-4";
    public static final int MC_TICK_PER_SECOND = 20;

    public static Identifier id(String id) {
        return new Identifier(MOD_ID, id);
    }
}
