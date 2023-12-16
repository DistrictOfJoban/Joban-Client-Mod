package com.lx862.jcm.mod.data.pids.preset;

import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.block.entity.RVPIDSBlockEntity;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.trm.TextInfo;
import com.lx862.jcm.mod.trm.TextRenderingManager;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.WorldHelper;

public class RVPIDSPreset extends PIDSPresetBase {
    private static final int PIDS_MARGIN = 8;
    private static final float ARRIVAL_TEXT_SCALE = 1.35F;
    private static final Identifier TEXTURE_PLATFORM_CIRCLE = new Identifier("jsblock:textures/block/pids/plat_circle.png");
    private static final Identifier TEXTURE_BACKGROUND = new Identifier("jsblock:textures/block/pids/rv_default.png");
    private static final Identifier ICON_WEATHER_SUNNY = new Identifier("jsblock:textures/block/pids/weather_sunny.png");
    private static final Identifier ICON_WEATHER_RAINY = new Identifier("jsblock:textures/block/pids/weather_rainy.png");
    private static final Identifier ICON_WEATHER_THUNDER = new Identifier("jsblock:textures/block/pids/weather_thunder.png");
    public RVPIDSPreset() {
        super("rv_pids", "Hong Kong Railway Vision PIDS", true);
    }

    public void render(PIDSBlockEntity be, GraphicsHolder graphicsHolder, World world, Direction facing, float tickDelta, int x, int y, int width, int height, int color, int light) {
        int contentWidth = width - (PIDS_MARGIN * 2);
        int rowAmount = be.getRowAmount();
        boolean hidePlatform = (be instanceof RVPIDSBlockEntity) ? ((RVPIDSBlockEntity)be).getHidePlatformNumber() : false;

        // Draw Textures
        drawBackground(graphicsHolder, width, height, facing);
        //TextureTextRenderer.stressTest(5);
        drawTestBackground(graphicsHolder, width, height, facing);

        graphicsHolder.translate(0, 0, -0.5);
        titleDrawWeatherIcon(graphicsHolder, world, facing, PIDS_MARGIN);
        if(!hidePlatform) {
            arrivalsDrawPlatformIcon(graphicsHolder, facing, PIDS_MARGIN, 15, contentWidth, height, rowAmount);
        }

        // Text
        graphicsHolder.translate(0, 0, -0.5);
        TextRenderingManager.bind(graphicsHolder);
        titleDrawClock(graphicsHolder, facing, world, contentWidth - 19, 2, ARGB_WHITE);
        arrivalsDrawTable(graphicsHolder, facing, PIDS_MARGIN, 15, contentWidth, height, rowAmount, ARGB_BLACK, hidePlatform);
    }

    protected void drawBackground(GraphicsHolder graphicsHolder, int width, int height, Direction facing) {
        graphicsHolder.createVertexConsumer(RenderLayer.getText(TEXTURE_BACKGROUND));
        RenderHelper.drawTexture(graphicsHolder, TEXTURE_BACKGROUND, 0, 0, 0, width, height, facing, ARGB_WHITE, MAX_RENDER_LIGHT);
    }

    private void drawTestBackground(GraphicsHolder graphicsHolder, int width, int height, Direction facing) {
        TextRenderingManager.bind(graphicsHolder);
        RenderHelper.drawTexture(graphicsHolder,0, height, 0, width, width, facing, ARGB_WHITE, MAX_RENDER_LIGHT);
    }

    private void drawArrivalEntryCallback(GraphicsHolder graphicsHolder, int x, int y, int width, int height, int rowAmount, DrawRowCallback drawRowCallback) {
        graphicsHolder.push();
        graphicsHolder.translate(x, y, 0);
        graphicsHolder.scale(ARRIVAL_TEXT_SCALE, ARRIVAL_TEXT_SCALE, ARRIVAL_TEXT_SCALE);

        for(int i = 0; i < rowAmount; i++) {
            drawRowCallback.accept(graphicsHolder, width);
            graphicsHolder.translate(0, ((height - 11) / 7.25) * ARRIVAL_TEXT_SCALE, 0);
        }
        graphicsHolder.pop();
    }

