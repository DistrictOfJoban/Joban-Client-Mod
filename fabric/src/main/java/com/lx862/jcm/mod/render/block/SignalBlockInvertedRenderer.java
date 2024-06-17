package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.JCMClient;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.block.BlockSignalBase;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.render.RenderSignalLight2Aspect;
import org.mtr.mod.render.RenderTrains;
import org.mtr.mod.render.StoredMatrixTransformations;

public class SignalBlockInvertedRenderer<T extends BlockSignalBase.BlockEntityBase> extends RenderSignalLight2Aspect<T> {
    private final int proceedColor;
    private final boolean redOnTop;
    public SignalBlockInvertedRenderer(Argument dispatcher, int proceedColor, boolean redOnTop) {
        super(dispatcher, redOnTop, proceedColor);
        this.proceedColor = proceedColor;
        this.redOnTop = redOnTop;
    }

    @Override
    protected void render(StoredMatrixTransformations t, BlockSignalBase.BlockEntityBase signalBlockEntity, float v, int occupiedAspect, boolean isBackSide) {
        if (JCMClient.getConfig().disableRendering) return;

        float y = (occupiedAspect > 0) == redOnTop ? 0.4375F : 0.0625F;

        RenderTrains.scheduleRender(new Identifier("mtr", "textures/block/white.png"), false, RenderTrains.QueuedRenderLayer.LIGHT, (graphicsHolder, offset) -> {
            t.transform(graphicsHolder, offset);
            IDrawing.drawTexture(graphicsHolder, -0.125F, y, -0.19375F, 0.125F, y + 0.25F, -0.19375F, Direction.UP, occupiedAspect > 0 ? proceedColor : 0xFFFF0000, GraphicsHolder.getDefaultLight());
            graphicsHolder.pop();
        });
    }
}
