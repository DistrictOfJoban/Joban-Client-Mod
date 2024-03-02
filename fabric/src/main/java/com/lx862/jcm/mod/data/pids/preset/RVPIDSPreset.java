package com.lx862.jcm.mod.data.pids.preset;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.config.ConfigEntry;
import com.lx862.jcm.mod.data.pids.preset.components.*;
import com.lx862.jcm.mod.data.pids.preset.components.base.TextComponent;
import com.lx862.jcm.mod.data.pids.preset.components.base.TextureComponent;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.data.pids.preset.components.base.DrawCall;
import com.lx862.jcm.mod.render.text.TextRenderingManager;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.core.operation.ArrivalsResponse;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.GraphicsHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RVPIDSPreset extends PIDSPresetBase {
    private static final int HEADER_HEIGHT = 11;
    private static final int PIDS_MARGIN = 7;
    private static final float ARRIVAL_TEXT_SCALE = 1.35F;
    private static final Identifier TEXTURE_PLATFORM_CIRCLE = Constants.id("textures/block/pids/plat_circle.png");
    private static final Identifier TEXTURE_BACKGROUND = Constants.id("textures/block/pids/rv_default.png");
    private static final Identifier ICON_WEATHER_SUNNY = Constants.id("textures/block/pids/weather_sunny.png");
    private static final Identifier ICON_WEATHER_RAINY = Constants.id("textures/block/pids/weather_rainy.png");
    private static final Identifier ICON_WEATHER_THUNDER = Constants.id("textures/block/pids/weather_thunder.png");
    public RVPIDSPreset() {
        super("rv_pids", "Hong Kong Railway Vision PIDS", true);
    }

    public void render(PIDSBlockEntity be, GraphicsHolder graphicsHolder, World world, Direction facing, ArrivalsResponse arrivals, boolean[] rowHidden, float tickDelta, int x, int y, int width, int height) {
        int startX = PIDS_MARGIN;
        int contentWidth = width - (PIDS_MARGIN * 2);
        int contentHeight = height - HEADER_HEIGHT;

        // Draw Background
        graphicsHolder.createVertexConsumer(RenderLayer.getText(TEXTURE_BACKGROUND));
        RenderHelper.drawTexture(graphicsHolder, TEXTURE_BACKGROUND, 0, 0, 0, width, height, facing, ARGB_WHITE, MAX_RENDER_LIGHT);

        // Debug View Texture
        if(ConfigEntry.DEBUG_MODE.getBool() && ConfigEntry.NEW_TEXT_RENDERER.getBool()) {
            //TextureTextRenderer.stressTest(5);
            drawAtlasBackground(graphicsHolder, width, height, facing);
        }

        graphicsHolder.translate(startX, 0, -0.5);

        List<DrawCall> components = new ArrayList<>();
        components.add(new ClockComponent(getFont(), ARGB_WHITE, contentWidth, 2, contentWidth, 10));
        components.add(new WeatherIconComponent(ICON_WEATHER_SUNNY, ICON_WEATHER_RAINY, ICON_WEATHER_THUNDER, 0, 0, 11, 11));

        drawArrivals(arrivals, rowHidden, 0, 15, contentWidth, contentHeight, be.getRowAmount(), ARGB_BLACK, be.platformNumberHidden(), components);

        List<DrawCall> textureComponents = components.stream().filter(e -> e instanceof TextureComponent).collect(Collectors.toList());
        List<DrawCall> textComponents = components.stream().filter(e -> e instanceof TextComponent).collect(Collectors.toList());

        // Texture
        for(DrawCall component : textureComponents) {
            graphicsHolder.push();
            component.render(graphicsHolder, world, facing);
            graphicsHolder.pop();
        }

        // Text
        graphicsHolder.translate(0, 0, -0.5);
        TextRenderingManager.bind(graphicsHolder);
        for(DrawCall component : textComponents) {
            graphicsHolder.push();
            component.render(graphicsHolder, world, facing);
            graphicsHolder.pop();
        }
    }

    private void drawArrivals(ArrivalsResponse arrivals, boolean[] rowHidden, int x, int y, int width, int height, int rows, int textColor, boolean hidePlatform, List<DrawCall> drawCalls) {
        int arrivalIndex = 0;
        double rowY = y;
        for(int i = 0; i < rows; i++) {
            if(arrivalIndex >= arrivals.getArrivals().size()) return;

            if(!rowHidden[i]) {
                ArrivalResponse arrival = arrivals.getArrivals().get(arrivalIndex);
                float destinationMaxWidth = !hidePlatform ? (44 * ARRIVAL_TEXT_SCALE) : (54 * ARRIVAL_TEXT_SCALE);
                drawCalls.add(new DestinationComponent(arrival, getFont(), textColor, x, rowY, destinationMaxWidth, 10, ARRIVAL_TEXT_SCALE));

                if(!hidePlatform) {
                    drawCalls.add(new PlatformComponent(arrival, getFont(), RenderHelper.ARGB_WHITE, 64 * ARRIVAL_TEXT_SCALE, rowY, 9, 9));
                    drawCalls.add(new PlatformCircleComponent(arrival, TEXTURE_PLATFORM_CIRCLE, 64 * ARRIVAL_TEXT_SCALE, rowY, 11, 11));
                }

                drawCalls.add(new ETAComponent(arrival, getFont(), textColor, width, rowY, 22 * ARRIVAL_TEXT_SCALE, 20, ARRIVAL_TEXT_SCALE));
                arrivalIndex++;
            }

            rowY += (height / 5.25) * ARRIVAL_TEXT_SCALE;
        }
    }

    @Override
    public String getFont() {
        return "mtr:mtr";
    }

    @Override
    public Identifier getBackground() {
        return TEXTURE_BACKGROUND;
    }

    @Override
    public int getPreviewTextColor() {
        return ARGB_BLACK;
    }

    @Override
    public boolean isRowHidden(int row) {
        return false;
    }
}