    private void arrivalsDrawTable(GraphicsHolder rawGraphicsHolder, Direction facing, int x, int y, int rawWidth, int height, int rowAmount, int textColor, boolean hidePlatform) {
        drawArrivalEntryCallback(rawGraphicsHolder, x, y, rawWidth, height, rowAmount, (graphicsHolder, width) -> {
            drawArrivalEntry(graphicsHolder, facing, (int)(width / ARRIVAL_TEXT_SCALE), "610", "§eTuen Mun §dFerry Pier" /*"§e屯門 Tuen Mun"*/, 2, 50000, textColor, false, hidePlatform);
        });
    }
    private void arrivalsDrawPlatformIcon(GraphicsHolder rawGraphicsHolder, Direction facing, int x, int y, int rawWidth, int height, int rowAmount) {
        rawGraphicsHolder.createVertexConsumer(RenderLayer.getText(TEXTURE_PLATFORM_CIRCLE));

        drawArrivalEntryCallback(rawGraphicsHolder, x, y, rawWidth, height, rowAmount, (graphicsHolder, width) -> {
            RenderHelper.drawTexture(graphicsHolder, 44 * ARRIVAL_TEXT_SCALE, 0, 0, 9, 9, facing, ARGB_BLACK, MAX_RENDER_LIGHT);
        });
    }

    private void drawArrivalEntry(GraphicsHolder graphicsHolder, Direction facing, int width, String lrtNumber, String destination, int car, long arrivalTime, int textColor, boolean showCar, boolean hidePlatform) {
        String leftDestination = lrtNumber + " " +  destination;
        TextInfo widthTextInfo = new TextInfo(leftDestination).withColor(textColor).withFont(getFont());

        int destinationWidth = TextRenderingManager.getTextWidth(widthTextInfo);
        float destinationMaxWidth = !hidePlatform ? (44 * ARRIVAL_TEXT_SCALE) : (54 * ARRIVAL_TEXT_SCALE);

        graphicsHolder.push();
        if(destinationWidth > destinationMaxWidth) {
            RenderHelper.scaleToFit(graphicsHolder, destinationWidth, destinationMaxWidth - 2, false);
            drawPIDSText(graphicsHolder, facing, leftDestination, 0, 0, textColor);
            // TODO: Make marquee an option in custom PIDS Preset
            //drawPIDSScrollingText(graphicsHolder, facing, leftDestination, 0, 0, textColor, (int)destinationMaxWidth - 2);
        } else {
            drawPIDSText(graphicsHolder, facing, leftDestination, 0, 0, textColor);
        }
        graphicsHolder.pop();

        // Platform Text
        if(!hidePlatform) {
            graphicsHolder.push();
            int platTextWidth = GraphicsHolder.getTextWidth("2");
            graphicsHolder.translate((44 * ARRIVAL_TEXT_SCALE) + 5, 1.75, 0);
            graphicsHolder.scale(0.75F, 0.75F, 0.75F);
            RenderHelper.scaleToFit(graphicsHolder, platTextWidth, 8, true, 9);
            drawPIDSCenteredText(graphicsHolder,facing, "2", 0, 0, ARGB_WHITE);
            graphicsHolder.pop();
        }

        // Right
        drawArrivalEntryETA(graphicsHolder, facing, width, car, showCar, arrivalTime, textColor);
    }

    private void drawArrivalEntryETA(GraphicsHolder graphicsHolder, Direction facing, int width, int car, boolean showCar, long arrivalTime, int textColor) {
        String etaString = "14 min";

        int etaCarWidth = GraphicsHolder.getTextWidth(etaString);
        drawPIDSText(graphicsHolder, facing, etaString, width - etaCarWidth, 0, textColor);
    }

    private void titleDrawWeatherIcon(GraphicsHolder graphicsHolder, World world, Direction facing, float x) {
        if(world.isRaining()) {
            graphicsHolder.createVertexConsumer(RenderLayer.getText(ICON_WEATHER_RAINY));
        } else if(world.isThundering()) {
            graphicsHolder.createVertexConsumer(RenderLayer.getText(ICON_WEATHER_THUNDER));
        } else {
            graphicsHolder.createVertexConsumer(RenderLayer.getText(ICON_WEATHER_SUNNY));
        }
        RenderHelper.drawTexture(graphicsHolder, x, 0, 0, 11, 11, facing, ARGB_WHITE, MAX_RENDER_LIGHT);
    }

    private void titleDrawClock(GraphicsHolder graphicsHolder, Direction facing, World world, int x, int y, int textColor) {
        long timeNow = WorldHelper.getTimeOfDay(world) + 6000;
        long hours = timeNow / 1000;
        long minutes = Math.round((timeNow - (hours * 1000)) / 16.8);
        String timeString = String.format("%02d:%02d", hours % 24, minutes % 60);
        drawPIDSText(graphicsHolder, facing, timeString,  x, y, textColor);
    }

    @Override
    public String getFont() {
        return "mtr:mtr";
    }

    @Override
    public Identifier getPreviewImage() {
        return TEXTURE_BACKGROUND;
    }
}
