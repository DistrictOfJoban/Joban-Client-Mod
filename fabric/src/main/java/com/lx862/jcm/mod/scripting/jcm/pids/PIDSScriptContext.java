package com.lx862.jcm.mod.scripting.jcm.pids;

import com.google.gson.JsonParser;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.jcm.mod.scripting.mtr.MTRScriptContext;
import com.lx862.jcm.mod.scripting.mtr.render.ScriptRenderManager;
import com.lx862.jcm.mod.scripting.mtr.sound.ScriptSoundManager;

public class PIDSScriptContext extends MTRScriptContext {
    protected final ScriptSoundManager soundManager;
    protected final ScriptRenderManager renderManager;

    public PIDSScriptContext(String name) {
        super(name);
        this.soundManager = new ScriptSoundManager();
        this.renderManager = new ScriptRenderManager();
    }

    public PIDSComponent parseComponent(String str) {
        return PIDSComponent.parse(new JsonParser().parse(str).getAsJsonObject());
    }

    @Deprecated
    public ScriptRenderManager renderManager() {
        return getRenderManager();
    }

    @Deprecated
    public ScriptSoundManager soundManager() {
        return getSoundManager();
    }

    public ScriptRenderManager getRenderManager() {
        return this.renderManager;
    }

    public ScriptSoundManager getSoundManager() {
        return this.soundManager;
    }

    public void draw(Object obj) {
        if(obj instanceof PIDSDrawCall) {
            renderManager().queue((PIDSDrawCall<?>)obj);
        } else {
            throw new IllegalArgumentException("1st parameter is not a DrawCall!");
        }
    }

    @Override
    public void resetForNextRun() {
        this.renderManager.reset();
        this.soundManager.reset();
    }
}
