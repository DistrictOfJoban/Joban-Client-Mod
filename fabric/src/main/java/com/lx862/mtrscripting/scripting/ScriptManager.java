package com.lx862.mtrscripting.scripting;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ScriptManager {
    public static final Logger LOGGER = LogManager.getLogger("JCM Scripting");

    public final ScriptInstanceManager instanceManager = new ScriptInstanceManager();
    private ExecutorService scriptThread = Executors.newSingleThreadExecutor();

    public void reset() {
        instanceManager.reset();
        scriptThread.shutdownNow();
        scriptThread = Executors.newSingleThreadExecutor();
    }

    public Future<?> submitScriptTask(Runnable runnable) {
        return scriptThread.submit(runnable);
    }
}
