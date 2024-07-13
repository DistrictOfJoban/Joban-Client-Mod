package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.Init;
import org.mtr.mod.block.BlockSignalBase;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.render.MoreRenderLayers;

public class StaticSignalLightRenderer<T extends BlockEntityExtension> extends JCMBlockEntityRenderer<T> {
        private final boolean drawOnTop;
        private final int color;

        public StaticSignalLightRenderer(Argument dispatcher, int color, boolean drawOnTop) {
            super(dispatcher);
            this.color = color;
            this.drawOnTop = drawOnTop;
        }

        @Override
        public final void renderCurated(T entity, GraphicsHolder graphicsHolder, World world, BlockState state, BlockPos pos, float tickDelta, int light, int overlay) {
            final Direction facing = IBlock.getStatePropertySafe(state, BlockProperties.FACING);
            final float angle = facing.asRotation() + (IBlock.getStatePropertySafe(state, BlockSignalBase.IS_22_5).booleanValue ? 22.5F : 0) + (IBlock.getStatePropertySafe(state, BlockSignalBase.IS_45).booleanValue ? 45 : 0);

            graphicsHolder.push();
            graphicsHolder.translate(0.5, 0, 0.5);

            graphicsHolder.push();
            graphicsHolder.rotateYDegrees(-angle);
            graphicsHolder.createVertexConsumer(MoreRenderLayers.getLight(new Identifier(Init.MOD_ID, "textures/block/white.png"), false));
            drawSignalLight(graphicsHolder, entity, tickDelta, facing);
            graphicsHolder.pop();

            graphicsHolder.pop();
        }

        private void drawSignalLight(GraphicsHolder graphicsHolder, T entity, float tickDelta, Direction facing) {
            final float y = drawOnTop ? 0.4375F : 0.0625F;
            IDrawing.drawTexture(graphicsHolder, -0.125F, y, -0.19375F, 0.125F, y + 0.25F, -0.19375F, facing.getOpposite(), color, RenderHelper.MAX_RENDER_LIGHT);
        }
}
