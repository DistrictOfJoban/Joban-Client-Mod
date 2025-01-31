package com.lx862.mtrscripting.scripting.base;

import com.lx862.mtrscripting.scripting.ParsedScript;
import vendor.com.lx862.jcm.org.mozilla.javascript.Scriptable;

import java.util.concurrent.Future;

public abstract class ScriptInstance<T> {
    private final ScriptContext scriptContext;
    protected T wrapperObject;
    public final String id;
    public final ParsedScript parsedScripts;
    public Scriptable state;
    public Future<?> scriptTask;
    public double lastExecuteTime = 0;

    public ScriptInstance(String id, ScriptContext scriptContext, ParsedScript script) {
        this.id = id;
        this.scriptContext = scriptContext;
        this.parsedScripts = script;
    }

    public ScriptContext getScriptContext() {
        return scriptContext;
    }

    public void updateWrapperObject(T obj) {
        wrapperObject = obj;
    }

    public T getWrapperObject() {
        return wrapperObject;
    }

    public abstract boolean isDead();
}
