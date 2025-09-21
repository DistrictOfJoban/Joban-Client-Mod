package com.lx862.jcm.mod.scripting.mtr.eyecandy;

import com.lx862.jcm.mod.scripting.mtr.render.ScriptRenderManager;
import com.lx862.jcm.mod.scripting.mtr.sound.ScriptSoundManager;
import com.lx862.mtrscripting.core.ParsedScript;
import com.lx862.mtrscripting.core.ScriptInstance;

import java.util.Objects;

public class EyeCandyScriptInstance extends ScriptInstance<EyecandyBlockEntityWrapper> {
    private final EyecandyBlockEntityWrapper be;
    private final ScriptRenderManager renderManager;
    private final ScriptSoundManager soundManager;

    public EyeCandyScriptInstance(EyeCandyScriptContext context, EyecandyBlockEntityWrapper be, ParsedScript script) {
        super(context, script);
        this.be = be;
        this.soundManager = new ScriptSoundManager();
        this.renderManager = new ScriptRenderManager();
    }

    public void updateRenderer(ScriptRenderManager renderManager) {
        this.renderManager.updateDrawCalls(renderManager);
    }

    public void updateSound(ScriptSoundManager soundManager) {
        this.soundManager.updateSoundCalls(soundManager);
    }

    public ScriptSoundManager getSoundManager() {
        return this.soundManager;
    }

    public ScriptRenderManager getRenderManager() {
        return this.renderManager;
    }

    public boolean shouldInvalidate() {
        return be.getWorld().getBlockEntity(be.blockPos().rawBlockPos()) == null || !Objects.equals(be.getModelId(), getScriptContext().getName());
    }
}
