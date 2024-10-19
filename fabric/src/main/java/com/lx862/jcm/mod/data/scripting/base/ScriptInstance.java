package com.lx862.jcm.mod.data.scripting.base;

import com.lx862.jcm.mod.data.scripting.PIDSScriptObject;
import com.lx862.jcm.mod.data.scripting.ScriptContext;
import com.lx862.jcm.mod.data.scripting.ScriptManager;
import com.lx862.jcm.mod.data.scripting.util.TimingUtil;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import java.util.concurrent.Future;

public abstract class ScriptInstance {
    private static final int SCRIPT_RESET_TIME = 4000;
    private long lastFailedTime = -1;
    protected Scriptable state;
    protected Future<?> scriptTask;
    protected final Scriptable scope;
    public double lastExecuteTime = 0;

    public ScriptInstance(Scriptable scope) {
        this.scope = scope;
    }

    public void execute(Object targetObject, Runnable callback) {
        if(scriptTask == null || scriptTask.isDone()) {
            if(lastFailedTime == -1 || System.currentTimeMillis() - lastFailedTime > SCRIPT_RESET_TIME) {
                scriptTask = ScriptManager.submitScript(() -> {
                    TimingUtil.prepareForScript(this);
                    try {
                        try {
                            Context cx = Context.enter();
                            cx.setLanguageVersion(Context.VERSION_ES6);
                            if(state == null) state = cx.newObject(scope);
                            ScriptContext ctx = new ScriptContext();
                            Object renderFunction = scope.get("render", scope);
                            Object[] renderParams = new Object[]{ctx, state, targetObject};

                            if(renderFunction instanceof Scriptable && renderFunction != Scriptable.NOT_FOUND) {
                                ((Function)renderFunction).call(cx, scope, scope, renderParams);
                            }
                        } finally {
                            Context.exit();
                        }
                    } catch (Exception e) {
                        JCMLogger.error("[PIDS] Error executing script!");
                        e.printStackTrace();
                        lastFailedTime = System.currentTimeMillis();
                    }
                    callback.run();
                });
            }
        }
    }

    public abstract boolean isDead();
}
