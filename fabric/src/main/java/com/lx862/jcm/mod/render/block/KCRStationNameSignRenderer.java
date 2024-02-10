package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.block.entity.KCRStationNameSignBlockEntity;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;

public class KCRStationNameSignRenderer extends JCMBlockEntityRenderer<KCRStationNameSignBlockEntity> {
    public KCRStationNameSignRenderer(Argument dispatcher) {
        super(dispatcher);
    }

    @Override
    public void renderCurated(KCRStationNameSignBlockEntity blockEntity, GraphicsHolder graphicsHolder, World world, BlockState state, BlockPos pos, float tickDelta, int light, int i1) {
        graphicsHolder.push();
        scaleCentered(graphicsHolder, 0.018F, 0.018F, 0.018F);
        rotateToBlockDirection(graphicsHolder, blockEntity);
        // TODO: Render with IDrawing
        graphicsHolder.pop();
    }
}
