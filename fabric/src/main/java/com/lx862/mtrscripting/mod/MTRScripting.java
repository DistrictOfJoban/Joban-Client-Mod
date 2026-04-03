package com.lx862.mtrscripting.mod;

import org.mtr.mapping.holder.Identifier;

public class MTRScripting {
    private static final String MOD_ID = "mtrscripting";

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}
