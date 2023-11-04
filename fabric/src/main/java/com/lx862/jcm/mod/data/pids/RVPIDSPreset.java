package com.lx862.jcm.mod.data.pids;

import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.data.pids.base.PIDSPresetBase;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.WorldHelper;

public class RVPIDSPreset extends PIDSPresetBase implements RenderHelper {
    private static final Identifier TEXTURE_BACKGROUND = new Identifier("jsblock:textures/block/pids/rv_default.png");
    public RVPIDSPreset() {
        super("rv_pids");
    }

    public void render(PIDSBlockEntity be, GraphicsHolder graphicsHolder, World world, float tickDelta, int x, int y, int width, int height, int color, int light) {
        BlockState bs = world.getBlockState(be.getPos2());
        graphicsHolder.createVertexConsumer(RenderLayer.getEntityShadow(TEXTURE_BACKGROUND));

        Direction direction = BlockUtil.getProperty(bs, BlockProperties.FACING);
        RenderHelper.drawTexture(graphicsHolder, x, y, 0, width, height, direction, color, MAX_RENDER_LIGHT);

        graphicsHolder.createVertexConsumer(RenderLayer.getTextSeeThrough(TEXTURE_BACKGROUND));
        drawClock(graphicsHolder, world, width - 32, 2, 0xFFFFFFFF);
    }

    private void drawClock(GraphicsHolder graphicsHolder, World world, int x, int y, int textColor) {
        long timeNow = WorldHelper.getTimeOfDay(world) + 6000;
        long hours = timeNow / 1000;
        long minutes = Math.round((timeNow - (hours * 1000)) / 16.8);
        String timeString = String.format("%02d:%02d", hours % 24, minutes % 60);
        graphicsHolder.drawText(timeString, x, y, textColor, false, MAX_RENDER_LIGHT);
    }
}
