package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.Init;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.render.MoreRenderLayers;
import org.mtr.mod.render.RenderTrains;

public class StaticSignalLightRenderer<T extends BlockEntityExtension> extends JCMBlockEntityRenderer<T> {
        private final boolean drawOnTop;
        private final int color;

        public StaticSignalLightRenderer(Argument dispatcher, int color, boolean drawOnTop) {
            super(dispatcher);
            this.color = color;
            this.drawOnTop = drawOnTop;
        }

        @Override
        public final void renderCurated(T entity, float tickDelta, GraphicsHolder graphicsHolder, int light, int overlay) {
            final World world = entity.getWorld2();
            if (world == null) {
                return;
            }

            final BlockPos pos = entity.getPos2();
            final BlockState state = world.getBlockState(pos);
            final Direction facing = BlockUtil.getProperty(state, BlockProperties.FACING);
            if (RenderTrains.shouldNotRender(pos, null)) {
                return;
            }

            graphicsHolder.push();
            graphicsHolder.translate(0.5, 0, 0.5);

            graphicsHolder.push();
            graphicsHolder.rotateYDegrees(-facing.asRotation());
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
