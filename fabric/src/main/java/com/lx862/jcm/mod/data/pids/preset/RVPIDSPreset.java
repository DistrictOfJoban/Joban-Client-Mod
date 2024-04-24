package com.lx862.jcm.mod.data.pids.preset;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.config.ConfigEntry;
import com.lx862.jcm.mod.data.KVPair;
import com.lx862.jcm.mod.data.pids.preset.components.*;
import com.lx862.jcm.mod.data.pids.preset.components.base.TextComponent;
import com.lx862.jcm.mod.data.pids.preset.components.base.TextureComponent;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.jcm.mod.render.TextOverflowMode;
import com.lx862.jcm.mod.render.text.TextAlignment;
import com.lx862.jcm.mod.render.text.TextRenderingManager;
import org.mtr.core.operation.ArrivalsResponse;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.GraphicsHolder;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RVPIDSPreset extends PIDSPresetBase {
    private static final int HEADER_HEIGHT = 9;
    private static final int PIDS_MARGIN = 7;
    private static final float ARRIVAL_TEXT_SCALE = 1.35F;
    private static final Identifier TEXTURE_PLATFORM_CIRCLE = new Identifier(Constants.MOD_ID, "textures/block/pids/plat_circle.png");
    private static final Identifier TEXTURE_BACKGROUND = new Identifier(Constants.MOD_ID, "textures/block/pids/rv_default.png");
    private static final Identifier ICON_WEATHER_SUNNY = new Identifier(Constants.MOD_ID, "textures/block/pids/weather_sunny.png");
    private static final Identifier ICON_WEATHER_RAINY = new Identifier(Constants.MOD_ID, "textures/block/pids/weather_rainy.png");
    private static final Identifier ICON_WEATHER_THUNDER = new Identifier(Constants.MOD_ID, "textures/block/pids/weather_thunder.png");
    public RVPIDSPreset() {
        super("rv_pids", "Hong Kong Railway Vision PIDS", true);
    }

    protected RVPIDSPreset(String id, String name) {
        super(id, name, true);
    }

    @Override
    public void render(PIDSBlockEntity be, GraphicsHolder graphicsHolder, World world, Direction facing, ArrivalsResponse arrivals, boolean[] rowHidden, float tickDelta, int x, int y, int width, int height) {
        int startX = PIDS_MARGIN;
        int contentWidth = width - (PIDS_MARGIN * 2);
        int contentHeight = height - HEADER_HEIGHT - 3;

        // Draw Background
        graphicsHolder.createVertexConsumer(RenderLayer.getText(getBackground()));
        RenderHelper.drawTexture(graphicsHolder, getBackground(), 0, 0, 0, width, height, facing, ARGB_WHITE, MAX_RENDER_LIGHT);

        // Debug View Texture
        if(ConfigEntry.DEBUG_MODE.getBool() && ConfigEntry.NEW_TEXT_RENDERER.getBool()) {
            //TextureTextRenderer.stressTest(5);
            drawAtlasBackground(graphicsHolder, width, height, facing);
        }

        graphicsHolder.translate(startX, 0, -0.5);

        List<PIDSComponent> components = getComponents(arrivals, be.getCustomMessages(), rowHidden, 0, HEADER_HEIGHT + 6, contentWidth, contentHeight, be.getRowAmount(), be.platformNumberHidden());

        List<PIDSComponent> textureComponents = components.stream().filter(e -> e instanceof TextureComponent).collect(Collectors.toList());
        List<PIDSComponent> textComponents = components.stream().filter(e -> e instanceof TextComponent).collect(Collectors.toList());

        // Texture
        for(PIDSComponent component : textureComponents) {
            graphicsHolder.push();
            component.render(graphicsHolder, null, world, facing);
            graphicsHolder.pop();
        }

        // Text
        graphicsHolder.translate(0, 0, -0.5);
        TextRenderingManager.bind(graphicsHolder);
        for(PIDSComponent component : textComponents) {
            graphicsHolder.push();
            component.render(graphicsHolder, null, world, facing);
            graphicsHolder.pop();
        }
    }

    @Override
    public List<PIDSComponent> getComponents(ArrivalsResponse arrivals, String[] customMessages, boolean[] rowHidden, int x, int y, int screenWidth, int screenHeight, int rows, boolean hidePlatform) {
        List<PIDSComponent> components = new ArrayList<>();
        components.add(new ClockComponent(screenWidth, 2, screenHeight, 10, getFont(), TextAlignment.RIGHT, TextOverflowMode.STRETCH, ARGB_WHITE, 1, new KVPair()));
        components.add(new WeatherIconComponent(ICON_WEATHER_SUNNY, ICON_WEATHER_RAINY, ICON_WEATHER_THUNDER, 0, 0, 11, 11));

        /* Arrivals */
        int arrivalIndex = 0;
        double rowY = y;
        for(int i = 0; i < rows; i++) {
            if(customMessages[i] != null && !customMessages[i].isEmpty()) {
                components.add(new CustomTextComponent(x, rowY, 78 * ARRIVAL_TEXT_SCALE, 10, getFont(), TextAlignment.LEFT, TextOverflowMode.STRETCH, getTextColor(), ARRIVAL_TEXT_SCALE, new KVPair().with("text", customMessages[i])));
            } else {
                if(!rowHidden[i] && arrivalIndex < arrivals.getArrivals().size()) {
                    float destinationMaxWidth = !hidePlatform ? (44 * ARRIVAL_TEXT_SCALE) : (54 * ARRIVAL_TEXT_SCALE);
                    components.add(new DestinationComponent(arrivals, TextOverflowMode.STRETCH, TextAlignment.LEFT, arrivalIndex, getFont(), getTextColor(), x, rowY, destinationMaxWidth, 10, ARRIVAL_TEXT_SCALE));

                    if(!hidePlatform) {
                        components.add(new PlatformComponent(arrivals, arrivalIndex, getFont(), RenderHelper.ARGB_WHITE, 64 * ARRIVAL_TEXT_SCALE, rowY, 9, 9));
                        components.add(new PlatformCircleComponent(arrivals, arrivalIndex, TEXTURE_PLATFORM_CIRCLE, 64 * ARRIVAL_TEXT_SCALE, rowY, 11, 11));
                    }

                    components.add(new ETAComponent(arrivals, TextOverflowMode.STRETCH, TextAlignment.RIGHT, arrivalIndex, getFont(), getTextColor(), screenWidth, rowY, 22 * ARRIVAL_TEXT_SCALE, 20, ARRIVAL_TEXT_SCALE));
                    arrivalIndex++;
                }
            }

            rowY += (screenHeight / 5.25) * ARRIVAL_TEXT_SCALE;
        }

        return components;
    }

    @Override
    public String getFont() {
        return "mtr:mtr";
    }

    @Override
    public @Nonnull Identifier getBackground() {
        return TEXTURE_BACKGROUND;
    }

    @Override
    public int getTextColor() {
        return ARGB_BLACK;
    }

    @Override
    public boolean isRowHidden(int row) {
        return false;
    }
}
