package com.lx862.jcm.mod.scripting.mtr.eyecandy;

import com.lx862.jcm.mod.scripting.mtr.MTRScriptContext;
import com.lx862.jcm.mod.scripting.mtr.eyecandy.event.EyecandyEvents;
import com.lx862.jcm.mod.scripting.mtr.render.DisplayHelperCompat;
import com.lx862.jcm.mod.scripting.mtr.render.ModelDrawCall;
import com.lx862.jcm.mod.scripting.mtr.render.ScriptRenderManager;
import com.lx862.jcm.mod.scripting.mtr.sound.ScriptSoundManager;
import com.lx862.jcm.mod.scripting.mtr.util.ScriptedModel;
import com.lx862.mtrscripting.util.Matrices;
import com.lx862.mtrscripting.util.ScriptVector3f;
import com.lx862.mtrscripting.wrapper.VoxelShapeWrapper;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.VoxelShape;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class EyeCandyScriptContext extends MTRScriptContext {
    private final EyecandyBlockEntityWrapper blockEntity;
    private final List<ModelDrawCall> drawCalls = new ArrayList<>();
    private final EyecandyEvents events;
    private VoxelShapeWrapper outlineShape;
    private VoxelShapeWrapper collisionShape;
    protected final ScriptSoundManager soundManager;
    protected final ScriptRenderManager renderManager;

    public EyeCandyScriptContext(EyecandyBlockEntityWrapper blockEntity) {
        super(blockEntity.getModelId());
        this.blockEntity = blockEntity;
        this.events = new EyecandyEvents();
        this.outlineShape = null;
        this.soundManager = new ScriptSoundManager();
        this.renderManager = new ScriptRenderManager();
    }

    public ScriptRenderManager renderManager() {
        return this.renderManager;
    }

    public ScriptSoundManager soundManager() {
        return this.soundManager;
    }

    public EyecandyEvents events() {
        return events;
    }

    public VoxelShape getOutlineShape() {
        return this.outlineShape == null ? null : this.outlineShape.impl();
    }

    public VoxelShape getCollisionShape() {
        return this.collisionShape == null ? null : this.collisionShape.impl();
    }

    public void setOutlineShape(VoxelShapeWrapper voxelShapeWrapper) {
        this.outlineShape = voxelShapeWrapper;
    }

    public void setCollisionShape(VoxelShapeWrapper voxelShapeWrapper) {
        if(voxelShapeWrapper.impl().getBoundingBox().getMaxYMapped() > 1.5) {
            throw new IllegalStateException("Collision shape must not be larger than 1.5 blocks (24 unit)!");
        }
        this.collisionShape = voxelShapeWrapper;
    }

    @Deprecated
    public void drawModel(DisplayHelperCompat displayHelperCompat, Matrices matrices) {
        if(matrices != null) displayHelperCompat.matrices(matrices);
        renderManager.queue(displayHelperCompat);
    }

    public void drawModel(ScriptedModel model, Matrices matrices) {
        ModelDrawCall modelDrawCall = ModelDrawCall.create()
                .modelObject(model)
                .matrices(matrices);
        renderManager().queue(modelDrawCall);
    }

    public void playSound(Identifier id, float volume, float pitch) {
        soundManager().playSound(id, ScriptVector3f.ZERO, volume, pitch);
    }

    @Override
    public void resetForNextRun() {
        this.renderManager.reset();
        this.soundManager.reset();
    }
}
