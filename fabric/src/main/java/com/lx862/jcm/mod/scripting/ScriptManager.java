package com.lx862.jcm.mod.scripting;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ScriptManager {
    private static ExecutorService SCRIPT_THREAD = Executors.newSingleThreadExecutor();

    public static void reset() {
        SCRIPT_THREAD.shutdownNow();
        SCRIPT_THREAD = Executors.newSingleThreadExecutor();
    }

    public static Future<?> submitScript(Runnable runnable) {
        return SCRIPT_THREAD.submit(runnable);
    }
}
