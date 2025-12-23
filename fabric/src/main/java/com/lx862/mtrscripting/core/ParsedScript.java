package com.lx862.mtrscripting.core;

import com.lx862.mtrscripting.ScriptManager;
import com.lx862.mtrscripting.data.ScriptContent;
import com.lx862.mtrscripting.util.*;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.*;
import com.lx862.mtrscripting.wrapper.MinecraftClientWrapper;
import com.lx862.mtrscripting.wrapper.VanillaTextWrapper;
import com.lx862.mtrscripting.wrapper.VoxelShapeWrapper;
import org.mtr.mapping.holder.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Represent a script which has been parsed/executed and ready for functions to be invoked
 */
public class ParsedScript {
    private static final int SCRIPT_RESET_TIME = 4000;
    private final String displayName;
    private final List<Function> createFunctions;
    private final List<Function> renderFunctions;
    private final List<Function> disposeFunctions;
    private final ScriptManager scriptManager;
    private final Scriptable scope;
    private final TimingUtil timingUtil;
    private final ExecutorService executorService;
    private Exception capturedScriptException = null;
    private long lastFailedTime = -1;

    public ParsedScript(ScriptManager scriptManager, String displayName, String contextName, List<ScriptContent> scripts) throws NoSuchMethodException {
        this.timingUtil = new TimingUtil();
        this.displayName = displayName;
        this.scriptManager = scriptManager;
        this.createFunctions = new ArrayList<>();
        this.renderFunctions = new ArrayList<>();
        this.disposeFunctions = new ArrayList<>();
        this.executorService = scriptManager.getDesignatedScriptExecutor();

        try {
            Context cx = Context.enter();
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setClassShutter(scriptManager.getClassShutter());
            scope = new ImporterTopLevel(cx);

            /* Init global variables */
            initBasicContextVariables(scope);
            scriptManager.onParseScript(contextName, cx, scope);

            cx.evaluateString(scope, "\"use strict\";", "", 1, null);

            ScriptResourceUtil.activeContext = cx;
            ScriptResourceUtil.activeScope = scope;
            for(ScriptContent scriptEntry : scripts) {
                final Identifier scriptLocation = scriptEntry.getLocation();
                final String scriptContent = scriptEntry.getContent();

                ScriptResourceUtil.executeScript(cx, scope, scriptLocation, scriptContent);

                tryAndAddFunction("create", scope, createFunctions);
                tryAndAddFunction("render", scope, renderFunctions);
                tryAndAddFunction("dispose", scope, disposeFunctions);
                tryAndAddFunction("create" + contextName, scope, createFunctions);
                tryAndAddFunction("render" + contextName, scope, renderFunctions);
                tryAndAddFunction("dispose" + contextName, scope, disposeFunctions);

                ScriptManager.LOGGER.info("[JCM Scripting] Loaded script: {}:{}", scriptLocation.getNamespace(), scriptLocation.getPath());
            }
        } finally {
            ScriptResourceUtil.activeContext = null;
            ScriptResourceUtil.activeScope = null;
            Context.exit();
        }
    }

