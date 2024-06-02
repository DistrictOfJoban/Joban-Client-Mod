package com.lx862.jcm.mod.data.pids.preset;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.config.ConfigEntry;
import com.lx862.jcm.mod.data.KVPair;
import com.lx862.jcm.mod.data.pids.preset.components.*;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.jcm.mod.data.pids.preset.components.base.TextComponent;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.render.text.TextOverflowMode;
import com.lx862.jcm.mod.render.text.TextAlignment;
import com.lx862.jcm.mod.resources.mcmeta.McMetaManager;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.GraphicsHolder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class JsonPIDSPreset extends PIDSPresetBase {
    private static final int PIDS_MARGIN = 7;
    private static final float ARRIVAL_TEXT_SCALE = 1.225F;
    private static final int HEADER_HEIGHT = 9;
    private static final String ICON_WEATHER_SUNNY = Constants.MOD_ID + ":textures/block/pids/weather_sunny.png";
    private static final String ICON_WEATHER_RAINY = Constants.MOD_ID + ":textures/block/pids/weather_rainy.png";
    private static final String ICON_WEATHER_THUNDER = Constants.MOD_ID + ":textures/block/pids/weather_thunder.png";
    private static final String TEXTURE_PLATFORM_CIRCLE = Constants.MOD_ID + ":textures/block/pids/plat_circle.png";
    private final Identifier background;
    private final String fontId;
    private final TextOverflowMode textOverflowMode;
    private final boolean showClock;
    private final boolean showWeather;
    private final boolean topPadding;
    private final boolean hidePlatform;
    private final int textColor;
    private final boolean[] rowHidden;

    public JsonPIDSPreset(String id, @Nullable String name, @Nullable Identifier background, @Nullable String fontId, TextOverflowMode textOverflowMode, boolean[] rowHidden, boolean showClock, boolean showWeather, boolean topPadding, boolean builtin, boolean hidePlatform, int textColor) {
        super(id, name, builtin);
        this.background = background;
        this.showClock = showClock;
        this.showWeather = showWeather;
        this.textColor = textColor;
        this.fontId = fontId == null ? "mtr:mtr" : fontId;
        this.rowHidden = rowHidden;
        this.topPadding = topPadding;
        this.textOverflowMode = textOverflowMode;
        this.hidePlatform = hidePlatform;
    }

    public static JsonPIDSPreset parse(JsonObject jsonObject) {
        String id = jsonObject.get("id").getAsString();
        boolean showWeather = jsonObject.has("showWeather") && jsonObject.get("showWeather").getAsBoolean();
        boolean showClock = jsonObject.has("showClock") && jsonObject.get("showClock").getAsBoolean();
        boolean topPadding = !jsonObject.has("topPadding") ? true : jsonObject.get("topPadding").getAsBoolean();
        boolean hidePlatform = jsonObject.has("hidePlatform") && jsonObject.get("hidePlatform").getAsBoolean();
        boolean[] rowHidden;

        int textColor = ARGB_BLACK;
        String name = id;
        String font = null;
        Identifier background = null;
        TextOverflowMode textOverflowMode = TextOverflowMode.STRETCH;
        if(jsonObject.has("color")) {
            textColor = (int)Long.parseLong(jsonObject.get("color").getAsString(), 16);
        }
        if(jsonObject.has("name")) {
            name = jsonObject.get("name").getAsString();
        }
        if(jsonObject.has("fonts")) {
            font = jsonObject.get("fonts").getAsString();
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
        boolean builtin = jsonObject.has("builtin") && jsonObject.get("builtin").getAsBoolean();
        return new JsonPIDSPreset(id, name, background, font, textOverflowMode, rowHidden, showClock, showWeather, topPadding, builtin, hidePlatform, textColor);
    }

    @Override
    public void render(PIDSBlockEntity be, GraphicsHolder graphicsHolder, World world, BlockPos pos, Direction facing, ObjectArrayList<ArrivalResponse> arrivals, boolean[] rowHidden, float tickDelta, int x, int y, int width, int height) {
        int headerHeight = topPadding ? HEADER_HEIGHT : 0;
        int startX = PIDS_MARGIN;
        int contentWidth = width - (PIDS_MARGIN * 2);
        int contentHeight = height - headerHeight - 3;

        // Draw Background
        graphicsHolder.createVertexConsumer(RenderLayer.getText(getBackground()));
        RenderHelper.drawTexture(graphicsHolder, getBackground(), 0, 0, 0, width, height, facing, ARGB_WHITE, MAX_RENDER_LIGHT);

        // Debug View Texture
        if(ConfigEntry.DEBUG_MODE.getBool() && ConfigEntry.NEW_TEXT_RENDERER.getBool()) {
            //TextureTextRenderer.stressTest(5);
            drawAtlasBackground(graphicsHolder, width, height, facing);
        }

        graphicsHolder.translate(startX, 0, -0.05);

        List<PIDSComponent> components = getComponents(arrivals, be.getCustomMessages(), rowHidden, 0, headerHeight + 4, contentWidth, contentHeight, be.getRowAmount(), be.platformNumberHidden());
        PIDSContext pidsContext = new PIDSContext(world, pos, be.getCustomMessages(), arrivals, tickDelta);

        // Texture
        graphicsHolder.push();
        for(PIDSComponent component : components) {
            graphicsHolder.translate(0, 0, -0.02);
            graphicsHolder.push();
            component.render(graphicsHolder, null, facing, pidsContext);
            graphicsHolder.pop();
        }
        graphicsHolder.pop();
    }

    @Override
    public List<PIDSComponent> getComponents(ObjectArrayList<ArrivalResponse> arrivals, String[] customMessages, boolean[] rowHidden, int x, int y, int screenWidth, int screenHeight, int rows, boolean hidePlatform) {
        List<PIDSComponent> components = new ArrayList<>();

        if(showClock) {
            components.add(new ClockComponent(screenWidth, 2, screenWidth, 10, TextComponent.of(TextAlignment.RIGHT, TextOverflowMode.STRETCH, getFont(), ARGB_WHITE, 0.9)));
        }

        if(showWeather) {
            components.add(new WeatherIconComponent(0, 0, 9, 9, new KVPair()
                    .with("weatherIconSunny", ICON_WEATHER_SUNNY)
                    .with("weatherIconRainy", ICON_WEATHER_RAINY)
                    .with("weatherIconThunder", ICON_WEATHER_THUNDER)));
        }

        boolean platformShown = !hidePlatform && !this.hidePlatform;

        /* Arrivals */
        int arrivalIndex = 0;
        double rowY = y;
        for(int i = 0; i < rows; i++) {
            if(customMessages[i] != null && !customMessages[i].isEmpty()) {
                components.add(new CustomTextComponent(x, rowY, 78 * ARRIVAL_TEXT_SCALE, 10, TextComponent.of(TextAlignment.LEFT, textOverflowMode, getFont(), getTextColor(), ARRIVAL_TEXT_SCALE).with("text", customMessages[i])));
            } else {
                if(arrivalIndex >= arrivals.size()) continue;

                if (!rowHidden[i]) {
                    float destinationMaxWidth = platformShown ? (44 * ARRIVAL_TEXT_SCALE) : (54 * ARRIVAL_TEXT_SCALE);
                    components.add(new ArrivalDestinationComponent(x, rowY, destinationMaxWidth, 10, TextComponent.of(TextAlignment.LEFT, textOverflowMode, getFont(), textColor, ARRIVAL_TEXT_SCALE).with("arrivalIndex", arrivalIndex)));

                    if (platformShown) {
                        components.add(new ArrivalTextureComponent(59 * ARRIVAL_TEXT_SCALE, rowY, 10, 10, new KVPair().with("textureId", TEXTURE_PLATFORM_CIRCLE).with("arrivalIndex", arrivalIndex)));
                        components.add(new PlatformComponent(59 * ARRIVAL_TEXT_SCALE, rowY, 8, 8, getFont(), RenderHelper.ARGB_WHITE, 0.85, new KVPair().with("arrivalIndex", arrivalIndex)));
                    }

                    components.add(new ArrivalETAComponent(screenWidth, rowY, 22 * ARRIVAL_TEXT_SCALE, 20, TextComponent.of(TextAlignment.RIGHT, TextOverflowMode.STRETCH, getFont(), textColor, ARRIVAL_TEXT_SCALE).with("arrivalIndex", arrivalIndex)));
                    arrivalIndex++;
                }
            }

            rowY += (screenHeight / 4.7) * ARRIVAL_TEXT_SCALE;
        }
        return components;
    }

    @Override
    public String getFont() {
        return fontId;
    }

    @Override
    public @Nonnull Identifier getBackground() {
        return background;
    }

    @Override
    public int getTextColor() {
        return 0xFF000000 | textColor;
    }

    @Override
    public boolean isRowHidden(int row) {
        return rowHidden.length - 1 < row ? false : rowHidden[row];
    }
}