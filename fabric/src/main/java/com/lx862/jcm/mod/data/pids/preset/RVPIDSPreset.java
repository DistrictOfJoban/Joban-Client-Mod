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
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
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
    private static final String TEXTURE_PLATFORM_CIRCLE = Constants.MOD_ID + ":textures/block/pids/plat_circle.png";
    private static final String TEXTURE_BACKGROUND = Constants.MOD_ID + ":textures/block/pids/rv_default.png";
    private static final String ICON_WEATHER_SUNNY = Constants.MOD_ID + ":textures/block/pids/weather_sunny.png";
    private static final String ICON_WEATHER_RAINY = Constants.MOD_ID + ":textures/block/pids/weather_rainy.png";
    private static final String ICON_WEATHER_THUNDER = Constants.MOD_ID + ":textures/block/pids/weather_thunder.png";
    public RVPIDSPreset() {
        super("rv_pids", "Hong Kong Railway Vision PIDS", true);
    }

    protected RVPIDSPreset(String id, String name) {
        super(id, name, true);
    }

    @Override
    public void render(PIDSBlockEntity be, GraphicsHolder graphicsHolder, World world, Direction facing, ObjectArrayList<ArrivalResponse> arrivals, boolean[] rowHidden, float tickDelta, int x, int y, int width, int height) {
        // Draw Background
        graphicsHolder.createVertexConsumer(RenderLayer.getText(getBackground()));

        // Debug View Texture
        if(ConfigEntry.DEBUG_MODE.getBool() && ConfigEntry.NEW_TEXT_RENDERER.getBool()) {
            //TextureTextRenderer.stressTest(5);
            drawAtlasBackground(graphicsHolder, width, height, facing);
        }

        graphicsHolder.translate(0, 0, -0.5);

        List<PIDSComponent> components = getComponents(arrivals, be.getCustomMessages(), rowHidden, 0, 0, width, height, be.getRowAmount(), be.platformNumberHidden());
        List<PIDSComponent> textureComponents = components.stream().filter(e -> e instanceof TextureComponent).collect(Collectors.toList());
        List<PIDSComponent> textComponents = components.stream().filter(e -> e instanceof TextComponent).collect(Collectors.toList());

        // Texture
        graphicsHolder.push();
        for(PIDSComponent component : textureComponents) {
            graphicsHolder.push();
            component.render(graphicsHolder, null, facing, new PIDSContext(world, arrivals, tickDelta));
            graphicsHolder.pop();
            graphicsHolder.translate(0, 0, -0.003);
        }
        graphicsHolder.pop();

        // Text
        graphicsHolder.translate(0, 0, -0.5);
        TextRenderingManager.bind(graphicsHolder);
        for(PIDSComponent component : textComponents) {
            graphicsHolder.push();
            component.render(graphicsHolder, null, facing, new PIDSContext(world, arrivals, tickDelta));
            graphicsHolder.pop();
        }
    }

    @Override
    public List<PIDSComponent> getComponents(ObjectArrayList<ArrivalResponse> arrivals, String[] customMessages, boolean[] rowHidden, int x, int y, int screenWidth, int screenHeight, int rows, boolean hidePlatform) {
        int startX = x + PIDS_MARGIN;
        int startY = y + (screenHeight / 9);
        int contentWidth = screenWidth - PIDS_MARGIN;
        int contentHeight = screenHeight - HEADER_HEIGHT - 3;

        List<PIDSComponent> components = new ArrayList<>();
        components.add(new CustomTextureComponent(0, 0, screenWidth, screenHeight, new KVPair().with("textureId", getBackground().getNamespace() + ":" + getBackground().getPath())));
        components.add(new ClockComponent(contentWidth, 2, contentWidth, 10, TextComponent.of(TextAlignment.RIGHT, TextOverflowMode.STRETCH, getFont(), ARGB_WHITE, 1)));
        components.add(new WeatherIconComponent(startX, 0, 11, 11,
                new KVPair().with("weatherIconSunny", ICON_WEATHER_SUNNY)
                .with("weatherIconRainy", ICON_WEATHER_RAINY)
                .with("weatherIconThunder", ICON_WEATHER_THUNDER)));

        /* Arrivals */
        int arrivalIndex = 0;
        double rowY = startY + 6;
        for(int i = 0; i < rows; i++) {
            if(customMessages[i] != null && !customMessages[i].isEmpty()) {
                components.add(new CustomTextComponent(startX, rowY, 78 * ARRIVAL_TEXT_SCALE, 10, TextComponent.of(TextAlignment.LEFT, TextOverflowMode.STRETCH, getFont(), getTextColor(), ARRIVAL_TEXT_SCALE).with("text", customMessages[i])));
            } else {
                if(!rowHidden[i] && arrivalIndex < arrivals.size()) {
                    float destinationMaxWidth = !hidePlatform ? (44 * ARRIVAL_TEXT_SCALE) : (54 * ARRIVAL_TEXT_SCALE);
                    components.add(new DestinationComponent(startX, rowY, destinationMaxWidth, 10, TextComponent.of(TextAlignment.LEFT, TextOverflowMode.STRETCH, getFont(), getTextColor(), ARRIVAL_TEXT_SCALE).with("arrivalIndex", arrivalIndex)));

                    if(!hidePlatform) {
                        components.add(new PlatformComponent(startX + (64 * ARRIVAL_TEXT_SCALE), rowY, 9, 9, getFont(), RenderHelper.ARGB_WHITE, 1, new KVPair().with("arrivalIndex", arrivalIndex)));
                        components.add(new PlatformCircleComponent(startX + (64 * ARRIVAL_TEXT_SCALE), rowY, 11, 11, new KVPair().with("textureId", TEXTURE_PLATFORM_CIRCLE).with("arrivalIndex", arrivalIndex)));
                    }

                    components.add(new ETAComponent(contentWidth, rowY, 22 * ARRIVAL_TEXT_SCALE, 20, TextComponent.of(TextAlignment.RIGHT, TextOverflowMode.STRETCH, getFont(), getTextColor(), ARRIVAL_TEXT_SCALE).with("arrivalIndex", arrivalIndex)));
                    arrivalIndex++;
                }
            }

            rowY += (contentHeight / 5.25) * ARRIVAL_TEXT_SCALE;
        }

        return components;
    }

    @Override
    public String getFont() {
        return "mtr:mtr";
    }

    @Override
    public @Nonnull Identifier getBackground() {
        return new Identifier(TEXTURE_BACKGROUND);
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
