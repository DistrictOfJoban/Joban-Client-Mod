package com.lx862.jcm.mod.scripting.mtr.eyecandy;

import com.lx862.jcm.mod.scripting.mtr.MTRScriptContext;
import com.lx862.jcm.mod.scripting.mtr.render.ModelDrawCall;
import com.lx862.jcm.mod.scripting.mtr.util.ScriptedModel;
import com.lx862.mtrscripting.util.Matrices;
import com.lx862.mtrscripting.util.Vector3dWrapper;
import org.mtr.mapping.holder.Identifier;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class EyeCandyScriptContext extends MTRScriptContext {
    private final EyecandyBlockEntityWrapper blockEntity;
    private final List<ModelDrawCall> drawCalls = new ArrayList<>();
    private final EyecandyEvents events;

    public EyeCandyScriptContext(EyecandyBlockEntityWrapper blockEntity) {
        super(blockEntity.getModelId());
        this.blockEntity = blockEntity;
        this.events = new EyecandyEvents();
    }

    public EyecandyEvents events() {
        return events;
    }

    public void drawModel(ScriptedModel model, Matrices matrices) {
        ModelDrawCall modelDrawCall = ModelDrawCall.create()
                .modelObject(model)
                .matrices(matrices);
        renderManager().queue(modelDrawCall);
    }

    public void playSound(Identifier id, float volume, float pitch) {
        soundManager().playSound(id, Vector3dWrapper.ZERO, volume, pitch);
    }
}
