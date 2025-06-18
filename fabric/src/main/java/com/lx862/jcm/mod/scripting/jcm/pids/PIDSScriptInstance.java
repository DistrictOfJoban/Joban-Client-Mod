package com.lx862.jcm.mod.scripting.jcm.pids;

import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.mtrscripting.api.ScriptResultCall;
import com.lx862.mtrscripting.core.ParsedScript;
import com.lx862.mtrscripting.core.ScriptInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PIDSScriptInstance extends ScriptInstance<PIDSWrapper> {
    private final PIDSBlockEntity blockEntity;
    public final List<ScriptResultCall> drawCalls;

    public PIDSScriptInstance(PIDSBlockEntity blockEntity, ParsedScript script, PIDSWrapper wrapperObject) {
        super(new PIDSScriptContext(blockEntity.getPresetId()), script);
        setWrapperObject(wrapperObject);
        this.blockEntity = blockEntity;
        this.drawCalls = new ArrayList<>();
    }

    public boolean shouldInvalidate() {
        return blockEntity.getWorld2().getBlockEntity(blockEntity.getPos2()) == null || !Objects.equals(blockEntity.getPresetId(), getScriptContext().getName());
    }
}
