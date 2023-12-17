package com.lx862.jcm.mod.data.pids.preset;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.resources.mcmeta.McMetaManager;
import com.lx862.jcm.mod.render.TextOverflowMode;
import com.lx862.jcm.mod.trm.FontManager;
import com.lx862.jcm.mod.util.JCMLogger;
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
    private final boolean[] rowHidden;
    private static final int PIDS_MARGIN = 8;
    private static final float ARRIVAL_TEXT_SCALE = 1.35F;
    public JsonPIDSPreset(String id, @Nullable String name, @Nullable Identifier background, @Nullable String fontId, TextOverflowMode textOverflowMode, boolean[] rowHidden, boolean showClock, boolean showWeather, int textColor) {
        super(id, name, false);
        this.background = background;
        this.showClock = showClock;
        this.showWeather = showWeather;
        this.textColor = textColor;
        this.fontId = fontId == null ? "mtr:mtr" : fontId;
        this.rowHidden = rowHidden;
        this.textOverflowMode = textOverflowMode;
    }

    public static JsonPIDSPreset parse(JsonObject jsonObject) {
        String id = jsonObject.get("id").getAsString();
        boolean showWeather = jsonObject.has("showWeather") && jsonObject.get("showWeather").getAsBoolean();
        boolean showClock = jsonObject.has("showWeather") && jsonObject.get("showWeather").getAsBoolean();
        boolean[] rowHidden;

        int textColor = ARGB_BLACK;
        String name = id;
        String font = null;
        Identifier background = null;
        TextOverflowMode textOverflowMode = TextOverflowMode.STRETCH;
        if(jsonObject.has("color")) {
            textColor = (int)Long.parseLong("FF" + jsonObject.get("color").getAsString(), 16);
        }
        if(jsonObject.has("name")) {
            name = jsonObject.get("name").getAsString();
        }
        if(jsonObject.has("font")) {
            font = jsonObject.get("font").getAsString();
            FontManager.loadVanillaFont(font);
        }
        if(jsonObject.has("textOverflowMode")) {
            textOverflowMode = TextOverflowMode.valueOf(jsonObject.get("textOverflowMode").getAsString());
        }
        if(jsonObject.has("hideRow")) {
            JsonArray arr = jsonObject.getAsJsonArray("hideRow");
            rowHidden = new boolean[arr.size()];
            for(int i = 0; i < arr.size(); i++) {
                rowHidden[i] = arr.get(i).getAsBoolean();
            }
        } else {
            rowHidden = new boolean[]{};
        }
        if(jsonObject.has("background")) {
            Identifier backgroundId = new Identifier(jsonObject.get("background").getAsString());
            try {
                McMetaManager.load(backgroundId);
            } catch (Exception e) {
                e.printStackTrace();
                JCMLogger.warn("Failed to parse mcmeta animation file: " + backgroundId.getPath());
            }

            background = backgroundId;
        }
        return new JsonPIDSPreset(id, name, background, font, textOverflowMode, rowHidden, showClock, showWeather, textColor);
    }

    @Override
    public String getFont() {
        return fontId;
    }

    @Override
    public Identifier getBackground() {
        return background;
    }

    @Override
    public int getPreviewTextColor() {
        return textColor;
    }

    @Override
    public boolean isRowHidden(int row) {
        return rowHidden.length - 1 < row ? false : rowHidden[row];
    }

    @Override
    public void render(PIDSBlockEntity be, GraphicsHolder graphicsHolder, World world, Direction facing, float tickDelta, int x, int y, int width, int height, int light, int overlay) {
        int contentWidth = width - (PIDS_MARGIN * 2);
        int rowAmount = be.getRowAmount();

        // Draw Textures
        drawBackground(graphicsHolder, width, height, facing);
    }

    protected void drawBackground(GraphicsHolder graphicsHolder, int width, int height, Direction facing) {
        if(background == null) return;
        graphicsHolder.createVertexConsumer(RenderLayer.getBeaconBeam(background, false));
        RenderHelper.drawTexture(graphicsHolder, background, 0, 0, 0, width, height, facing, ARGB_WHITE, MAX_RENDER_LIGHT);
    }
}
