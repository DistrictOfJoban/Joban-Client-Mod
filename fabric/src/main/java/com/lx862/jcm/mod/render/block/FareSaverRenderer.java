package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.block.entity.FareSaverBlockEntity;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.trm.TextRenderingManager;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import net.minecraft.client.MinecraftClient;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.GraphicsHolder;

public class FareSaverRenderer extends JCMBlockEntityRenderer<FareSaverBlockEntity> implements RenderHelper {
    public static final int TEXT_TILT_ANGLE = -10;
    public FareSaverRenderer(Argument argument) {
        super(argument);
    }

    @Override
    public void renderCurated(FareSaverBlockEntity blockEntity, float tickDelta, GraphicsHolder graphicsHolder, int light, int i1) {
        BlockState state = blockEntity.getWorld2().getBlockState(blockEntity.getPos2());
        Direction facing = BlockUtil.getProperty(state, BlockProperties.FACING);
        int part = BlockUtil.getProperty(state, BlockProperties.VERTICAL_PART_3);
        if(part != 2) return;

        TextRenderer textRenderer = new TextRenderer(MinecraftClient.getInstance().textRenderer);
        MutableText discountText = TextUtil.translatable(TextCategory.BLOCK, "faresaver.currency", blockEntity.getDiscount()).formatted(TextFormatting.WHITE);

        graphicsHolder.push();
        scaleCentered(graphicsHolder, 0.011F, 0.011F, 0.011F);
        rotateToBlockDirection(graphicsHolder, blockEntity);
        graphicsHolder.rotateZDegrees(180);
        graphicsHolder.rotateZDegrees(TEXT_TILT_ANGLE);
        graphicsHolder.translate(0, 13, -6);
        scaleToFitBoundary(graphicsHolder, GraphicsHolder.getTextWidth(discountText), 12, true, textRenderer.getFontHeightMapped());
        TextRenderingManager.bind(graphicsHolder);
        TextRenderingManager.draw(graphicsHolder, discountText, facing, 0, 0);
        graphicsHolder.pop();
    }
}
