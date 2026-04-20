package com.lx862.mtrscripting.core;

import com.lx862.mtrscripting.core.api.MTRClassShutter;
import com.lx862.mtrscripting.core.primitive.ParsedScript;
import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.integration.MinecraftParticleData;
import com.lx862.mtrscripting.core.primitive.ScriptContent;
import com.lx862.mtrscripting.core.util.*;
import com.lx862.mtrscripting.core.util.model.DynamicModelHolderJS;
import com.lx862.mtrscripting.core.util.model.ModelManagerJS;
import com.lx862.mtrscripting.core.util.model.RawMeshBuilderJS;
import com.lx862.mtrscripting.core.util.model.RawModelJS;
import com.lx862.mtrscripting.core.util.render.ModelDrawCall;
import com.lx862.mtrscripting.core.util.render.QuadDrawCall;
import com.lx862.mtrscripting.core.util.sound.TickableSoundInstanceJS;
import com.lx862.mtrscripting.core.integration.MinecraftClientWrapper;
import com.lx862.mtrscripting.core.integration.VanillaTextWrapper;
import com.lx862.mtrscripting.core.integration.VoxelShapeWrapper;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.*;
import com.lx862.mtrscripting.mod.MTRScriptingMod;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;

@ApiInternal
public class ScriptManager {
    private final MTRClassShutter classShutter;
    private final ScriptInstanceManager instanceManager;
    private final List<ExecutorService> scriptExecutors;
    private final Logger logger;
    private final BackgroundWorkerJS backgroundWorker;
    private int scriptExecutorCounter = 0;

    public final ParseScriptEvent<TriConsumer<String, Context, Scriptable>> parseScriptEvent = new ParseScriptEvent<>();
    public final ParseScriptEvent<Consumer<ParsedScript>> finishParseScriptEvent = new ParseScriptEvent<>();

    /**
     * Create a new script manager
     * @param logger - The logger which scripts will use for logging.
     * @param scriptExecutors - A list of ExecutorService in which scripts can execute.
     */
    public ScriptManager(Logger logger, List<ExecutorService> scriptExecutors) {
        if(scriptExecutors == null || scriptExecutors.isEmpty()) throw new IllegalArgumentException("At least 1 script executors must be passed to ScriptManager!");
        MinecraftParticleData.init();
        this.scriptExecutors = scriptExecutors;
        this.instanceManager = new ScriptInstanceManager();
        this.classShutter = new MTRClassShutter();
        this.backgroundWorker = new BackgroundWorkerJS();
        this.logger = logger;
        parseScriptEvent.register(this::addBuiltInTypes);
    }

    public ScriptInstanceManager getInstanceManager() {
        return this.instanceManager;
    }

    /**
     * Obtain the class shutter, this could be used to explicitly allow/deny java package/class accesses for scripts.
     */
    public MTRClassShutter getClassShutter() {
        return this.classShutter;
    }

    public ParsedScript parseScript(String displayName, String contextName, List<ScriptContent> scripts) {
        try {
            ParsedScript parsedScript = new ParsedScript(this, displayName, contextName, scripts);
            finishParseScriptEvent.invoke(e -> e.accept(parsedScript));
            return parsedScript;
        } catch (NoSuchMethodException e) {
            MTRScriptingMod.LOGGER.error("[JCM Scripting] Fatal error: Cannot find required java method to add to script!", e);
            return null;
        }
    }

    public ExecutorService getDesignatedScriptExecutor() {
        ExecutorService executor = scriptExecutors.get(scriptExecutorCounter);
        scriptExecutorCounter = (scriptExecutorCounter + 1) % scriptExecutors.size();
        return executor;
    }

    public Logger getLogger() {
        return this.logger;
    }

    /** Clear all script instances<br>
     * This should be called on resource reload */
    public void reset() {
        instanceManager.reset();
        backgroundWorker.reset();
    }

    /** Submit a task to the script thread executor */
    public Future<?> submitScriptTask(ExecutorService executorService, Runnable runnable) {
        return executorService.submit(runnable);
    }

    /**
     * Expose basic methods/classes to provide a useful environment for script developer
     */
    private void addBuiltInTypes(String contextName, Context cx, Scriptable scope) throws RuntimeException {
        try {
            scope.put("include", scope, new NativeJavaMethod(ScriptResourceUtil.class.getMethod("includeScript", Object.class), "includeScript"));
            scope.put("print", scope, new NativeJavaMethod(ScriptResourceUtil.class.getMethod("print", Object[].class), "print"));
            scope.put("Resources", scope, new NativeJavaClass(scope, ScriptResourceUtil.class));
            scope.put("GraphicsTexture", scope, new NativeJavaClass(scope, GraphicsTexture.class));
            scope.put("console", scope, new NativeJavaObject(scope, new ConsoleJS(), com.lx862.mtrscripting.lib.org.mozilla.javascript.lc.type.TypeInfo.OBJECT));
            scope.put("BackgroundWorker", scope, new NativeJavaObject(scope, backgroundWorker, com.lx862.mtrscripting.lib.org.mozilla.javascript.lc.type.TypeInfo.OBJECT));

            scope.put("StateTracker", scope, new NativeJavaClass(scope, StateTrackerJS.class));
            scope.put("CycleTracker", scope, new NativeJavaClass(scope, CycleTrackerJS.class));
            scope.put("RateLimit", scope, new NativeJavaClass(scope, RateLimitJS.class));
            scope.put("Networking", scope, new NativeJavaClass(scope, NetworkingJS.class));
            scope.put("Files", scope, new NativeJavaClass(scope, FilesJS.class));

            scope.put("Matrices", scope, new NativeJavaClass(scope, Matrices.class));
            scope.put("Vector3f", scope, new NativeJavaClass(scope, ScriptVector3f.class));
            scope.put("VoxelShape", scope, new NativeJavaClass(scope, VoxelShapeWrapper.class));
            scope.put("VanillaText", scope, new NativeJavaClass(scope, VanillaTextWrapper.class));
            scope.put("MinecraftClient", scope, new NativeJavaClass(scope, MinecraftClientWrapper.class));
            scope.put("TickableSoundInstance", scope, new NativeJavaClass(scope, TickableSoundInstanceJS.class));

            scope.put("ModelDrawCall", scope, new NativeJavaClass(scope, ModelDrawCall.class));
            scope.put("QuadDrawCall", scope, new NativeJavaClass(scope, QuadDrawCall.class));
            scope.put("ModelManager", scope, new NativeJavaClass(scope, ModelManagerJS.class));
            scope.put("RawModel", scope, new NativeJavaClass(scope, RawModelJS.class));
            scope.put("RawMeshBuilder", scope, new NativeJavaClass(scope, RawMeshBuilderJS.class));
            scope.put("DynamicModelHolder", scope, new NativeJavaClass(scope, DynamicModelHolderJS.class));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static class ParseScriptEvent<T> {
        private final List<T> callbackQueue;

        private ParseScriptEvent() {
            this.callbackQueue = new ArrayList<>();
        }

        public void register(T callback) {
            this.callbackQueue.add(callback);
        }

        public void invoke(Consumer<T> callback) {
            this.callbackQueue.forEach(callback);
        }
    }
}