    /**
     * Expose basic methods/classes to provide a useful environment for script developer
     */
    private void initBasicContextVariables(Scriptable scope) throws NoSuchMethodException {
        scope.put("include", scope, new NativeJavaMethod(ScriptResourceUtil.class.getMethod("includeScript", Object.class), "includeScript"));
        scope.put("print", scope, new NativeJavaMethod(ScriptResourceUtil.class.getMethod("print", Object[].class), "print"));
        scope.put("Resources", scope, new NativeJavaClass(scope, ScriptResourceUtil.class));
        scope.put("GraphicsTexture", scope, new NativeJavaClass(scope, GraphicsTexture.class));

        #if MC_VERSION < "11701"
        scope.put("Timing", scope, new NativeJavaObject(scope, timingUtil, TimingUtil.class));
        #else
        scope.put("Timing", scope, new NativeJavaObject(scope, timingUtil, com.lx862.mtrscripting.lib.org.mozilla.javascript.lc.type.TypeInfo.OBJECT));
        #endif
        scope.put("StateTracker", scope, new NativeJavaClass(scope, StateTracker.class));
        scope.put("CycleTracker", scope, new NativeJavaClass(scope, CycleTracker.class));
        scope.put("RateLimit", scope, new NativeJavaClass(scope, RateLimit.class));
        scope.put("Networking", scope, new NativeJavaClass(scope, NetworkingUtil.class));
        scope.put("Files", scope, new NativeJavaClass(scope, FilesUtil.class));

        scope.put("Matrices", scope, new NativeJavaClass(scope, Matrices.class));
        scope.put("Vector3f", scope, new NativeJavaClass(scope, ScriptVector3f.class));
        scope.put("VoxelShape", scope, new NativeJavaClass(scope, VoxelShapeWrapper.class));
        scope.put("VanillaText", scope, new NativeJavaClass(scope, VanillaTextWrapper.class));
        scope.put("MinecraftClient", scope, new NativeJavaClass(scope, MinecraftClientWrapper.class));
    }

    private void tryAndAddFunction(String name, Scriptable scope, List<Function> listToAdd) {
        Object func = scope.get(name, scope);
        if(func != Scriptable.NOT_FOUND) {
            if(func instanceof Function) {
                listToAdd.add((Function)func);
            }
            scope.delete(name);
        }
    }

    public Future<?> invokeFunctions(ScriptInstance<?> scriptInstance, List<Function> functions, Runnable finishCallback) {
        if(duringFailCooldown()) {
            return null;
        }

        return scriptManager.submitScriptTask(executorService, () -> {
            if(duringFailCooldown()) return;

            timingUtil.prepareForScript(scriptInstance);
            try {
                Scriptable scope = getScope();
                Context cx = Context.enter();
                cx.setLanguageVersion(Context.VERSION_ES6);
                cx.setClassShutter(scriptManager.getClassShutter());
                if(scriptInstance.state == null) scriptInstance.state = cx.newObject(scope);
                long startTime = System.nanoTime();
                for(Function func : functions) {
                    func.call(cx, scope, scope, new Object[]{scriptInstance.getScriptContext(), scriptInstance.state, scriptInstance.getWrapperObject()});
                }
                scriptInstance.setLastExecutionDurationMs(System.nanoTime() - startTime);
            } catch (Exception e) {
                ScriptManager.LOGGER.error("[JCM Scripting] Error executing script {}!", displayName, e);
                lastFailedTime = System.currentTimeMillis();
                capturedScriptException = e;
            } finally {
                Context.exit();
            }
            finishCallback.run();
        });
    }

    public Future<?> invokeCreateFunctions(ScriptInstance<?> instance, Runnable finishCallback) {
        return invokeFunctions(instance, createFunctions, () -> {
            instance.setCreateFunctionInvoked();
            finishCallback.run();
        });
    }

    public void invokeRenderFunctions(ScriptInstance<?> instance, Runnable finishCallback) {
        if(instance.shouldInvalidate() || instance.scriptTask != null && !instance.scriptTask.isDone()) {
            return;
        }
        if(instance.isCreateFunctionInvoked()) {
            instance.scriptTask = invokeFunctions(instance, renderFunctions, finishCallback);
        } else {
            instance.scriptTask = invokeCreateFunctions(instance, () -> {});
        }
    }

    public void invokeDisposeFunctions(ScriptInstance<?> instance, Runnable finishCallback) {
        invokeFunctions(instance, disposeFunctions, finishCallback);
    }

    /**
     * @return Whether we are currently in the cooldown period after an errored script execution, and script shouldn't be executed.
     */
    public boolean duringFailCooldown() {
        return lastFailedTime != -1 && System.currentTimeMillis() - lastFailedTime <= SCRIPT_RESET_TIME;
    }

    /** Returns the exception occurred during the last failed script execution. Use in combination with {@link ParsedScript#duringFailCooldown()} to ensure the exception is still relevant. */
    public Exception getCapturedScriptException() {
        return capturedScriptException;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public Scriptable getScope() {
        return scope;
    }
}
