package com.lx862.mtrscripting;

import com.lx862.mtrscripting.core.MTRClassShutter;
import com.lx862.mtrscripting.core.ParsedScript;
import com.lx862.mtrscripting.data.ScriptContent;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.Context;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.Scriptable;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class ScriptManager {
    public static final Logger LOGGER = LogManager.getLogger("MTR Scripting via JCM");
    private final ObjectList<TriConsumer<String, Context, Scriptable>> onParseScriptCallback = new ObjectArrayList<>();
    private final MTRClassShutter classShutter;
    private final ScriptInstanceManager instanceManager;
    private final List<ExecutorService> scriptExecutors;

    private int nextScriptExecutor = 0;

    /**
     * Create a new script manager
     * @param scriptExecutors - A list of ExecutorService in which scripts can execute.
     */
    public ScriptManager(List<ExecutorService> scriptExecutors) {
        if(scriptExecutors == null || scriptExecutors.isEmpty()) throw new IllegalArgumentException("At least 1 script executors must be passed to ScriptManager!");
        this.instanceManager = new ScriptInstanceManager();
        this.scriptExecutors = scriptExecutors;
        this.classShutter = new MTRClassShutter();
    }

    public ScriptInstanceManager getInstanceManager() {
        return this.instanceManager;
    }

    public void onParseScript(String contextName, Context context, Scriptable scriptable) {
        for(TriConsumer<String, Context, Scriptable> entry : onParseScriptCallback) {
            entry.accept(contextName, context, scriptable);
        }
    }

    /**
     * Register a callback that will be called when a script is to be parsed.<br>
     * This can be to add new types/objects to the script context.
     * @param callback The callback to run (Context type, Rhino Context, Scriptable)
     */
    public void onParseScript(TriConsumer<String, Context, Scriptable> callback) {
        onParseScriptCallback.add(callback);
    }

    /**
     * Obtain the class shutter, this could be used to explicitly allow/deny java package/class accesses for scripts.
     */
    public MTRClassShutter getClassShutter() {
        return this.classShutter;
    }

    public ParsedScript parseScript(String scriptName, String contextName, List<ScriptContent> scripts) {
        try {
            return new ParsedScript(this, scriptName, contextName, scripts);
        } catch (NoSuchMethodException e) {
            ScriptManager.LOGGER.error("[JCM Scripting] Fatal error: Cannot find required java method to add to script!", e);
            return null;
        }
    }

    public ExecutorService getDesignatedScriptExecutor() {
        ExecutorService executor = scriptExecutors.get(nextScriptExecutor);
        nextScriptExecutor = (nextScriptExecutor + 1) % scriptExecutors.size();
        return executor;
    }

    /** Clear all script instances<br>
     * This should be called on resource reload */
    public void reset() {
        instanceManager.reset();
    }

    /** Submit a task to the script thread executor */
    public Future<?> submitScriptTask(ExecutorService executorService, Runnable runnable) {
        return executorService.submit(runnable);
    }
}
