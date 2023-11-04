package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.data.pids.PIDSManager;
import com.lx862.jcm.mod.data.pids.base.PIDSPreset;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;

public class RVPIDSRenderer extends JCMBlockEntityRenderer<PIDSBlockEntity> {
    public RVPIDSRenderer(Argument argument) {
        super(argument);
    }

    @Override
    public void renderCurated(PIDSBlockEntity blockEntity, float tickDelta, GraphicsHolder graphicsHolder, int light, int i1) {
        World world = blockEntity.getWorld2();
        if(world == null) return;

        PIDSPreset pidsPreset = getPreset(blockEntity, "rv_pids");

        graphicsHolder.push();
        scaleCentered(graphicsHolder, 0.008F, 0.008F, 0.008F);
        BlockState state = blockEntity.getWorld2().getBlockState(blockEntity.getPos2());
        Direction facing = BlockUtil.getProperty(state, BlockProperties.FACING);
        graphicsHolder.rotateYDegrees(90 - facing.asRotation());
        graphicsHolder.rotateZDegrees(180);
        graphicsHolder.translate(-25, -18, -17);

        pidsPreset.render(blockEntity, graphicsHolder, blockEntity.getWorld2(), tickDelta, 0, 0, 175, 97, 0xFFFFFFFF, MAX_RENDER_LIGHT);

        graphicsHolder.pop();
    }

    private PIDSPreset getPreset(PIDSBlockEntity blockEntity, String defaultId) {
        return PIDSManager.presetList.getOrDefault(blockEntity.getPidsPresetId(), PIDSManager.presetList.get(defaultId));
    }
}
