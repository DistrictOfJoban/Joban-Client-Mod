package com.lx862.jcm.mod.data.pids;

import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.data.pids.base.PIDSPresetBase;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.util.BlockUtil;
import net.minecraft.client.MinecraftClient;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.WorldHelper;

public class RVPIDSPreset extends PIDSPresetBase implements RenderHelper {
    private static final int PIDS_MARGIN = 8;
    private static final float ARRIVAL_TEXT_SCALE = 1.35F;
    private static final Identifier TEXTURE_PLATFORM_CIRCLE = new Identifier("jsblock:textures/block/pids/plat_circle.png");
    private static final Identifier TEXTURE_BACKGROUND = new Identifier("jsblock:textures/block/pids/rv_default.png");
    private static final Identifier ICON_WEATHER_SUNNY = new Identifier("jsblock:textures/block/pids/weather_sunny.png");
    private static final Identifier ICON_WEATHER_RAINY = new Identifier("jsblock:textures/block/pids/weather_rainy.png");
    private static final Identifier ICON_WEATHER_THUNDER = new Identifier("jsblock:textures/block/pids/weather_thunder.png");
    public RVPIDSPreset() {
        super("rv_pids");
    }

    public void render(PIDSBlockEntity be, GraphicsHolder graphicsHolder, World world, float tickDelta, int x, int y, int width, int height, int color, int light) {
        BlockState bs = world.getBlockState(be.getPos2());
        Direction facing = BlockUtil.getProperty(bs, BlockProperties.FACING);
        int contentWidth = width - (PIDS_MARGIN * 2);
        int rowAmount = be.getRowAmount();

        // Draw Textures
        graphicsHolder.translate(x, y, 0);
        drawBackground(graphicsHolder, width, height, facing);
        titleDrawWeatherIcon(graphicsHolder, world, facing, PIDS_MARGIN);
        arrivalsDrawPlatformIcon(graphicsHolder, facing, PIDS_MARGIN, 15, contentWidth, rowAmount);

        // Text
        graphicsHolder.createVertexConsumer(RenderLayer.getTextSeeThrough(TEXTURE_BACKGROUND));
        titleDrawClock(graphicsHolder, world, contentWidth - 19, 2, ARGB_WHITE);
        arrivalsDrawText(graphicsHolder, PIDS_MARGIN, 15, contentWidth, rowAmount, ARGB_BLACK);
    }

    protected void drawBackground(GraphicsHolder graphicsHolder, int width, int height, Direction facing) {
        graphicsHolder.createVertexConsumer(RenderLayer.getEntityShadow(TEXTURE_BACKGROUND));
        RenderHelper.drawTexture(graphicsHolder, 0, 0, 0, width, height, facing, ARGB_WHITE, MAX_RENDER_LIGHT);
    }

    private void drawArrivalEntryCallback(GraphicsHolder graphicsHolder, int x, int y, int width, int rowAmount, DrawRowCallback drawRowCallback) {
        graphicsHolder.push();
        graphicsHolder.translate(x, y, 0);
        graphicsHolder.scale(ARRIVAL_TEXT_SCALE, ARRIVAL_TEXT_SCALE, ARRIVAL_TEXT_SCALE);

        for(int i = 0; i < rowAmount; i++) {
            drawRowCallback.accept(graphicsHolder, width);
            graphicsHolder.translate(0, 10.75 * ARRIVAL_TEXT_SCALE, 0);
        }
        graphicsHolder.pop();
    }

    private void arrivalsDrawText(GraphicsHolder rawGraphicsHolder, int x, int y, int rawWidth, int rowAmount, int textColor) {
        TextRenderer textRenderer = new TextRenderer(MinecraftClient.getInstance().textRenderer);

        drawArrivalEntryCallback(rawGraphicsHolder, x, y, rawWidth, rowAmount, (graphicsHolder, width) -> {
            drawArrivalEntry(graphicsHolder, textRenderer, (int)(width / ARRIVAL_TEXT_SCALE), "610", "Tuen Mun Ferry Pier", 2, 50000, false, textColor);
        });
    }
    private void arrivalsDrawPlatformIcon(GraphicsHolder rawGraphicsHolder, Direction facing, int x, int y, int rawWidth, int rowAmount) {
        rawGraphicsHolder.createVertexConsumer(RenderLayer.getEntityShadow(TEXTURE_PLATFORM_CIRCLE));

        drawArrivalEntryCallback(rawGraphicsHolder, x, y, rawWidth, rowAmount, (graphicsHolder, width) -> {
            RenderHelper.drawTexture(graphicsHolder, 44 * ARRIVAL_TEXT_SCALE, 0, 0, 9, 9, facing, ARGB_BLACK, MAX_RENDER_LIGHT);
        });
    }

