package com.lx862.jcm.mod.scripting.pids;

import com.lx862.mtrscripting.api.ScriptResultCall;
import com.lx862.mtrscripting.scripting.ParsedScript;
import com.lx862.mtrscripting.scripting.base.ScriptInstance;
import org.mtr.mapping.holder.BlockEntity;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public class PIDSScriptInstance extends ScriptInstance<PIDSWrapper> {
    private final BlockEntity be;
    public final List<ScriptResultCall> drawCalls;

    public PIDSScriptInstance(BlockPos pos, ParsedScript script, PIDSWrapper wrapperObject) {
        super(new PIDSScriptContext(), script);
        setWrapperObject(wrapperObject);
        this.be = MinecraftClient.getInstance().getWorldMapped().getBlockEntity(pos);
        this.drawCalls = new ArrayList<>();
    }

    public boolean isDead() {
        return be.isRemoved();
    }
}
