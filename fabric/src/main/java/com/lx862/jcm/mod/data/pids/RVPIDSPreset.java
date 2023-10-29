package com.lx862.jcm.mod.data.pids;

import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.data.pids.base.BuiltinPIDSPreset;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.GraphicsHolder;

public class RVPIDSPreset extends BuiltinPIDSPreset implements RenderHelper {
    private static final Identifier TEXTURE_BACKGROUND = new Identifier("jsblock:textures/block/pids/rv_default.png");
    public RVPIDSPreset() {
        super("rv_pids");
    }

    public void render(PIDSBlockEntity be, GraphicsHolder graphicsHolder, World world, float tickDelta, int x, int y, int width, int height, int color, int light) {
        BlockState bs = world.getBlockState(be.getPos2());
        graphicsHolder.createVertexConsumer(RenderLayer.getEntityCutout(TEXTURE_BACKGROUND));

        if(!bs.isAir()) {
            Direction direction = BlockUtil.getProperty(bs, BlockProperties.FACING);
            RenderHelper.drawTexture(graphicsHolder, x, y, 0, width, height, direction, color, MAX_RENDER_LIGHT);
        }
    }
}
