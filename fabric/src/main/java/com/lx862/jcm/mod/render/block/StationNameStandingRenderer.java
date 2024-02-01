package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.block.entity.StationNameStandingBlockEntity;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mod.client.DynamicTextureCache;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.render.RenderStationNameBase;
import org.mtr.mod.render.RenderTrains;
import org.mtr.mod.render.StoredMatrixTransformations;

public class StationNameStandingRenderer extends RenderStationNameBase<StationNameStandingBlockEntity> {

    private static final float WIDTH = 0.6875F;
    private static final float HEIGHT = 1;
    private static final float OFFSET_Y = 0.125F;

    public StationNameStandingRenderer(Argument dispatcher) {
        super(dispatcher);
    }

    @Override
    protected void drawStationName(World world, BlockPos pos, BlockState state, Direction facing, StoredMatrixTransformations storedMatrixTransformations, String stationName, int stationColor, int color, int light) {
        if (BlockUtil.getProperty(state, BlockProperties.VERTICAL_PART_3) == 1) {
            RenderTrains.scheduleRender(DynamicTextureCache.instance.getTallStationName(color, stationName, stationColor, WIDTH / HEIGHT).identifier, false, RenderTrains.QueuedRenderLayer.EXTERIOR, (graphicsHolder, offset) -> {
                storedMatrixTransformations.transform(graphicsHolder, offset);
                IDrawing.drawTexture(graphicsHolder, -WIDTH / 2, -HEIGHT / 2 - OFFSET_Y, WIDTH, HEIGHT, 0, 0, 1, 1, facing, ARGB_WHITE, light);
                graphicsHolder.pop();
            });
        }
    }
}
