package com.lx862.mtrscripting.core.primitive;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.api.AbstractScriptContext;

import java.util.concurrent.Future;
import java.util.function.Supplier;

@ApiInternal
public abstract class ScriptInstance<T> {
    private final AbstractScriptContext contextObject;
    private Object stateObject;
    private T wrapperObject;

    private final ParsedScript parsedScript;
    private double lastExecutionDurationMs;
    private double beginExecuteTime = -1;
    private boolean createFunctionInvoked = false;
    public Future<?> scriptTask;
    public double lastExecuteTime;

    public ScriptInstance(AbstractScriptContext contextObject, ParsedScript script) {
        this.contextObject = contextObject;
        this.parsedScript = script;
    }

    public AbstractScriptContext getContextObject() {
        return contextObject;
    }

    public T getWrapperObject() {
        return wrapperObject;
    }

    public ParsedScript getScript() {
        return parsedScript;
    }

    public Object getOrCreateStateObject(Supplier<Object> createState) {
        if(this.stateObject == null) this.stateObject = createState.get();
        return stateObject;
    }

    public void setWrapperObject(T obj) {
        wrapperObject = obj;
    }

    public void beginTrackExecuteTime() {
        this.beginExecuteTime = System.nanoTime();
    }

    public void endTrackExecuteTime() {
        this.lastExecutionDurationMs = (System.nanoTime() - this.beginExecuteTime) / 1000000.0;
        this.beginExecuteTime = -1;
    }

    public double getLastExecutionDurationMs() {
        return this.lastExecutionDurationMs;
    }

    public void setCreateFunctionInvoked(boolean isTrue) {
        this.createFunctionInvoked = isTrue;
    }

    public boolean isCreateFunctionInvoked() {
        return this.createFunctionInvoked;
    }

    public boolean executionTimedOut(int thresholdMs) {
        return this.beginExecuteTime > 0 && (System.nanoTime() - this.beginExecuteTime) > (thresholdMs * 1000000L);
    }

    public abstract boolean shouldInvalidate();
}
