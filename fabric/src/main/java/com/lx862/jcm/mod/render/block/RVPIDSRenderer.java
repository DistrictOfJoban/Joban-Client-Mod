package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.block.entity.RVPIDSBlockEntity;
import com.lx862.jcm.mod.data.pids.PIDSManager;
import com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongImmutableList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.render.StoredMatrixTransformations;

public class RVPIDSRenderer extends PIDSRenderer<RVPIDSBlockEntity> {
    public RVPIDSRenderer(Argument dispatcher) {
        super(dispatcher);
    }

    @Override
    public void renderPIDS(RVPIDSBlockEntity blockEntity, PIDSPresetBase pidsPreset, GraphicsHolder graphicsHolder, StoredMatrixTransformations storedMatrixTransformations, World world, BlockState state, BlockPos pos, Direction facing, LongImmutableList targetPlatformIds, ObjectArrayList<ArrivalResponse> arrivals, float tickDelta, boolean[] rowHidden, int light) {
        storedMatrixTransformations.add(graphicsHolderNew -> {
            graphicsHolderNew.translate(-0.21, -0.14, -0.128);
        });
        pidsPreset.render(blockEntity, graphicsHolder, storedMatrixTransformations, world, blockEntity.getPos2(), facing, targetPlatformIds, arrivals, rowHidden, tickDelta, 0, 0, 136, 76, light);
    }

    @Override
    public boolean rendersOutsideBoundingBox2(RVPIDSBlockEntity blockEntity) {
        PIDSPresetBase pidsPreset = PIDSManager.getPreset(blockEntity.getPresetId());
        if(pidsPreset != null && pidsPreset.outOfBoundAllowed()) return true;
        return super.rendersOutsideBoundingBox2(blockEntity);
    }

    @Override
    public boolean isInRenderDistance(RVPIDSBlockEntity blockEntity, Vector3d position) {
        PIDSPresetBase pidsPreset = PIDSManager.getPreset(blockEntity.getPresetId());
        if(pidsPreset != null && pidsPreset.outOfBoundAllowed()) return true;
        return super.isInRenderDistance(blockEntity, position);
    }
}
