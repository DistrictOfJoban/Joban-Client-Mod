package com.lx862.mtrscripting.scripting;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ScriptManager {
    public static final Logger LOGGER = LogManager.getLogger("JCM Scripting");

    private final ScriptInstanceManager instanceManager;
    private ExecutorService scriptThread;

    public ScriptManager() {
        this.instanceManager = new ScriptInstanceManager();
        this.scriptThread = Executors.newSingleThreadExecutor();
    }

    public ScriptInstanceManager getInstanceManager() {
        return this.instanceManager;
    }

    /** Currently this checks and dispose dead script instances (i.e. Those that are no longer active).
     * This should be called from time to time. */
    public void tick() {
        this.instanceManager.clearDeadInstance();
    }

    /** Clear all script instances and restart the script thread executor
     * This should be called on resource reload */
    public void reset() {
        instanceManager.reset();
        scriptThread.shutdownNow();
        scriptThread = Executors.newSingleThreadExecutor();
    }

    /** Submit a task to the script thread executor */
    public Future<?> submitScriptTask(Runnable runnable) {
        return scriptThread.submit(runnable);
    }
}
