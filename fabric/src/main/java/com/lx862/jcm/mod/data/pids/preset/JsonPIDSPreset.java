package com.lx862.jcm.mod.data.pids.preset;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.JCMClient;
import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.data.KVPair;
import com.lx862.jcm.mod.data.pids.preset.components.*;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.jcm.mod.data.pids.preset.components.base.TextComponent;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.render.text.TextAlignment;
import com.lx862.jcm.mod.render.text.TextOverflowMode;
import com.lx862.jcm.mod.render.text.TextTranslationMode;
import com.lx862.jcm.mod.resources.mcmeta.McMetaManager;
import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.data.IGui;

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
    private final @Nonnull Identifier background;
    private final String fontId;
    private final TextOverflowMode textOverflowMode;
    private final boolean showClock;
    private final boolean showWeather;
    private final boolean topPadding;
    private final boolean hidePlatform;
    private final int textColor;
    private final boolean[] rowHidden;

    public JsonPIDSPreset(String id, @Nullable String name, @Nonnull Identifier background, Identifier thumbnail, @Nullable String fontId, TextOverflowMode textOverflowMode, boolean[] rowHidden, boolean showClock, boolean showWeather, boolean topPadding, boolean builtin, boolean hidePlatform, int textColor) {
        super(id, name, thumbnail, builtin);
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

    public static JsonPIDSPreset parse(JsonObject rootJsonObject) {
        String id = rootJsonObject.get("id").getAsString();
        boolean showWeather = rootJsonObject.has("showWeather") && rootJsonObject.get("showWeather").getAsBoolean();
        boolean showClock = rootJsonObject.has("showClock") && rootJsonObject.get("showClock").getAsBoolean();
        boolean topPadding = !rootJsonObject.has("topPadding") ? true : rootJsonObject.get("topPadding").getAsBoolean();
        boolean hidePlatform = rootJsonObject.has("hidePlatform") && rootJsonObject.get("hidePlatform").getAsBoolean();
        boolean[] rowHidden;

        int textColor = ARGB_BLACK;
        String name = id;
        String font = null;
        Identifier background = null;
        TextOverflowMode textOverflowMode = TextOverflowMode.STRETCH;
        if(rootJsonObject.has("color")) {
            textColor = (int)Long.parseLong(rootJsonObject.get("color").getAsString(), 16);
        }
        if(rootJsonObject.has("name")) {
            name = rootJsonObject.get("name").getAsString();
        }
        if(rootJsonObject.has("fonts")) {
            font = rootJsonObject.get("fonts").getAsString();
        }
        if(rootJsonObject.has("textOverflowMode")) {
            textOverflowMode = TextOverflowMode.valueOf(rootJsonObject.get("textOverflowMode").getAsString());
        }
        if(rootJsonObject.has("hideRow")) {
            JsonArray arr = rootJsonObject.getAsJsonArray("hideRow");
            rowHidden = new boolean[arr.size()];
            for(int i = 0; i < arr.size(); i++) {
                rowHidden[i] = arr.get(i).getAsBoolean();
            }
        } else {
            rowHidden = new boolean[]{};
        }
        if(rootJsonObject.has("background")) {
            Identifier backgroundId = new Identifier(rootJsonObject.get("background").getAsString());
            try {
                McMetaManager.load(backgroundId);
            } catch (Exception e) {
                e.printStackTrace();
                JCMLogger.warn("Failed to parse mcmeta animation file: " + backgroundId.getPath());
            }

            background = backgroundId;
        }
        Identifier thumbnail = rootJsonObject.has("thumbnail") ? new Identifier(rootJsonObject.get("thumbnail").getAsString()) : background;
        boolean builtin = rootJsonObject.has("builtin") && rootJsonObject.get("builtin").getAsBoolean();
        return new JsonPIDSPreset(id, name, background, thumbnail, font, textOverflowMode, rowHidden, showClock, showWeather, topPadding, builtin, hidePlatform, textColor);
    }

    @Override
    public void render(PIDSBlockEntity be, GraphicsHolder graphicsHolder, World world, BlockPos pos, Direction facing, ObjectArrayList<ArrivalResponse> arrivals, boolean[] rowHidden, float tickDelta, int x, int y, int width, int height) {
        int headerHeight = topPadding ? HEADER_HEIGHT : 0;
        int startX = PIDS_MARGIN;
        int contentWidth = width - (PIDS_MARGIN * 2);
        int contentHeight = height - headerHeight - 3;

        // Draw Background
        graphicsHolder.createVertexConsumer(RenderLayer.getText(background));
        RenderHelper.drawTexture(graphicsHolder, background, x, y, 0, width, height, facing, ARGB_WHITE, MAX_RENDER_LIGHT);

        // Debug View Texture
        if(JCMClient.getConfig().debug) {
            //TextureTextRenderer.stressTest(5);
            drawAtlasBackground(graphicsHolder, width, height, facing);
        }

        graphicsHolder.translate(startX, 0, -0.05);

        List<PIDSComponent> components = getComponents(arrivals, be.getCustomMessages(), rowHidden, x, y, contentWidth, contentHeight, be.getRowAmount(), be.platformNumberHidden());
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
            components.add(new ClockComponent(x + screenWidth, y + 2, screenWidth, 10, TextComponent.of(TextAlignment.RIGHT, TextOverflowMode.STRETCH, getFont(), ARGB_WHITE, 0.9)));
        }

        if(showWeather) {
            components.add(new WeatherIconComponent(x, y, 9, 9, new KVPair()
                    .with("weatherIconSunny", ICON_WEATHER_SUNNY)
                    .with("weatherIconRainy", ICON_WEATHER_RAINY)
                    .with("weatherIconThunder", ICON_WEATHER_THUNDER)));
        }

        boolean platformShown = !hidePlatform && !this.hidePlatform;
        boolean showCar = false;
        int tmpCar = -1;
        for(ArrivalResponse arrival : arrivals) {
            if(tmpCar == -1) {
                tmpCar = arrival.getCarCount();
                continue;
            }

            if(tmpCar != arrival.getCarCount()) {
                showCar = true;
            }
        }

        /* Arrivals */
        int arrivalIndex = 0;
        double rowY = y + (topPadding ? HEADER_HEIGHT : 0) + 4;
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

                    // ETA text cycle should follow destination
                    ArrivalResponse thisArrival = arrivals.get(arrivalIndex);
                    boolean haveCjk = IGui.isCjk(thisArrival.getDestination());
                    boolean haveEn = TextUtil.haveNonCjk(thisArrival.getDestination());
                    TextTranslationMode mode = haveCjk && haveEn ? TextTranslationMode.CYCLE : haveCjk ? TextTranslationMode.CJK : TextTranslationMode.NON_CJK;
                    ArrivalETAComponent eta = new ArrivalETAComponent(screenWidth, rowY, 22 * ARRIVAL_TEXT_SCALE, 20, TextComponent.of(TextAlignment.RIGHT, TextOverflowMode.STRETCH, getFont(), textColor, ARRIVAL_TEXT_SCALE)
                            .with("arrivalIndex", arrivalIndex)
                            .with("textTranslationMode", mode.name())
                    );

                    ArrivalCarComponent car = new ArrivalCarComponent(screenWidth, rowY, 22 * ARRIVAL_TEXT_SCALE, 20, TextComponent.of(TextAlignment.RIGHT, TextOverflowMode.STRETCH, getFont(), textColor, ARRIVAL_TEXT_SCALE)
                            .with("arrivalIndex", arrivalIndex)
                            .with("textTranslationMode", mode.name())
                    );

                    if(showCar) {
                        components.add(new CycleComponent(new KVPair().with("cycleTime", 80), eta, car));
                    } else {
                        components.add(eta);
                    }
                    arrivalIndex++;
                }
            }

            rowY += (screenHeight / 4.7) * ARRIVAL_TEXT_SCALE;
        }
        return components;
    }

    public String getFont() {
        return fontId;
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
