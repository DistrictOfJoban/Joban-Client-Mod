package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.block.entity.VerticalPIDSBlockEntity;
import com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.render.StoredMatrixTransformations;
import org.mtr.mod.block.IBlock;

import static com.lx862.jcm.mod.block.base.Vertical2Block.HALF;

public class VerticalPIDSRenderer extends PIDSRenderer<VerticalPIDSBlockEntity> {
    public VerticalPIDSRenderer(Argument dispatcher) {
        super(dispatcher);
    }

    @Override
    public void renderPIDS(VerticalPIDSBlockEntity blockEntity, PIDSPresetBase pidsPreset, GraphicsHolder graphicsHolder, StoredMatrixTransformations storedMatrixTransformations, World world, BlockState state, BlockPos pos, Direction facing, org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongImmutableList targetPlatformIds, ObjectArrayList<ArrivalResponse> arrivals, float tickDelta, boolean[] rowHidden, int light) {
        if (IBlock.getStatePropertySafe(blockEntity.getCachedState2(), HALF) == IBlock.DoubleBlockHalf.LOWER) {
            storedMatrixTransformations.add(graphicsHolderNew -> {
                graphicsHolderNew.rotateYDegrees(90);
                graphicsHolderNew.translate(-0.4375, -1.4375, 0.434);
            });
            pidsPreset.render(blockEntity, graphicsHolder, storedMatrixTransformations, world, blockEntity.getPos2(), facing, targetPlatformIds, arrivals, rowHidden, tickDelta, 0, 0, 84, 169, light);
        }
    }
}
