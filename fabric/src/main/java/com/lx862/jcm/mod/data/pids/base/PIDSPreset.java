package com.lx862.jcm.mod.data.pids.base;

import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;

public abstract class PIDSPreset {
    private String id;
    public PIDSPreset(String id) {
        this.id = id;
    }
    public abstract void render(PIDSBlockEntity be, GraphicsHolder graphicsHolder, World world, float tickDelta, int x, int y, int width, int height, int light, int overlay);
}
