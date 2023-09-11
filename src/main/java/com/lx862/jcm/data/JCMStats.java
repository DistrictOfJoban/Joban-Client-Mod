package com.lx862.jcm.data;

public class JCMStats {
    private static int currentGameTick = 0;

    public static void incrementGameTick() {
        currentGameTick++;
    }

    public static int getGameTick() {
        return currentGameTick;
    }
}
