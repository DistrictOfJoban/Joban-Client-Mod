package com.lx862.jcm.mod.data.pids.preset.components;

import com.lx862.jcm.mod.data.pids.preset.components.base.TextureComponent;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;

public class WeatherIconComponent extends TextureComponent {
    private final Identifier iconSunny;
    private final Identifier iconThunder;
    private final Identifier iconRainy;
    public WeatherIconComponent(Identifier iconSunny, Identifier iconRainy, Identifier iconThunder, double x, double y, double width, double height) {
        super(null, x, y, width, height);
        this.iconSunny = iconSunny;
        this.iconRainy = iconRainy;
        this.iconThunder = iconThunder;
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, World world, Direction facing) {
        Identifier textureId;
        if(world.isThundering()) {
            textureId = iconThunder;
        } else if(world.isRaining()) {
            textureId = iconRainy;
        } else {
            textureId = iconSunny;
        }
        drawTexture(graphicsHolder, facing, textureId, 0, 0, width, height, ARGB_WHITE);
    }
}
