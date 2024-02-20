package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.block.entity.FareSaverBlockEntity;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.render.text.TextAlignment;
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
    public void renderCurated(FareSaverBlockEntity blockEntity, GraphicsHolder graphicsHolder, World world, BlockState state, BlockPos pos, float tickDelta, int light, int i1) {
        Direction facing = BlockUtil.getProperty(state, BlockProperties.FACING);
        int part = BlockUtil.getProperty(state, BlockProperties.VERTICAL_PART_3);
        if(part != 2) return;

        MutableText discountText = TextUtil.translatable(TextCategory.BLOCK, "faresaver.currency", blockEntity.getDiscount());

        graphicsHolder.push();
        graphicsHolder.translate(0.5, 0.5, 0.5);
        graphicsHolder.scale(0.011F, 0.011F, 0.011F);
        rotateToBlockDirection(graphicsHolder, blockEntity);
        graphicsHolder.rotateZDegrees(180);
        graphicsHolder.rotateZDegrees(TEXT_TILT_ANGLE);

        graphicsHolder.translate(5.8, 14, -9.15);

        TextRenderingManager.bind(graphicsHolder);

        graphicsHolder.push();
        TextInfo textInfo = new TextInfo(discountText)
                .withFont("mtr:mtr")
                .withColor(ARGB_WHITE)
                .withTextAlignment(TextAlignment.CENTER);
        RenderHelper.scaleToFit(graphicsHolder, TextRenderingManager.getTextWidth(textInfo), 12, true, 12);
        TextRenderingManager.draw(graphicsHolder, textInfo, facing, 0, 0);
        graphicsHolder.pop();

        graphicsHolder.pop();
    }
}
