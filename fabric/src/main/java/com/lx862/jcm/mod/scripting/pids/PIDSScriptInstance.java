package com.lx862.jcm.mod.scripting.pids;

import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.util.render.ScriptRenderManager;
import com.lx862.mtrscripting.core.util.sound.ScriptSoundManager;
import com.lx862.mtrscripting.core.primitive.ParsedScript;
import com.lx862.mtrscripting.core.primitive.ScriptInstance;
import org.mtr.mapping.holder.MinecraftClient;

import java.util.Objects;

@ApiInternal
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
        boolean presetChanged = !Objects.equals(blockEntity.getPresetId(), getContextObject().getName());
        boolean notInGame = MinecraftClient.getInstance().getWorldMapped() == null;
        return notInGame || beRemoved || presetChanged;
    }
}
