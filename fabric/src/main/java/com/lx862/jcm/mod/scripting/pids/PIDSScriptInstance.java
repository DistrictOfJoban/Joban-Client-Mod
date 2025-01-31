package com.lx862.jcm.mod.scripting.pids;

import com.lx862.mtrscripting.scripting.ParsedScript;
import com.lx862.mtrscripting.scripting.base.ScriptInstance;
import org.mtr.mapping.holder.BlockEntity;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public class PIDSScriptInstance extends ScriptInstance<PIDSWrapper> {
    private final BlockEntity be;
    public final List<PIDSDrawCall> drawCalls;

    public PIDSScriptInstance(String id, BlockPos pos, ParsedScript script) {
        super(id, new PIDSScriptContext(), script);
        this.be = MinecraftClient.getInstance().getWorldMapped().getBlockEntity(pos);
        this.drawCalls = new ArrayList<>();
    }

    public boolean isDead() {
        return be.isRemoved();
    }
}