    private void drawArrivalEntry(GraphicsHolder graphicsHolder, TextRenderer textRenderer, int width, String lrtNumber, String destination, int car, long arrivalTime, boolean showCar, int textColor) {
        String leftDestination = lrtNumber + " " +  destination;

        int destinationWidth = GraphicsHolder.getTextWidth(leftDestination);
        float destinationMaxWidth = true ? (44 * ARRIVAL_TEXT_SCALE) : (54 * ARRIVAL_TEXT_SCALE);

        graphicsHolder.push();
        if(destinationWidth > destinationMaxWidth) {
            scaleToFitBoundary(graphicsHolder, destinationWidth, destinationMaxWidth - 2, false);
            drawPIDSText(graphicsHolder, leftDestination, 0, 0, 0);
            // TODO: Make marquee an option in custom PIDS Preset
            //drawMarqueeText(graphicsHolder, TextUtil.withFont(TextUtil.literal(leftDestination), getFont()), (int)destinationMaxWidth - 2, 0, 0, 0);
        } else {
            drawPIDSText(graphicsHolder, leftDestination, 0, 0, 0);
        }
        graphicsHolder.pop();

        // Platform Text
        if(true) {
            graphicsHolder.push();
            int platTextWidth = GraphicsHolder.getTextWidth("2");
            graphicsHolder.translate((44 * ARRIVAL_TEXT_SCALE) + 5, 1.75, 0);
            graphicsHolder.scale(0.75F, 0.75F, 0.75F);
            scaleToFitBoundary(graphicsHolder, platTextWidth, 8, true, textRenderer.getFontHeightMapped());
            drawPIDSCenteredText(graphicsHolder,"2", 0, 0, ARGB_WHITE);
            graphicsHolder.pop();
        }

        // Right
        drawArrivalEntryETA(graphicsHolder, width, car, showCar, arrivalTime, textColor);
    }

    private void drawArrivalEntryETA(GraphicsHolder graphicsHolder, int width, int car, boolean showCar, long arrivalTime, int textColor) {
        String etaString = "14 min";

        int etaCarWidth = GraphicsHolder.getTextWidth(etaString);
        drawPIDSText(graphicsHolder, etaString, width - etaCarWidth, 0, textColor);
    }

    private void titleDrawWeatherIcon(GraphicsHolder graphicsHolder, World world, Direction facing, float x) {
        if(world.isRaining()) {
            graphicsHolder.createVertexConsumer(RenderLayer.getEntityShadow(ICON_WEATHER_RAINY));
        } else if(world.isThundering()) {
            graphicsHolder.createVertexConsumer(RenderLayer.getEntityShadow(ICON_WEATHER_THUNDER));
        } else {
            graphicsHolder.createVertexConsumer(RenderLayer.getEntityShadow(ICON_WEATHER_SUNNY));
        }
        RenderHelper.drawTexture(graphicsHolder, x, 0, 0, 11, 11, Direction.convert(facing.data), ARGB_WHITE, MAX_RENDER_LIGHT);
    }

    private void titleDrawClock(GraphicsHolder graphicsHolder, World world, int x, int y, int textColor) {
        long timeNow = WorldHelper.getTimeOfDay(world) + 6000;
        long hours = timeNow / 1000;
        long minutes = Math.round((timeNow - (hours * 1000)) / 16.8);
        String timeString = String.format("%02d:%02d", hours % 24, minutes % 60);
        drawPIDSText(graphicsHolder, timeString, x, y, textColor);
    }

    @Override
    public String getFont() {
        return "jsblock:kcr_sign";
    }
}
