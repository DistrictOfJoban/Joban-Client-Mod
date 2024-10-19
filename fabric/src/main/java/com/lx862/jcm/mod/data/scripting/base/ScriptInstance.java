package com.lx862.jcm.mod.data.scripting.base;

import com.lx862.jcm.mod.data.scripting.ScriptManager;
import com.lx862.jcm.mod.data.scripting.util.TimingUtil;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mozilla.javascript.Scriptable;

import java.util.concurrent.Future;

public abstract class ScriptInstance {
    private static final int SCRIPT_RESET_TIME = 4000;
    private long lastFailedTime = -1;
    protected Future<?> scriptTask;
    protected final Scriptable scope;
    public double lastExecuteTime = 0;

    public ScriptInstance(Scriptable scope) {
        this.scope = scope;
    }

    public void execute(Runnable runScript) {
        if(scriptTask == null || scriptTask.isDone()) {
            if(lastFailedTime == -1 || System.currentTimeMillis() - lastFailedTime > SCRIPT_RESET_TIME) {
                scriptTask = ScriptManager.submitScript(() -> {
                    TimingUtil.prepareForScript(this);
                    try {
                        runScript.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                        JCMLogger.error("[PIDS] Error executing script!");
                        lastFailedTime = System.currentTimeMillis();
                    }
                });
            }
        }
    }
}
