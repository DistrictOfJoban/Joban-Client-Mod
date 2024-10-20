package com.lx862.jcm.mod.data.pids.scripting;

import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.jcm.mod.data.scripting.ParsedScript;
import com.lx862.jcm.mod.data.scripting.base.ScriptInstance;
import org.mtr.mapping.holder.BlockEntity;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public class PIDSScriptInstance extends ScriptInstance {
    public List<PIDSComponent> components;
    private final BlockEntity be;
    private final PIDSWrapper wrapperObject;

    public PIDSScriptInstance(BlockPos pos, ParsedScript script, PIDSWrapper wrapperObject) {
        super(new PIDSScriptContext(), script);
        this.wrapperObject = wrapperObject;
        this.be = MinecraftClient.getInstance().getWorldMapped().getBlockEntity(pos);
        this.components = new ArrayList<>();
    }

    @Override
    public Object getWrapperObject() {
        return wrapperObject;
    }

    public boolean isDead() {
        return be.isRemoved();
    }
}
