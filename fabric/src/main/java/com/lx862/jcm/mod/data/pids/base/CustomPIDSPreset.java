package com.lx862.jcm.mod.data.pids.base;

import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;

public abstract class CustomPIDSPreset {
    private String id;
    public CustomPIDSPreset(String id) {
        this.id = id;
    }
    public abstract void render(GraphicsHolder graphicsHolder, World world, float tickDelta, int x, int y, int width, int height, int light, int overlay);
}
