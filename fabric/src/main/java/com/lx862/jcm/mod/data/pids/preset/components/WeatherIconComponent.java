package com.lx862.jcm.mod.data.pids.preset.components;

import com.google.gson.JsonObject;
import com.lx862.jcm.mod.data.KVPair;
import com.lx862.jcm.mod.data.pids.preset.PIDSContext;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
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
        super(x, y, width, height, additionalParam);
        this.iconSunny = additionalParam.getIdentifier("weatherIconSunny");
        this.iconRainy = additionalParam.getIdentifier("weatherIconRainy");
        this.iconThunder = additionalParam.getIdentifier("weatherIconThunder");
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, Direction facing, PIDSContext context) {
        Identifier textureId;
        if(context.world.isThundering()) {
            textureId = iconThunder;
        } else if(context.world.isRaining()) {
            textureId = iconRainy;
        } else {
            textureId = iconSunny;
        }

        if(textureId != null) {
            drawTexture(graphicsHolder, guiDrawing, facing, textureId, 0, 0, width, height, ARGB_WHITE);
        }
    }

    public static PIDSComponent parseComponent(double x, double y, double width, double height, JsonObject jsonObject) {
        return new WeatherIconComponent(x, y, width, height, new KVPair(jsonObject));
    }
}
