package com.lx862.jcm.mod.data.pids.preset.components;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.data.KVPair;
import com.lx862.jcm.mod.data.pids.preset.components.base.TextureComponent;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

public class WeatherIconComponent extends TextureComponent {
    private final Identifier iconSunny;
    private final Identifier iconThunder;
    private final Identifier iconRainy;
    public WeatherIconComponent(double x, double y, double width, double height, KVPair additionalParam) {
        super(null, x, y, width, height);
        this.iconSunny = new Identifier(additionalParam.get("weatherIconSunny", (String)null));
        this.iconRainy = new Identifier(additionalParam.get("weatherIconRainy", (String)null));
        this.iconThunder = new Identifier(additionalParam.get("weatherIconThunder", (String)null));
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, World world, Direction facing, KVPair context) {
        Identifier textureId;
        if(world.isThundering()) {
            textureId = iconThunder;
        } else if(world.isRaining()) {
            textureId = iconRainy;
        } else {
            textureId = iconSunny;
        }

        if(textureId != null) {
            drawTexture(graphicsHolder, guiDrawing, facing, textureId, 0, 0, width, height, ARGB_WHITE);
        }
    }
}
