package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.block.entity.FareSaverBlockEntity;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.render.text.TextInfo;
import com.lx862.jcm.mod.render.text.TextRenderingManager;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.GraphicsHolder;

public class FareSaverRenderer extends JCMBlockEntityRenderer<FareSaverBlockEntity> {
    public static final int TEXT_TILT_ANGLE = -10;
    public FareSaverRenderer(Argument dispatcher) {
        super(dispatcher);
    }

    @Override
    public void renderCurated(FareSaverBlockEntity blockEntity, float tickDelta, GraphicsHolder graphicsHolder, int light, int i1) {
        BlockState state = blockEntity.getWorld2().getBlockState(blockEntity.getPos2());
        Direction facing = BlockUtil.getProperty(state, BlockProperties.FACING);
        int part = BlockUtil.getProperty(state, BlockProperties.VERTICAL_PART_3);
        if(part != 2) return;

        MutableText discountText = TextUtil.translatable(TextCategory.BLOCK, "faresaver.currency", blockEntity.getDiscount());

        graphicsHolder.push();
        scaleCentered(graphicsHolder, 0.011F, 0.011F, 0.011F);
        rotateToBlockDirection(graphicsHolder, blockEntity);
        graphicsHolder.rotateZDegrees(180);
        graphicsHolder.rotateZDegrees(TEXT_TILT_ANGLE);
        graphicsHolder.translate(0, 13, -6);
        RenderHelper.scaleToFit(graphicsHolder, GraphicsHolder.getTextWidth(discountText), 12, true, 9);
        TextRenderingManager.bind(graphicsHolder);
        TextRenderingManager.draw(graphicsHolder, new TextInfo(discountText).withFont("mtr:mtr").withColor(ARGB_WHITE), facing, 0, 0);
        graphicsHolder.pop();
    }
}
