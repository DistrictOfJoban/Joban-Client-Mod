package com.lx862.jcm.mod.scripting.eyecandy;

import com.lx862.jcm.mod.scripting.SoundCall;
import com.lx862.mtrscripting.api.ScriptResultCall;
import com.lx862.mtrscripting.scripting.ParsedScript;
import com.lx862.mtrscripting.scripting.base.ScriptInstance;
import org.mtr.mod.block.BlockEyeCandy;

import java.util.ArrayList;
import java.util.List;

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

    public void setSoundCalls(List<SoundCall> calls) {
        this.soundCalls.clear();
        this.soundCalls.addAll(calls);
    }

    public boolean isDead() {
        return be.isRemoved2();
    }
}
