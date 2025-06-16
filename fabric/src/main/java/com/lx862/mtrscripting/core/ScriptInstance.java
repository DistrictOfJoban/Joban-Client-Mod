package com.lx862.mtrscripting.core;

import com.lx862.mtrscripting.lib.org.mozilla.javascript.Scriptable;

import java.util.concurrent.Future;

public abstract class ScriptInstance<T> {
    private final ScriptContext scriptContext;
    private final ParsedScript parsedScripts;
    private double lastExecutionDurationMs;
    protected T wrapperObject;
    public Scriptable state;
    public Future<?> scriptTask;
    public double lastExecuteTime;
    private boolean createFunctionInvoked = false;

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

    public void setLastExecutionDurationMs(long nanoSec) {
        this.lastExecutionDurationMs = nanoSec / 1000000.0;
    }

    public double getLastExecutionDurationMs() {
        return this.lastExecutionDurationMs;
    }

    public void setCreateFunctionInvoked() {
        this.createFunctionInvoked = true;
    }

    public boolean isCreateFunctionInvoked() {
        return this.createFunctionInvoked;
    }

    public abstract boolean shouldInvalidate();
}
