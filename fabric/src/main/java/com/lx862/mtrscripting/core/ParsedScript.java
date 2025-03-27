package com.lx862.mtrscripting.core;

import com.lx862.mtrscripting.api.ScriptingAPI;
import com.lx862.mtrscripting.ScriptManager;
import com.lx862.mtrscripting.util.*;
import vendor.com.lx862.jcm.org.mozilla.javascript.*;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.ResourceManagerHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

public class ParsedScript {
    private static final int SCRIPT_RESET_TIME = 4000;
    private final List<Function> createFunctions;
    private final List<Function> renderFunctions;
    private final List<Function> disposeFunctions;
    private final ScriptManager scriptManager;
    private final Scriptable scope;
    private long lastFailedTime = -1;

    public ParsedScript(ScriptManager scriptManager, String contextName, Map<Identifier, String> scripts) throws Exception {
        this.scriptManager = scriptManager;
        this.createFunctions = new ArrayList<>();
        this.renderFunctions = new ArrayList<>();
        this.disposeFunctions = new ArrayList<>();

        try {
            Context cx = Context.enter();
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setClassShutter(ScriptManager.CLASS_SHUTTER);
            scope = new ImporterTopLevel(cx);

            scope.put("include", scope, new NativeJavaMethod(ScriptResourceUtil.class.getMethod("includeScript", Object.class), "includeScript"));
            scope.put("print", scope, new NativeJavaMethod(ScriptResourceUtil.class.getMethod("print", Object[].class), "print"));
            scope.put("Resources", scope, new NativeJavaClass(scope, ScriptResourceUtil.class));
            scope.put("GraphicsTexture", scope, new NativeJavaClass(scope, GraphicsTexture.class));

            scope.put("Timing", scope, new NativeJavaClass(scope, TimingUtil.class));
            scope.put("StateTracker", scope, new NativeJavaClass(scope, StateTracker.class));
            scope.put("CycleTracker", scope, new NativeJavaClass(scope, CycleTracker.class));
            scope.put("RateLimit", scope, new NativeJavaClass(scope, RateLimit.class));
            scope.put("HttpUtil", scope, new NativeJavaClass(scope, HttpUtil.class));

            scope.put("Matrices", scope, new NativeJavaClass(scope, Matrices.class));

            scope.put("MinecraftClient", scope, new NativeJavaClass(scope, MinecraftClientUtil.class));
            scope.put("ModelManager", scope, new NativeJavaClass(scope, ModelManager.class));

            ScriptingAPI.callOnParseScriptCallback(contextName, cx, scope);

            cx.evaluateString(scope, "\"use strict\";", "", 1, null);

            ScriptResourceUtil.activeContext = cx;
            ScriptResourceUtil.activeScope = scope;
            for(Map.Entry<Identifier, String> scriptEntry : scripts.entrySet()) {
                final Identifier scriptLocation = scriptEntry.getKey();
                final String scriptContent;
                if(scriptEntry.getValue() == null) {
                    scriptContent = ResourceManagerHelper.readResource(scriptEntry.getKey());
                    if(scriptContent.isEmpty()) throw new IllegalStateException(String.format("Cannot find script %s!", scriptLocation.getNamespace() + ":" + scriptLocation.getPath()));
                } else {
                    scriptContent = scriptEntry.getValue();
                }

                ScriptResourceUtil.executeScript(cx, scope, scriptEntry.getKey(), scriptContent);

                tryAndAddFunction("create", scope, createFunctions);
                tryAndAddFunction("render", scope, renderFunctions);
                tryAndAddFunction("dispose", scope, disposeFunctions);
                tryAndAddFunction("create" + contextName, scope, createFunctions);
                tryAndAddFunction("render" + contextName, scope, renderFunctions);
                tryAndAddFunction("dispose" + contextName, scope, disposeFunctions);

                ScriptManager.LOGGER.info("[Scripting] Loaded script: {}:{}", scriptLocation.getNamespace(), scriptLocation.getPath());
            }
        } finally {
            ScriptResourceUtil.activeContext = null;
            ScriptResourceUtil.activeScope = null;
            Context.exit();
        }
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

    public Future<?> invokeFunction(ScriptInstance<?> scriptInstance, List<Function> functionList, Runnable callback) {
        if(lastFailedTime != -1 && System.currentTimeMillis() - lastFailedTime <= SCRIPT_RESET_TIME) {
            return null;
        }

        return scriptManager.submitScriptTask(() -> {
            TimingUtil.prepareForScript(scriptInstance);
            try {
                Scriptable scope = getScope();
                Context cx = Context.enter();
                cx.setLanguageVersion(Context.VERSION_ES6);
                cx.setClassShutter(ScriptManager.CLASS_SHUTTER);
                if(scriptInstance.state == null) scriptInstance.state = cx.newObject(scope);

                for(Function func : functionList) {
                    func.call(cx, scope, scope, new Object[]{scriptInstance.getScriptContext(), scriptInstance.state, scriptInstance.getWrapperObject()});
                }
            } catch (Exception e) {
                ScriptManager.LOGGER.error("[Scripting] Error executing script!", e);
                lastFailedTime = System.currentTimeMillis();
            } finally {
                Context.exit();
            }
            callback.run();
        });
    }

    public Future<?> invokeCreateFunction(ScriptInstance<?> instance, Runnable callback) {
        return invokeFunction(instance, createFunctions, callback);
    }

    public Future<?> invokeRenderFunction(ScriptInstance<?> instance, Runnable callback) {
        if(instance.scriptTask != null && !instance.scriptTask.isDone()) {
            return instance.scriptTask;
        }
        return invokeFunction(instance, renderFunctions, callback);
    }

    public Future<?> invokeDisposeFunction(ScriptInstance<?> instance, Runnable callback) {
        return invokeFunction(instance, disposeFunctions, callback);
    }

    public Scriptable getScope() {
        return scope;
    }
}
