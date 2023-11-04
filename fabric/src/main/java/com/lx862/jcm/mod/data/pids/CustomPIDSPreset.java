package com.lx862.jcm.mod.data.pids;

import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.data.pids.base.PIDSPresetBase;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;

import javax.annotation.Nullable;

public class CustomPIDSPreset extends PIDSPresetBase {
    private Identifier thumbnail;
    private Identifier background;
    private boolean showClock;
    private boolean showWeather;
    public CustomPIDSPreset(String id, @Nullable String name, Identifier thumbnail, Identifier background, boolean showClock, boolean showWeather) {
        super(id, name);
        this.thumbnail = thumbnail;
        this.background = background;
        this.showClock = showClock;
        this.showWeather = showWeather;
    }

    @Override
    public void render(PIDSBlockEntity be, GraphicsHolder graphicsHolder, World world, float tickDelta, int x, int y, int width, int height, int light, int overlay) {

    }
}
