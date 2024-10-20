package com.lx862.jcm.mod.data.scripting.base;

import com.lx862.jcm.mod.data.scripting.ParsedScript;
import org.mozilla.javascript.Scriptable;

import java.util.concurrent.Future;

public abstract class ScriptInstance {
    public final ParsedScript parsedScript;
    private final ScriptContext scriptContext;
    public Scriptable state;
    protected Future<?> scriptTask;
    public double lastExecuteTime = 0;

    public ScriptInstance(ScriptContext scriptContext, ParsedScript script) {
        this.scriptContext = scriptContext;
        this.parsedScript = script;
    }

    public void onRender(Object targetObject, Runnable callback) {
        if(scriptTask == null || scriptTask.isDone()) {
            scriptTask = parsedScript.invokeRenderFunction(this, scriptContext, targetObject, callback);
        }
    }

    public ScriptContext getScriptContext() {
        return scriptContext;
    }

    public abstract Object getWrapperObject();

    public abstract boolean isDead();
}
