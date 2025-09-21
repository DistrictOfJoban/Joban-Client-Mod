package com.lx862.jcm.mod.scripting.mtr;

import com.lx862.jcm.mod.JCMClient;
import com.lx862.jcm.mod.scripting.mtr.render.ScriptRenderManager;
import com.lx862.jcm.mod.scripting.mtr.sound.ScriptSoundManager;
import com.lx862.mtrscripting.core.ScriptContext;

public abstract class MTRScriptContext extends ScriptContext {
    protected final ScriptSoundManager soundManager;
    protected final ScriptRenderManager renderManager;

    public MTRScriptContext(String name) {
        super(name);
        this.soundManager = new ScriptSoundManager();
        this.renderManager = new ScriptRenderManager();
    }

    public boolean debugModeEnabled() {
        return JCMClient.getConfig().debug;
    }

    public ScriptRenderManager renderManager() {
        return this.renderManager;
    }

    public ScriptSoundManager soundManager() {
        return this.soundManager;
    }

    @Override
    public void resetForNextRun() {
        this.renderManager.reset();
        this.soundManager.reset();
    }
}
