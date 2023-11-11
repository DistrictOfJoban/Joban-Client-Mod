package com.lx862.jcm.mod.data.pids;

import com.google.gson.JsonObject;
import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.data.pids.base.PIDSPresetBase;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.render.data.McMetaManager;
import com.lx862.jcm.mod.render.data.TextOverflowMode;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.RenderLayer;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;

import javax.annotation.Nullable;

public class JsonPIDSPreset extends PIDSPresetBase {
    private final Identifier background;
    private final String fontId;
    private final boolean showClock;
    private final boolean showWeather;
    private final int textColor;
    private final TextOverflowMode textOverflowMode;
    private boolean[] rowVisibilityOverride;
    private static final int PIDS_MARGIN = 8;
    private static final float ARRIVAL_TEXT_SCALE = 1.35F;
    public JsonPIDSPreset(String id, @Nullable String name, @Nullable Identifier background, @Nullable String fontId, TextOverflowMode textOverflowMode, boolean showClock, boolean showWeather, int textColor) {
        super(id, name);
        this.background = background;
        this.showClock = showClock;
        this.showWeather = showWeather;
        this.textColor = textColor;
        // TODO: kcr_sign is temp
        this.fontId = fontId == null ? "jsblock:kcr_sign" : fontId;
        this.textOverflowMode = textOverflowMode;
    }

    public static JsonPIDSPreset parse(JsonObject jsonObject) {
        String id = jsonObject.get("id").getAsString();
        boolean showWeather = jsonObject.has("showWeather") && jsonObject.get("showWeather").getAsBoolean();
        boolean showClock = jsonObject.has("showWeather") && jsonObject.get("showWeather").getAsBoolean();
        int textColor = 0;
        String name = id;
        String font = null;
        Identifier background = null;
        TextOverflowMode textOverflowMode = TextOverflowMode.STRETCH;
        if(jsonObject.has("color")) {
            // TODO: Implement custom text color
        }
        if(jsonObject.has("name")) {
            name = jsonObject.get("name").getAsString();
        }
        if(jsonObject.has("font")) {
            font = jsonObject.get("font").getAsString();
        }
        if(jsonObject.has("textOverflowMode")) {
            textOverflowMode = TextOverflowMode.valueOf(jsonObject.get("textOverflowMode").getAsString());
        }
        if(jsonObject.has("background")) {
            String backgroundId = jsonObject.get("background").getAsString();

            if(backgroundId.contains(".mcmeta")) {
                McMetaManager.load(new Identifier(backgroundId));
                backgroundId = backgroundId.replace(".mcmeta", "");
            }
            background = new Identifier(backgroundId);
        }
        return new JsonPIDSPreset(id, name, background, font, textOverflowMode, showClock, showWeather, textColor);
    }

    @Override
    public void render(PIDSBlockEntity be, GraphicsHolder graphicsHolder, World world, Direction facing, float tickDelta, int x, int y, int width, int height, int light, int overlay) {
        int contentWidth = width - (PIDS_MARGIN * 2);
        int rowAmount = be.getRowAmount();

        // Draw Textures
        drawBackground(graphicsHolder, width, height, facing);
    }

    protected void drawBackground(GraphicsHolder graphicsHolder, int width, int height, Direction facing) {
        graphicsHolder.push();
        graphicsHolder.createVertexConsumer(RenderLayer.getBeaconBeam(background, false));
        RenderHelper.drawTexture(graphicsHolder, background, 0, 0, 0, width, height, facing, ARGB_WHITE, MAX_RENDER_LIGHT);
        graphicsHolder.pop();
    }
}
