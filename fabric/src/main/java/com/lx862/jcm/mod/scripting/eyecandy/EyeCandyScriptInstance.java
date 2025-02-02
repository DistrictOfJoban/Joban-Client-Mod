package com.lx862.jcm.mod.scripting.eyecandy;

import com.lx862.mtrscripting.scripting.ParsedScript;
import com.lx862.mtrscripting.scripting.base.ScriptInstance;
import org.mtr.mod.block.BlockEyeCandy;

import java.util.ArrayList;
import java.util.List;

public class EyeCandyScriptInstance extends ScriptInstance<BlockEyeCandy.BlockEntity> {
    private final BlockEyeCandy.BlockEntity be;
    public final List<ModelDrawCall> drawCalls;

    public EyeCandyScriptInstance(BlockEyeCandy.BlockEntity be, ParsedScript script) {
        super(new EyeCandyScriptContext(), script);
        this.be = be;
        this.drawCalls = new ArrayList<>();
    }

    public void setDrawCalls(List<ModelDrawCall> newDrawCalls) {
        this.drawCalls.clear();
        this.drawCalls.addAll(newDrawCalls);
    }

    public boolean isDead() {
        return be.isRemoved2();
    }
}
