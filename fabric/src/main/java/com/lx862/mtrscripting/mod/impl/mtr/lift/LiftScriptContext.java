package com.lx862.mtrscripting.mod.impl.mtr.lift;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.util.render.ScriptRenderManager;
import com.lx862.mtrscripting.core.util.sound.ScriptSoundManager;
import com.lx862.mtrscripting.mod.impl.mtr.MTRScriptContext;
import org.mtr.core.data.Lift;

@SuppressWarnings("unused")
public class LiftScriptContext extends MTRScriptContext {
    protected ScriptSoundManager soundManager;
    protected ScriptRenderManager renderManager;

    @ApiInternal
    public LiftScriptContext(Lift lift) {
        super(lift.getStyle());
        this.soundManager = new ScriptSoundManager();
        this.renderManager = new ScriptRenderManager();
    }

    public ScriptRenderManager getRenderManager() {
        return this.renderManager;
    }

    public ScriptSoundManager getSoundManager() {
        return this.soundManager;
    }

    public void updateRenderer(ScriptRenderManager renderManager) {
        this.renderManager = renderManager.copy();
    }

    public void updateSound(ScriptSoundManager soundManager) {
        this.soundManager = soundManager.copy();
    }

    @Override
    public void resetForNextRun() {
        this.renderManager.reset();
        this.soundManager.reset();
    }
}
