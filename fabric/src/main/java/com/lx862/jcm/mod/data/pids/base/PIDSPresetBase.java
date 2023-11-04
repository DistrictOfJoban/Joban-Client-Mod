package com.lx862.jcm.mod.data.pids.base;

import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;

import javax.annotation.Nullable;

public abstract class PIDSPresetBase {
    private final String id;
    private final String name;
    public PIDSPresetBase(String id, @Nullable String name) {
        this.id = id;
        if(name == null) {
            this.name = id;
        } else {
            this.name = name;
        }
    }
    public PIDSPresetBase(String id) {
        this(id, null);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public abstract void render(PIDSBlockEntity be, GraphicsHolder graphicsHolder, World world, float tickDelta, int x, int y, int width, int height, int light, int overlay);
}