package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.data.pids.PIDSManager;
import com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongImmutableList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.InitClient;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.data.ArrivalsCacheClient;

public abstract class PIDSRenderer<T extends PIDSBlockEntity> extends JCMBlockEntityRenderer<T> {
    public PIDSRenderer(Argument dispatcher) {
        super(dispatcher);
    }

    @Override
    public void renderCurated(T blockEntity, GraphicsHolder graphicsHolder, World world, BlockState state, BlockPos pos, float tickDelta, int light, int i1) {
        PIDSPresetBase pidsPreset = getPreset(blockEntity);
        if(pidsPreset == null) return;

        Direction facing = IBlock.getStatePropertySafe(state, BlockProperties.FACING);

        boolean[] rowHidden = new boolean[blockEntity.getRowAmount()];
        boolean[] beRowHidden = blockEntity.getRowHidden();
        for(int i = 0; i < rowHidden.length; i++) {
            rowHidden[i] = pidsPreset.isRowHidden(i) || beRowHidden[i];
        }

        LongImmutableList platforms;
        if(!blockEntity.getPlatformIds().isEmpty()) {
            platforms = new LongImmutableList(blockEntity.getPlatformIds());
        } else {
            LongArrayList closestPlatforms = new LongArrayList();
            InitClient.findClosePlatform(pos, 5, e -> closestPlatforms.add(e.getId()));
            platforms = new LongImmutableList(closestPlatforms);
        }

        ObjectArrayList<ArrivalResponse> arrivals = ArrivalsCacheClient.INSTANCE.requestArrivals(platforms);

        graphicsHolder.push();
        graphicsHolder.translate(0.5, 0.5, 0.5);
        graphicsHolder.rotateYDegrees(90 - facing.asRotation());
        graphicsHolder.rotateZDegrees(180);
        renderPIDS(blockEntity, pidsPreset, graphicsHolder, world, state, pos, facing, arrivals, tickDelta, rowHidden);
        graphicsHolder.pop();
    }

    public abstract void renderPIDS(T blockEntity, PIDSPresetBase pidsPreset, GraphicsHolder graphicsHolder, World world, BlockState state, BlockPos pos, Direction facing, ObjectArrayList<ArrivalResponse> arrivals, float tickDelta, boolean[] rowHidden);

    private PIDSPresetBase getPreset(PIDSBlockEntity blockEntity) {
        return PIDSManager.getPreset(blockEntity.getPresetId(), PIDSManager.getPreset(blockEntity.getDefaultPresetId()));
    }
}
