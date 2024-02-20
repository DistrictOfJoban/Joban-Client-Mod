package com.lx862.jcm.mod.data;

/**
 * Dynamic global statistics that JCM keeps track of
 */
public class JCMServerStats {
    private static int currentGameTick = 0;

    public static void incrementGameTick() {
        currentGameTick++;
    }

    public static int getGameTick() {
        return currentGameTick;
    }
}
