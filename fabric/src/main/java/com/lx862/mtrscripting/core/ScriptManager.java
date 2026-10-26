package com.lx862.mtrscripting.core;

import com.lx862.mtrscripting.core.api.MTRClassShutter;
import com.lx862.mtrscripting.core.api.MTRScriptingAPI;
import com.lx862.mtrscripting.core.api.ScriptingLanguage;
import com.lx862.mtrscripting.core.primitive.ParsedScript;
import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.integration.MinecraftParticleData;
import com.lx862.mtrscripting.core.util.*;
import com.lx862.mtrscripting.core.util.ante.ANTEComponentUtilJS;
import com.lx862.mtrscripting.core.util.ante.ANTEGlobalRegisterJS;
import com.lx862.mtrscripting.core.util.ante.ANTESoundHelperJS;
import com.lx862.mtrscripting.core.util.ante.ANTETickableSoundJS;
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
import com.lx862.mtrscripting.mod.gui.hud.ScriptErrorNotifier;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@ApiInternal
public class ScriptManager {
    private final MTRClassShutter classShutter;
    private final ScriptInstanceManager instanceManager;
    private final List<ExecutorService> scriptExecutors;
    private final Logger logger;
    private final BackgroundWorkerJS backgroundWorker;
    private int scriptExecutorCounter = 0;

    public final ParseScriptEvent<BiConsumer<String, ParsedScript>> parseScriptEvent = new ParseScriptEvent<>();
    public final ParseScriptEvent<Consumer<ParsedScript>> finishParseScriptEvent = new ParseScriptEvent<>();
    public final ScriptErrorNotifier scriptErrorNotifier = new ScriptErrorNotifier();

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
     * Obtain the class restrictor, this could be used to explicitly allow/deny java package/class accesses for scripts.
     */
    public MTRClassShutter getClassShutter() {
        return this.classShutter;
    }

    public ParsedScript parseScript(String displayName, String contextName, ParsedScript.Configuration scriptConfiguration, boolean verboseLogging) {
        ScriptingLanguage lang = MTRScriptingAPI.getScriptingLanguage(scriptConfiguration.scriptingLanguage());
        if(lang == null) throw new IllegalArgumentException("Cannot parse unknown programming language '" + scriptConfiguration.scriptingLanguage() + "'!");

        ParsedScript parsedScript = lang.createParsedScript(this, displayName, contextName, scriptConfiguration).parse(verboseLogging);
        finishParseScriptEvent.invoke(e -> e.accept(parsedScript));
        return parsedScript;
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
    private void addBuiltInTypes(String contextName, ParsedScript parsedScript) throws RuntimeException {
        try {
            parsedScript.exposeClassMethod("include", ScriptResourceUtil.class.getMethod("includeScript", Object.class));
            parsedScript.exposeClassMethod("print", ScriptResourceUtil.class.getMethod("print", Object[].class));
            parsedScript.exposeClass("Resources", ScriptResourceUtil.class);
            parsedScript.exposeClass("GraphicsTexture", GraphicsTexture.class);
            parsedScript.exposeClass("Java", JavaUtilJS.class);
            parsedScript.exposeClassInstance("console", new ConsoleJS());
            parsedScript.exposeClassInstance("BackgroundWorker", backgroundWorker);

            parsedScript.exposeClass("StateTracker", StateTrackerJS.class);
            parsedScript.exposeClass("CycleTracker", CycleTrackerJS.class);
            parsedScript.exposeClass("RateLimit", RateLimitJS.class);
            parsedScript.exposeClass("Networking", NetworkingJS.class);
            parsedScript.exposeClass("Files", FilesJS.class);

            parsedScript.exposeClass("Matrices", Matrices.class);
            parsedScript.exposeClass("Vector3f", ScriptVector3f.class);
            parsedScript.exposeClass("VoxelShape", VoxelShapeWrapper.class);
            parsedScript.exposeClass("VanillaText", VanillaTextWrapper.class);
            parsedScript.exposeClass("MinecraftClient", MinecraftClientWrapper.class);
            parsedScript.exposeClass("TickableSoundInstance", TickableSoundInstanceJS.class);

            parsedScript.exposeClass("ModelDrawCall", ModelDrawCall.class);
            parsedScript.exposeClass("QuadDrawCall", QuadDrawCall.class);
            parsedScript.exposeClass("ModelManager", ModelManagerJS.class);
            parsedScript.exposeClass("RawModel", RawModelJS.class);
            parsedScript.exposeClass("RawMeshBuilder", RawMeshBuilderJS.class);
            parsedScript.exposeClass("DynamicModelHolder", DynamicModelHolderJS.class);

            /* ANTE */
            parsedScript.exposeClass("SoundHelper", ANTESoundHelperJS.class);
            parsedScript.exposeClass("TickableSound", ANTETickableSoundJS.class);
            parsedScript.exposeClass("GlobalRegister", ANTEGlobalRegisterJS.class);
            parsedScript.exposeClass("ComponentUtil", ANTEComponentUtilJS.class);
            parsedScript.exposeClassMethod("asJavaArray", JavaUtilJS.class.getMethod("toArray", List.class, Class.class));
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
