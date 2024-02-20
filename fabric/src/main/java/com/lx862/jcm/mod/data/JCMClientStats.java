package com.lx862.jcm.mod.data;

/**
 * Dynamic global statistics that JCM keeps track of
 */
public class JCMClientStats {
    private static long timeElapsed;
    private static long lastElapsed = -1;

    public static void incrementGameTick() {
        timeElapsed += lastElapsed == -1 ? (1000 / 20) : System.currentTimeMillis() - lastElapsed;
        lastElapsed = System.currentTimeMillis();
    }

    public static double getGameTick() {
        assert timeElapsed >= 0;
        return timeElapsed / 50.0;
    }
}
