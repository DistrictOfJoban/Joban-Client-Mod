package com.lx862.mtrscripting.core.primitive;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.api.AbstractScriptContext;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.Context;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.Scriptable;

import java.util.concurrent.Future;

@ApiInternal
public abstract class ScriptInstance<T> {
    private final AbstractScriptContext contextObject;
    private Scriptable stateObject;
    private T wrapperObject;

    private final ParsedScript parsedScript;
    private double lastExecutionDurationMs;
    public Future<?> scriptTask;
    public double lastExecuteTime;
    private boolean createFunctionInvoked = false;

    public ScriptInstance(AbstractScriptContext contextObject, ParsedScript script) {
        this.contextObject = contextObject;
        this.parsedScript = script;
    }

    public AbstractScriptContext getContextObject() {
        return contextObject;
    }

    public Scriptable getOrCreateStateObject(Context cx, Scriptable scope) {
        if(this.stateObject == null) this.stateObject = cx.newObject(scope);
        return stateObject;
    }

    public T getWrapperObject() {
        return wrapperObject;
    }

    public ParsedScript getScript() {
        return parsedScript;
    }

    public void setWrapperObject(T obj) {
        wrapperObject = obj;
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
