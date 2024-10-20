package com.lx862.jcm.mod.scripting.base;

import com.lx862.jcm.mod.scripting.ParsedScript;
import org.mozilla.javascript.Scriptable;

import java.util.concurrent.Future;

public abstract class ScriptInstance {
    private final ScriptContext scriptContext;
    public final ParsedScript parsedScripts;
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

    public abstract Object getWrapperObject();

    public abstract boolean isDead();
}
