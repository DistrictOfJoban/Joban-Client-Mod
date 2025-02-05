package com.lx862.mtrscripting.scripting.base;

import com.lx862.mtrscripting.scripting.ParsedScript;
import vendor.com.lx862.jcm.org.mozilla.javascript.Scriptable;

import java.util.concurrent.Future;

public abstract class ScriptInstance<T> {
    private final ScriptContext scriptContext;
    private final ParsedScript parsedScripts;
    public double lastExecuteTime = 0;
    protected T wrapperObject;
    public Scriptable state;
    public Future<?> scriptTask;

    public ScriptInstance(ScriptContext scriptContext, ParsedScript script) {
        this.scriptContext = scriptContext;
        this.parsedScripts = script;
    }

    public ScriptContext getScriptContext() {
        return scriptContext;
    }

    public ParsedScript getScript() {
        return parsedScripts;
    }

    public void setWrapperObject(T obj) {
        wrapperObject = obj;
    }

    public T getWrapperObject() {
        return wrapperObject;
    }

    public abstract boolean isDead();
}
