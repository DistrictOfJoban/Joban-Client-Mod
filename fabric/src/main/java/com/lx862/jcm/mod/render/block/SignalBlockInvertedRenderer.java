package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.config.ConfigEntry;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.render.RenderSignalBase;

public class SignalBlockInvertedRenderer<T extends BlockEntityExtension> extends RenderSignalBase<T> {
    private final int proceedColor;
    private final boolean redOnTop;
    public SignalBlockInvertedRenderer(Argument dispatcher, int proceedColor, boolean isSingleSided, boolean redOnTop) {
        super(dispatcher, isSingleSided, 2);
        this.proceedColor = proceedColor;
        this.redOnTop = redOnTop;
    }

    @Override
    protected void render(GraphicsHolder graphicsHolder, BlockEntityExtension blockEntityExtension, float v, Direction facing, int occupiedAspect, boolean isBackSide) {
        if (ConfigEntry.DISABLE_RENDERING.getBool()) return;

        float y = (occupiedAspect > 0) == redOnTop ? 0.4375F : 0.0625F;
        IDrawing.drawTexture(graphicsHolder, -0.125F, y, -0.19375F, 0.125F, y + 0.25F, -0.19375F, facing.getOpposite(), occupiedAspect > 0 ? proceedColor : 0xFFFF0000, GraphicsHolder.getDefaultLight());
    }
}
