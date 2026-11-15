package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.config.JCMClientConfig;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.Init;
import org.mtr.mod.block.BlockSignalBase;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.RenderSignalLight2Aspect;
import org.mtr.mod.render.StoredMatrixTransformations;

public class SignalBlockInvertedRenderer<T extends BlockSignalBase.BlockEntityBase> extends RenderSignalLight2Aspect<T> {
    private static final Identifier BLANK_TEXTURE_ID = new Identifier(Init.MOD_ID, "textures/block/white.png");
    private final int proceedColor;
    private final boolean redOnTop;


    public SignalBlockInvertedRenderer(Argument dispatcher, int proceedColor, boolean redOnTop) {
        super(dispatcher, redOnTop, proceedColor);
        this.proceedColor = proceedColor;
        this.redOnTop = redOnTop;
    }

    @Override
    protected void render(StoredMatrixTransformations t, BlockSignalBase.BlockEntityBase signalBlockEntity, float v, int occupiedAspect, boolean isBackSide) {
        if (JCMClientConfig.INSTANCE.disableRendering.value()) return;

        float y = (occupiedAspect > 0) == redOnTop ? 0.4375F : 0.0625F;

        MainRenderer.scheduleRender(BLANK_TEXTURE_ID, false, QueuedRenderLayer.LIGHT, (graphicsHolder, offset) -> {
            t.transform(graphicsHolder, offset);
            IDrawing.drawTexture(graphicsHolder, -0.125F, y, -0.19375F, 0.125F, y + 0.25F, -0.19375F, Direction.UP, occupiedAspect > 0 ? proceedColor : 0xFFFF0000, GraphicsHolder.getDefaultLight());
            graphicsHolder.pop();
        });
    }
}
