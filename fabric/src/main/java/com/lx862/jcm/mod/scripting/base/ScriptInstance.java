package com.lx862.jcm.mod.scripting.base;

import com.lx862.jcm.mod.scripting.ParsedScript;
import vendor.com.lx862.jcm.org.mozilla.javascript.Scriptable;

import java.util.concurrent.Future;

public abstract class ScriptInstance<T> {
    private final ScriptContext scriptContext;
    public final ParsedScript parsedScripts;
    protected T wrapperObject;
    public Scriptable state;
    public Future<?> scriptTask;
    public double lastExecuteTime = 0;

    public ScriptInstance(ScriptContext scriptContext, ParsedScript script) {
        this.scriptContext = scriptContext;
        this.parsedScripts = script;
    }

    public ScriptContext getScriptContext() {
        return scriptContext;
    }

    public abstract T getWrapperObject();

    public void updateWrapperObject(T obj) {
        wrapperObject = obj;
    }

    public abstract boolean isDead();
}
