package com.lx862.mtrscripting.core.rhinojs;

import com.lx862.mtrscripting.core.ScriptManager;
import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.primitive.ParsedScript;
import com.lx862.mtrscripting.core.primitive.ScriptContent;
import com.lx862.mtrscripting.core.primitive.ScriptInstance;
import com.lx862.mtrscripting.core.util.ScriptResourceUtil;
import com.lx862.mtrscripting.core.util.TimingJS;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.*;
import com.lx862.mtrscripting.mod.MTRScriptingMod;
import org.mtr.mapping.holder.Identifier;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Represent a script parsed and executed under the Rhino JS engine.
 */
@ApiInternal
public class RhinoParsedScript extends ParsedScript {
    private Scriptable scope;
    private final List<Function> createFunctions;
    private final List<Function> renderFunctions;
    private final List<Function> disposeFunctions;

    private static final Map<String, Script> compiledScriptCache = new HashMap<>();

    public RhinoParsedScript(ScriptManager scriptManager, String displayName, String contextName, ParsedScript.Configuration configuration) {
        super(scriptManager, displayName, contextName, configuration);
        this.createFunctions = new ArrayList<>();
        this.renderFunctions = new ArrayList<>();
        this.disposeFunctions = new ArrayList<>();
    }

    @Override
    public RhinoParsedScript parse(boolean verboseLogging) {
        try {
            Context cx = Context.enter();
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setClassShutter(new MTRRhinoClassShutter(scriptManager.getClassShutter()));
            scope = new ImporterTopLevel(cx);
            parseScriptContent("\"use strict\";", "Use Strict Statement");
            ScriptResourceUtil.beginParseScript(this);

            /* Init global variables */
            exposeClassInstance("Timing", timing);
            scriptManager.parseScriptEvent.invoke(e -> e.accept(contextName, this));

            for(ScriptContent scriptEntry : scriptContents) {
                final Identifier scriptLocation = scriptEntry.location();
                final String scriptContent = scriptEntry.content();

                ScriptResourceUtil.executeScript(this, scriptLocation, scriptContent);

                tryAndAddFunction("create", createFunctions);
                tryAndAddFunction("render", renderFunctions);
                tryAndAddFunction("dispose", disposeFunctions);

                if(verboseLogging) {
                    scriptManager.getLogger().info("[MTR Scripting via JCM] Loaded script: {}:{}", scriptLocation.getNamespace(), scriptLocation.getPath());
                }
            }
        } finally {
            ScriptResourceUtil.finishParseScript();
            Context.exit();
        }
        return this;
    }

    @Override
    public void parseScriptContent(String str, String source) {
        Context cx = Context.getCurrentContext();
        if(cx == null) throw new IllegalStateException("No rhino context currently available!");
        Script script = compiledScriptCache.containsKey(source) ? compiledScriptCache.get(source) : cx.compileString(str, source, 1, null);
        script.exec(cx, scope);
        compiledScriptCache.put(source, script);
    }

    @Override
    public Future<?> invokeCreateFunctions(ScriptInstance<?> instance, Runnable finishCallback) {
        return invokeJSFunctions(instance, createFunctions, () -> {
                finishCallback.run();
                instance.setCreateFunctionInvoked(true);
        },
                // Ensure create function is re-invoked after error.
                // Note: This will leak memory when the create function is invoked again, but not sure if there's a good solution without manually introducing checkpoint for disposing.
                () -> instance.setCreateFunctionInvoked(false)
        );
    }

    @Override
    public void invokeRenderFunctions(ScriptInstance<?> instance, Runnable finishCallback) {
        if(instance.shouldInvalidate() || instance.scriptTask != null && !instance.scriptTask.isDone()) {
            return;
        }
        if(instance.isCreateFunctionInvoked()) {
            instance.scriptTask = invokeJSFunctions(instance, renderFunctions, finishCallback, null);
        } else {
            instance.scriptTask = invokeCreateFunctions(instance, finishCallback);
        }
    }

    @Override
    public void invokeDisposeFunctions(ScriptInstance<?> instance, Runnable finishCallback) {
        invokeJSFunctions(instance, disposeFunctions, finishCallback, null);
    }

    private Future<?> invokeJSFunctions(ScriptInstance<?> scriptInstance, List<Function> functions, Runnable finishExecutionCallback, Runnable errorCallback) {
        if(duringFailCooldown()) return null;
        if(TimingJS.globalElapsed() - scriptInstance.lastExecuteTime < rateLimit) return null;

        return scriptManager.submitScriptTask(executorService, () -> {
            if(duringFailCooldown()) return;
            if(Thread.currentThread().isInterrupted()) return;

            try {
                Context cx = Context.enter();
                cx.setLanguageVersion(Context.VERSION_ES6);
                cx.setClassShutter(new MTRRhinoClassShutter(scriptManager.getClassShutter()));
                timing.prepareForScript(scriptInstance);
                scriptInstance.beginTrackExecuteTime();
                for(Function func : functions) {
                    func.call(cx, scope, scope, new Object[] {
                        scriptInstance.getContextObject(),
                        scriptInstance.getOrCreateStateObject(() -> cx.newObject(scope)),
                        scriptInstance.getWrapperObject()
                    });
                }
            } catch (Exception e) {
                scriptManager.getLogger().error("[MTR Scripting via JCM {}] Error executing JS Script {}!", MTRScriptingMod.VERSION, displayName);
                if(e instanceof RhinoException) {
                    RhinoException re = ((RhinoException)e);
                    scriptManager.getLogger().error(re.getMessage());
                    for(String ste : re.getScriptStackTrace().split("\n")) {
                        scriptManager.getLogger().error("{}", ste);
                    }
                } else {
                    scriptManager.getLogger().error("", e);
                }

                lastFailedTime = System.currentTimeMillis();
                capturedScriptException = e;
                if(errorCallback != null) errorCallback.run();
            } finally {
                scriptInstance.endTrackExecuteTime();
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

    private void tryAndAddFunction(String name, List<Function> listToAdd) {
        Object func = scope.get(name, scope);
        if(func != Scriptable.NOT_FOUND) {
            if(func instanceof Function) {
                listToAdd.add((Function)func);
            }
            scope.delete(name);
        }
    }

    @Override
    public void exposeClassMethod(String name, Method method) {
        scope.put(name, scope, new NativeJavaMethod(method, name));
    }

    @Override
    public void exposeClass(String name, Class<?> cl) {
        scope.put(name, scope, new NativeJavaClass(scope, cl));
    }

    @Override
    public void exposeClassInstance(String name, Object classInstance) {
        scope.put(name, scope, new NativeJavaObject(scope, classInstance, com.lx862.mtrscripting.lib.org.mozilla.javascript.lc.type.TypeInfo.OBJECT));
    }

    public static void clearScriptCache() {
        compiledScriptCache.clear();
    }
}
