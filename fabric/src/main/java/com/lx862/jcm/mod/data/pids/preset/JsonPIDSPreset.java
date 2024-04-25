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
import com.lx862.jcm.mod.data.pids.preset.components.base.TextureComponent;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.render.text.TextAlignment;
import com.lx862.jcm.mod.render.text.TextRenderingManager;
import com.lx862.jcm.mod.resources.mcmeta.McMetaManager;
import com.lx862.jcm.mod.render.TextOverflowMode;
import com.lx862.jcm.mod.render.text.font.FontManager;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.core.operation.ArrivalsResponse;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.RenderLayer;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JsonPIDSPreset extends PIDSPresetBase {
    private static final int PIDS_MARGIN = 7;
    private static final float ARRIVAL_TEXT_SCALE = 1.35F;
    private static final int HEADER_HEIGHT = 9;
    private static final String ICON_WEATHER_SUNNY = Constants.MOD_ID + ":textures/block/pids/weather_sunny.png";
    private static final String ICON_WEATHER_RAINY = Constants.MOD_ID + ":textures/block/pids/weather_rainy.png";
    private static final String ICON_WEATHER_THUNDER = Constants.MOD_ID + ":textures/block/pids/weather_thunder.png";
    private static final Identifier TEXTURE_PLATFORM_CIRCLE = new Identifier(Constants.MOD_ID, "textures/block/pids/plat_circle.png");
    protected Identifier background;
    protected String fontId;
    private final TextOverflowMode textOverflowMode;
    protected boolean showClock;
    protected boolean showWeather;
    protected boolean topPadding;
    protected int textColor;
    protected boolean[] rowHidden;

    public JsonPIDSPreset(String id, @Nullable String name, @Nullable Identifier background, @Nullable String fontId, TextOverflowMode textOverflowMode, boolean[] rowHidden, boolean showClock, boolean showWeather, boolean topPadding, int textColor) {
        super(id, name, false);
        this.background = background;
        this.showClock = showClock;
        this.showWeather = showWeather;
        this.textColor = textColor;
        this.fontId = fontId == null ? "mtr:mtr" : fontId;
        this.rowHidden = rowHidden;
        this.topPadding = topPadding;
        this.textOverflowMode = textOverflowMode;
    }

    public static JsonPIDSPreset parse(JsonObject jsonObject) {
        String id = jsonObject.get("id").getAsString();
        boolean showWeather = jsonObject.has("showWeather") && jsonObject.get("showWeather").getAsBoolean();
        boolean showClock = jsonObject.has("showClock") && jsonObject.get("showClock").getAsBoolean();
        boolean topPadding = !jsonObject.has("topPadding") ? true : jsonObject.get("topPadding").getAsBoolean();
        boolean[] rowHidden;

        int textColor = ARGB_BLACK;
        String name = id;
        String font = null;
        Identifier background = null;
        TextOverflowMode textOverflowMode = TextOverflowMode.STRETCH;
        if(jsonObject.has("color")) {
            String textColorString = jsonObject.get("color").getAsString();
            if(textColorString.length() < 8) {
                textColorString = "FF" + textColorString;
            }
            textColor = (int)Long.parseLong(textColorString, 16);
        }
        if(jsonObject.has("name")) {
            name = jsonObject.get("name").getAsString();
        }
        if(jsonObject.has("fonts")) {
            font = jsonObject.get("fonts").getAsString();
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
        return new JsonPIDSPreset(id, name, background, font, textOverflowMode, rowHidden, showClock, showWeather, topPadding, textColor);
    }

    @Override
    public void render(PIDSBlockEntity be, GraphicsHolder graphicsHolder, World world, Direction facing, ArrivalsResponse arrivals, boolean[] rowHidden, float tickDelta, int x, int y, int width, int height) {
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

        graphicsHolder.translate(startX, 0, -0.5);

        List<PIDSComponent> components = getComponents(arrivals, be.getCustomMessages(), rowHidden, 0, headerHeight + 6, contentWidth, contentHeight, be.getRowAmount(), be.platformNumberHidden());
        List<PIDSComponent> textureComponents = components.stream().filter(e -> e instanceof TextureComponent).collect(Collectors.toList());
        List<PIDSComponent> textComponents = components.stream().filter(e -> e instanceof TextComponent).collect(Collectors.toList());

        // Texture
        for(PIDSComponent component : textureComponents) {
            graphicsHolder.push();
            component.render(graphicsHolder, null, facing, new PIDSContext(world, arrivals));
            graphicsHolder.pop();
        }

        // Text
        graphicsHolder.translate(0, 0, -0.5);
        TextRenderingManager.bind(graphicsHolder);
        for(PIDSComponent component : textComponents) {
            graphicsHolder.push();
            component.render(graphicsHolder, null, facing, new PIDSContext(world, arrivals));
            graphicsHolder.pop();
        }
    }

    @Override
    public List<PIDSComponent> getComponents(ArrivalsResponse arrivals, String[] customMessages, boolean[] rowHidden, int x, int y, int screenWidth, int screenHeight, int rows, boolean hidePlatform) {
        List<PIDSComponent> components = new ArrayList<>();

        if(showClock) {
            components.add(new ClockComponent(screenWidth, 2, screenWidth, 10, getFont(), TextAlignment.RIGHT, TextOverflowMode.STRETCH, ARGB_WHITE, 1, new KVPair()));
        }

        if(showWeather) {
            components.add(new WeatherIconComponent(0, 0, 11, 11, new KVPair()
                    .with("weatherIconSunny", ICON_WEATHER_SUNNY)
                    .with("weatherIconRainy", ICON_WEATHER_RAINY)
                    .with("weatherIconThunder", ICON_WEATHER_THUNDER)));
        }

        /* Arrivals */
        int arrivalIndex = 0;
        double rowY = y;
        for(int i = 0; i < rows; i++) {
            if(arrivalIndex >= arrivals.getArrivals().size()) continue;

            if(!customMessages[i].isEmpty()) {
                components.add(new CustomTextComponent(x, rowY, 78 * ARRIVAL_TEXT_SCALE, 10, getFont(), TextAlignment.LEFT, textOverflowMode, getTextColor(), ARRIVAL_TEXT_SCALE, new KVPair().with("text", customMessages[i])));
            } else {
                if (!rowHidden[i]) {
                    float destinationMaxWidth = !hidePlatform ? (44 * ARRIVAL_TEXT_SCALE) : (54 * ARRIVAL_TEXT_SCALE);
                    components.add(new DestinationComponent(x, rowY, destinationMaxWidth, 10, getFont(), TextAlignment.LEFT, textOverflowMode, textColor, ARRIVAL_TEXT_SCALE, new KVPair().with("arrivalIndex", arrivalIndex)));

                    if (!hidePlatform) {
                        components.add(new PlatformComponent(arrivals, arrivalIndex, getFont(), RenderHelper.ARGB_WHITE, 64 * ARRIVAL_TEXT_SCALE, rowY, 9, 9));
                        components.add(new PlatformCircleComponent(64 * ARRIVAL_TEXT_SCALE, rowY, 11, 11, new KVPair().with("textureId", TEXTURE_PLATFORM_CIRCLE).with("arrivalIndex", arrivalIndex) ));
                    }

                    components.add(new ETAComponent(screenWidth, rowY, 22 * ARRIVAL_TEXT_SCALE, 20, getFont(), TextAlignment.RIGHT, TextOverflowMode.STRETCH, textColor, ARRIVAL_TEXT_SCALE,
                            new KVPair().with("arrivalIndex", arrivalIndex)));
                    arrivalIndex++;
                }
            }

            rowY += (screenHeight / 5.25) * ARRIVAL_TEXT_SCALE;
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
        return textColor;
    }

    @Override
    public boolean isRowHidden(int row) {
        return rowHidden.length - 1 < row ? false : rowHidden[row];
    }

    public boolean getShowWeather() {
        return showWeather;
    }

    public boolean getShowClock() {
        return showClock;
    }

    public boolean getTopPadding() {
        return topPadding;
    }

    public MutableJsonPIDSPreset toMutable() {
        return new MutableJsonPIDSPreset(getId(), getName(), background, fontId, textOverflowMode, rowHidden, showClock, showWeather, topPadding, textColor);
    }
}
