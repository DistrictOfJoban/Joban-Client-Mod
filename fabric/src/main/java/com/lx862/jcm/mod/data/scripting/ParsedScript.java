package com.lx862.jcm.mod.data.scripting;

import com.lx862.jcm.mod.data.scripting.base.ScriptContext;
import com.lx862.jcm.mod.data.scripting.base.ScriptInstance;
import com.lx862.jcm.mod.data.scripting.util.*;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mozilla.javascript.*;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.ResourceManagerHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public class ParsedScript {
    private static final int SCRIPT_RESET_TIME = 4000;
    private long lastFailedTime = -1;
    private final String contextName;
    private final Scriptable scope;
    private final List<Function> createFunctions = new ArrayList<>();
    private final List<Function> renderFunctions = new ArrayList<>();
    private final List<Function> disposeFunctions = new ArrayList<>();

    public ParsedScript(String contextName, List<Identifier> scriptsLocation) {
        this.contextName = contextName;

        try {
            Context cx = Context.enter();
            cx.setLanguageVersion(Context.VERSION_ES6);
            scope = cx.initStandardObjects();
            //scope.put("print", scope, new NativeJavaMethod(ScriptResourceUtil.class.getMethod("print", Object[].class), "print"));

            scope.put("Resources", scope, new NativeJavaClass(scope, ScriptResourceUtil.class));
            scope.put("GraphicsTexture", scope, new NativeJavaClass(scope, GraphicsTexture.class));

            scope.put("Timing", scope, new NativeJavaClass(scope, TimingUtil.class));
            scope.put("StateTracker", scope, new NativeJavaClass(scope, StateTracker.class));
            scope.put("CycleTracker", scope, new NativeJavaClass(scope, CycleTracker.class));
            scope.put("RateLimit", scope, new NativeJavaClass(scope, RateLimit.class));
            scope.put("TextUtil", scope, new NativeJavaClass(scope, TextUtil.class));

            scope.put("MinecraftClient", scope, new NativeJavaClass(scope, MinecraftClientUtil.class));


            cx.evaluateString(scope, "\"use strict\"", "", 1, null);

            ScriptResourceUtil.activeCtx = cx;
            ScriptResourceUtil.activeScope = scope;
            for(Identifier scriptLocation : scriptsLocation) {
                String scriptText = ResourceManagerHelper.readResource(scriptLocation);
                cx.evaluateString(scope, scriptText, scriptLocation.getNamespace() + ":" + scriptLocation.getPath(), 1, null);

                tryAndAddFunction("create", scope, createFunctions);
                tryAndAddFunction("render", scope, renderFunctions);
                tryAndAddFunction("dispose", scope, disposeFunctions);
                tryAndAddFunction("create" + contextName, scope, createFunctions);
                tryAndAddFunction("render" + contextName, scope, renderFunctions);
                tryAndAddFunction("dispose" + contextName, scope, disposeFunctions);

                JCMLogger.info("[Scripting] Loaded script" + scriptLocation.getNamespace() + ":" + scriptLocation.getPath());
            }
        } finally {
            ScriptResourceUtil.activeCtx = null;
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

    public Future<?> invokeFunction(ScriptInstance scriptInstance, ScriptContext contextObj, Object params, List<Function> functionList, Runnable callback) {
        if(lastFailedTime != -1 && System.currentTimeMillis() - lastFailedTime <= SCRIPT_RESET_TIME) {
            return null;
        }

        return ScriptManager.submitScript(() -> {
            TimingUtil.prepareForScript(scriptInstance);
            try {
                Scriptable scope = getScope();
                Context cx = Context.enter();
                cx.setLanguageVersion(Context.VERSION_ES6);
                if(scriptInstance.state == null) scriptInstance.state = cx.newObject(scope);

                for(Function func : functionList) {
                    func.call(cx, scope, scope, new Object[]{contextObj, scriptInstance.state, params});
                }
            } catch (Exception e) {
                JCMLogger.error("[Scripting] Error executing script!");
                e.printStackTrace();
                lastFailedTime = System.currentTimeMillis();
            } finally {
                Context.exit();
            }
            callback.run();
        });
    }

    public Future<?> invokeCreateFunction(ScriptInstance instance, ScriptContext contextObj, Object params, Runnable callback) {
        return invokeFunction(instance, contextObj, params, createFunctions, callback);
    }

    public Future<?> invokeRenderFunction(ScriptInstance instance, ScriptContext contextObj, Object params, Runnable callback) {
        return invokeFunction(instance, contextObj, params, renderFunctions, callback);
    }

    public Future<?> invokeDisposeFunction(ScriptInstance instance, ScriptContext contextObj, Object params, Runnable callback) {
        return invokeFunction(instance, contextObj, params, disposeFunctions, callback);
    }

    public Scriptable getScope() {
        return scope;
    }
}
