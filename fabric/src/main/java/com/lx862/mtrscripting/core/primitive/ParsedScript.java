package com.lx862.mtrscripting.core.primitive;

import com.lx862.jcm.mod.config.JCMClientConfig;
import com.lx862.mtrscripting.core.ScriptManager;
import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.util.*;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.*;
import org.mtr.mapping.holder.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Represent a script which has been parsed/executed and ready for functions to be invoked.
 */
@ApiInternal
public class ParsedScript {
    private static final int SCRIPT_RESET_TIME = 4000;

    private final String displayName;
    private final ScriptManager scriptManager;
    private final List<Function> createFunctions;
    private final List<Function> renderFunctions;
    private final List<Function> disposeFunctions;
    private final Scriptable scope;
    private final TimingJS timing;
    private final ExecutorService executorService;
    private final List<ScriptContent> scriptContents;
    private Exception capturedScriptException = null;
    private long lastFailedTime = -1;

    public ParsedScript(ScriptManager scriptManager, String displayName, String contextName, List<ScriptContent> scriptContents) throws NoSuchMethodException {
        this.displayName = displayName;
        this.scriptManager = scriptManager;
        this.scriptContents = scriptContents;
        this.timing = new TimingJS();
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
            ScriptResourceUtil.beginParseScript(cx, scope);

            scriptManager.parseScriptEvent.invoke(e -> e.accept(contextName, cx, scope));
            cx.evaluateString(scope, "\"use strict\";", "", 1, null);
            scope.put("Timing", scope, new NativeJavaObject(scope, timing, com.lx862.mtrscripting.lib.org.mozilla.javascript.lc.type.TypeInfo.OBJECT));

            for(ScriptContent scriptEntry : scriptContents) {
                final Identifier scriptLocation = scriptEntry.location();
                final String scriptContent = scriptEntry.content();

                ScriptResourceUtil.executeScript(cx, scope, scriptLocation, scriptContent);

                tryAndAddFunction("create", scope, createFunctions);
                tryAndAddFunction("render", scope, renderFunctions);
                tryAndAddFunction("dispose", scope, disposeFunctions);

                if(JCMClientConfig.INSTANCE.scripting.scriptDebugMode.value()) {
                    scriptManager.getLogger().info("[JCM Scripting] Loaded script: {}:{}", scriptLocation.getNamespace(), scriptLocation.getPath());
                }
            }
        } finally {
            ScriptResourceUtil.finishParseScript();
            Context.exit();
        }
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

    public List<ScriptContent> getScriptContents() {
        return this.scriptContents;
    }

    public Future<?> invokeCreateFunctions(ScriptInstance<?> instance, Runnable finishCallback) {
        return invokeFunctions(instance, createFunctions, finishCallback);
    }

    public void invokeRenderFunctions(ScriptInstance<?> instance, Runnable finishCallback) {
        if(instance.shouldInvalidate() || instance.scriptTask != null && !instance.scriptTask.isDone()) {
            return;
        }
        if(instance.isCreateFunctionInvoked()) {
            instance.scriptTask = invokeFunctions(instance, renderFunctions, finishCallback);
        } else {
            instance.scriptTask = invokeCreateFunctions(instance, finishCallback);
            instance.setCreateFunctionInvoked();
        }
    }

    public void invokeDisposeFunctions(ScriptInstance<?> instance, Runnable finishCallback) {
        invokeFunctions(instance, disposeFunctions, finishCallback);
    }

    public Future<?> invokeFunctions(ScriptInstance<?> scriptInstance, List<Function> functions, Runnable finishExecutionCallback) {
        if(duringFailCooldown()) {
            return null;
        }

        return scriptManager.submitScriptTask(executorService, () -> {
            if(duringFailCooldown()) return;

            timing.prepareForScript(scriptInstance);
            try {
                Scriptable scope = getScope();
                Context cx = Context.enter();
                cx.setLanguageVersion(Context.VERSION_ES6);
                cx.setClassShutter(scriptManager.getClassShutter());
                long startTime = System.nanoTime();
                for(Function func : functions) {
                    func.call(cx, scope, scope, new Object[] {
                        scriptInstance.getContextObject(),
                        scriptInstance.getOrCreateStateObject(cx, scope),
                        scriptInstance.getWrapperObject()
                    });
                }
                scriptInstance.setLastExecutionDurationMs(System.nanoTime() - startTime);
            } catch (Exception e) {
                scriptManager.getLogger().error("[MTR Scripting via JCM] Error executing script {}!", displayName, e);
                lastFailedTime = System.currentTimeMillis();
                capturedScriptException = e;
            } finally {
                Context.exit();
            }

            if(finishExecutionCallback != null) {
                try {
                    finishExecutionCallback.run();
                } catch (Exception e) {
                    scriptManager.getLogger().fatal("[MTR Scripting via JCM] Internal error during finalization work after script execution, this is a bug!", e);
                }
            }
        });
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
}
