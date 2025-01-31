package com.lx862.jcm.mod.data.pids.scripting;

import com.lx862.mtrscripting.scripting.ParsedScript;
import com.lx862.mtrscripting.scripting.base.ScriptInstance;
import org.mtr.mapping.holder.BlockEntity;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public class PIDSScriptInstance extends ScriptInstance<PIDSWrapper> {
    public final List<DrawCall> drawCalls;
    private final BlockEntity be;

    public PIDSScriptInstance(String id, BlockPos pos, ParsedScript script) {
        super(id, new PIDSScriptContext(), script);
        this.be = MinecraftClient.getInstance().getWorldMapped().getBlockEntity(pos);
        this.drawCalls = new ArrayList<>();
    }

    @Override
    public PIDSWrapper getWrapperObject() {
        return wrapperObject;
    }

    public boolean isDead() {
        return be.isRemoved();
    }
}
