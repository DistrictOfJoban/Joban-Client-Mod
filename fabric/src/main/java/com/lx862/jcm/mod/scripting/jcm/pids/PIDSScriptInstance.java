package com.lx862.jcm.mod.scripting.jcm.pids;

import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.scripting.mtr.render.ScriptRenderManager;
import com.lx862.jcm.mod.scripting.mtr.sound.ScriptSoundManager;
import com.lx862.mtrscripting.core.ParsedScript;
import com.lx862.mtrscripting.core.ScriptInstance;
import org.mtr.mapping.holder.MinecraftClient;

import java.util.Objects;

public class PIDSScriptInstance extends ScriptInstance<PIDSWrapper> {
    private final PIDSBlockEntity blockEntity;
    private ScriptSoundManager soundManager;
    private ScriptRenderManager renderManager;

    public PIDSScriptInstance(PIDSBlockEntity blockEntity, ParsedScript script, PIDSWrapper wrapperObject) {
        super(new PIDSScriptContext(blockEntity.getPresetId()), script);
        setWrapperObject(wrapperObject);
        this.blockEntity = blockEntity;
        this.soundManager = new ScriptSoundManager();
        this.renderManager = new ScriptRenderManager();
    }

    public void saveRenderCalls(ScriptRenderManager renderManager) {
        this.renderManager = renderManager.copy();
    }

    public void saveSoundCalls(ScriptSoundManager soundManager) {
        this.soundManager = soundManager.copy();
    }

    public ScriptSoundManager getSoundManager() {
        return this.soundManager;
    }

    public ScriptRenderManager getRenderManager() {
        return this.renderManager;
    }

    public boolean shouldInvalidate() {
        boolean beRemoved = blockEntity.isRemoved2() || blockEntity.getWorld2().getBlockEntity(blockEntity.getPos2()) == null;
        boolean mismatchedPreset = !Objects.equals(blockEntity.getPresetId(), getScriptContext().getName());
        boolean notInGame = MinecraftClient.getInstance().getWorldMapped() == null;
        return notInGame || beRemoved || mismatchedPreset;
    }
}
