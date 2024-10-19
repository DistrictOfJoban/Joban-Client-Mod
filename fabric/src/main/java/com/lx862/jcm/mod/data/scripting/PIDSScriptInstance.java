package com.lx862.jcm.mod.data.scripting;

import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.jcm.mod.data.scripting.base.ScriptInstance;
import org.mozilla.javascript.Scriptable;
import org.mtr.mapping.holder.BlockEntity;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public class PIDSScriptInstance extends ScriptInstance {
    public List<PIDSComponent> components;
    private final BlockEntity be;

    public PIDSScriptInstance(BlockPos pos, Scriptable scope) {
        super(scope);
        this.be = MinecraftClient.getInstance().getWorldMapped().getBlockEntity(pos);
        this.components = new ArrayList<>();
    }

    public boolean isDead() {
        return be.isRemoved();
    }
}
