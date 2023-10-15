package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.block.entity.FareSaverBlockEntity;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import net.minecraft.client.MinecraftClient;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.TextRenderer;
import org.mtr.mapping.mapper.GraphicsHolder;

public class FareSaverRenderer extends JCMBlockEntityRenderer<FareSaverBlockEntity> implements RenderHelper {
    public static final int TEXT_TILT_ANGLE = -10;
    public FareSaverRenderer(Argument argument) {
        super(argument);
    }

    @Override
    public void renderCurated(FareSaverBlockEntity blockEntity, float tickDelta, GraphicsHolder graphicsHolder, int light, int i1) {
        BlockState state = blockEntity.getWorld2().getBlockState(blockEntity.getPos2());
        int part = BlockUtil.getProperty(state, BlockProperties.VERTICAL_PART_3);
        if(part != 2) return;

        TextRenderer textRenderer = new TextRenderer(MinecraftClient.getInstance().textRenderer);
        MutableText discountText = TextUtil.translatable(TextCategory.BLOCK, "faresaver.currency", blockEntity.getDiscount());

        graphicsHolder.push();
        scaleCentered(graphicsHolder, 0.011F, 0.011F, 0.011F);
        rotateToBlockDirection(graphicsHolder, blockEntity);
        graphicsHolder.rotateZDegrees(180);
        graphicsHolder.rotateZDegrees(TEXT_TILT_ANGLE);
        graphicsHolder.translate(0, 13, -6);
        scaleToFitBoundary(graphicsHolder, GraphicsHolder.getTextWidth(discountText), 12, true, textRenderer.getFontHeightMapped());
        graphicsHolder.drawText(discountText, 0, 0, 0xFFFFFFFF, false, MAX_RENDER_LIGHT);
        graphicsHolder.pop();
    }
}
