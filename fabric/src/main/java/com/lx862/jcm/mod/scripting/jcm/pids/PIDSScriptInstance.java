package com.lx862.jcm.mod.scripting.jcm.pids;

import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.scripting.mtr.render.ScriptRenderManager;
import com.lx862.jcm.mod.scripting.mtr.sound.ScriptSoundManager;
import com.lx862.mtrscripting.core.ParsedScript;
import com.lx862.mtrscripting.core.ScriptInstance;

import java.util.Objects;

public class PIDSScriptInstance extends ScriptInstance<PIDSWrapper> {
    private final PIDSBlockEntity blockEntity;
    private final ScriptSoundManager soundManager;
    private final ScriptRenderManager renderManager;

    public PIDSScriptInstance(PIDSBlockEntity blockEntity, ParsedScript script, PIDSWrapper wrapperObject) {
        super(new PIDSScriptContext(blockEntity.getPresetId()), script);
        setWrapperObject(wrapperObject);
        this.blockEntity = blockEntity;
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
        return blockEntity.getWorld2().getBlockEntity(blockEntity.getPos2()) == null || !Objects.equals(blockEntity.getPresetId(), getScriptContext().getName());
    }
}
