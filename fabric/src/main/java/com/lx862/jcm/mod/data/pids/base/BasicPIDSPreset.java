package com.lx862.jcm.mod.data.pids.base;

import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;

public abstract class BasicPIDSPreset extends PIDSPreset {
    private String thumbnail;
    private String background;
    public BasicPIDSPreset(String id, String thumbnail, String background) {
        super(id);
        this.thumbnail = thumbnail;
        this.background = background;
    }

    public abstract void render(GraphicsHolder graphicsHolder, World world, float tickDelta, int x, int y, int width, int height, int light, int overlay);
}
