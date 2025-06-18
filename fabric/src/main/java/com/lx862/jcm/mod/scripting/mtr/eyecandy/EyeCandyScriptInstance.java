package com.lx862.jcm.mod.scripting.mtr.eyecandy;

import com.lx862.jcm.mod.scripting.mtr.sound.PositionedSoundCall;
import com.lx862.mtrscripting.api.ScriptResultCall;
import com.lx862.mtrscripting.core.ParsedScript;
import com.lx862.mtrscripting.core.ScriptInstance;
import org.mtr.mod.block.BlockEyeCandy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EyeCandyScriptInstance extends ScriptInstance<BlockEyeCandy.BlockEntity> {
    private final BlockEyeCandy.BlockEntity be;
    public final List<ScriptResultCall> drawCalls;
    public final List<ScriptResultCall> soundCalls;

    public EyeCandyScriptInstance(EyeCandyScriptContext context, BlockEyeCandy.BlockEntity be, ParsedScript script) {
        super(context, script);
        this.be = be;
        this.soundCalls = new ArrayList<>();
        this.drawCalls = new ArrayList<>();
    }

    public void setDrawCalls(List<ModelDrawCall> newDrawCalls) {
        this.drawCalls.clear();
        this.drawCalls.addAll(newDrawCalls);
    }

    public void setSoundCalls(List<PositionedSoundCall> calls) {
        this.soundCalls.clear();
        this.soundCalls.addAll(calls);
    }

    public boolean shouldInvalidate() {
        return be.getWorld2().getBlockEntity(be.getPos2()) == null || !Objects.equals(be.getModelId(), getScriptContext().getName());
    }
}
