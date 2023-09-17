package com.lx862.jcm.data;

/**
 * Dynamic statistics that JCM has to keep track of
 */
public class JCMStats {
    private static int currentGameTick = 0;

    public static void incrementGameTick() {
        currentGameTick++;
    }

    public static int getGameTick() {
        return currentGameTick;
    }
}
