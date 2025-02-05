package com.lx862.jcm.mod.scripting.eyecandy;

import com.lx862.jcm.mod.scripting.SoundCall;
import com.lx862.mtrscripting.scripting.base.ScriptContext;
import com.lx862.mtrscripting.scripting.util.Matrices;
import com.lx862.mtrscripting.scripting.util.ScriptedModel;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mod.block.BlockEyeCandy;

import java.util.ArrayList;
import java.util.List;

public class EyeCandyScriptContext extends ScriptContext {
    private final BlockEyeCandy.BlockEntity blockEntity;
    private final List<ModelDrawCall> drawCalls = new ArrayList<>();
    private final List<SoundCall> soundCalls = new ArrayList<>();

    public EyeCandyScriptContext(BlockEyeCandy.BlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    public void drawModel(ScriptedModel model, Matrices matrices) {
        this.drawCalls.add(new ModelDrawCall(model, matrices == null ? null : matrices.getStoredMatrixTransformations().copy()));
    }

    public void playSound(Identifier id, float volume, float pitch) {
        this.soundCalls.add(new SoundCall(id, blockEntity.getPos2().getX() + blockEntity.getTranslateX(), blockEntity.getPos2().getY() + blockEntity.getTranslateY(), blockEntity.getPos2().getZ() + blockEntity.getTranslateZ(), volume, pitch));
    }

    public List<ModelDrawCall> getDrawCalls() {
        return new ArrayList<>(this.drawCalls);
    }

    public List<SoundCall> getSoundCalls() {
        return new ArrayList<>(this.soundCalls);
    }

    @Override
    public void reset() {
        this.drawCalls.clear();
        this.soundCalls.clear();
    }
}
