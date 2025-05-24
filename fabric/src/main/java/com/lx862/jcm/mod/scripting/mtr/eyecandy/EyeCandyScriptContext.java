package com.lx862.jcm.mod.scripting.mtr.eyecandy;

import com.lx862.jcm.mod.scripting.mtr.sound.PositionedSoundCall;
import com.lx862.mtrscripting.core.ScriptContext;
import com.lx862.mtrscripting.util.Matrices;
import com.lx862.mtrscripting.util.ScriptedModel;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mod.block.BlockEyeCandy;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class EyeCandyScriptContext extends ScriptContext {
    private final BlockEyeCandy.BlockEntity blockEntity;
    private final List<ModelDrawCall> drawCalls = new ArrayList<>();
    private final List<PositionedSoundCall> positionedSoundCalls = new ArrayList<>();

    public EyeCandyScriptContext(BlockEyeCandy.BlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    public void drawModel(ScriptedModel model, Matrices matrices) {
        this.drawCalls.add(new ModelDrawCall(model, matrices == null ? null : matrices.getStoredMatrixTransformations().copy()));
    }

    public void playSound(Identifier id, float volume, float pitch) {
        this.positionedSoundCalls.add(new PositionedSoundCall(id, blockEntity.getPos2().getX() + blockEntity.getTranslateX(), blockEntity.getPos2().getY() + blockEntity.getTranslateY(), blockEntity.getPos2().getZ() + blockEntity.getTranslateZ(), volume, pitch));
    }

    public List<ModelDrawCall> getDrawCalls() {
        return new ArrayList<>(this.drawCalls);
    }

    public List<PositionedSoundCall> getSoundCalls() {
        return new ArrayList<>(this.positionedSoundCalls);
    }

    @Override
    public void reset() {
        this.drawCalls.clear();
        this.positionedSoundCalls.clear();
    }
}
