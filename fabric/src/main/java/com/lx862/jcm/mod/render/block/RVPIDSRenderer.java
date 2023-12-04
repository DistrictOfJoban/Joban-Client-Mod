package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.block.entity.RVPIDSBlockEntity;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.data.pids.PIDSManager;
import com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;

public class RVPIDSRenderer extends JCMBlockEntityRenderer<RVPIDSBlockEntity> {
    public RVPIDSRenderer(Argument dispatcher) {
        super(dispatcher);
    }

    @Override
    public void renderCurated(RVPIDSBlockEntity blockEntity, float tickDelta, GraphicsHolder graphicsHolder, int light, int i1) {
        PIDSPresetBase pidsPreset = getPreset(blockEntity, "rv_pids");
        if(pidsPreset == null) return;

        BlockState state = blockEntity.getWorld2().getBlockState(blockEntity.getPos2());
        Direction facing = BlockUtil.getProperty(state, BlockProperties.FACING);
        
        graphicsHolder.push();
        scaleCentered(graphicsHolder, 0.009F, 0.009F, 0.009F);
        graphicsHolder.rotateYDegrees(90 - facing.asRotation());
        graphicsHolder.rotateZDegrees(180);
        graphicsHolder.translate(-22.5, -17, -14.5);

        pidsPreset.render(blockEntity, graphicsHolder, blockEntity.getWorld2(), facing, tickDelta, 0, 0, 156, 88, 0xFFFFFFFF, MAX_RENDER_LIGHT);
        graphicsHolder.pop();
    }

    private PIDSPresetBase getPreset(PIDSBlockEntity blockEntity, String defaultId) {
        return PIDSManager.getPreset(blockEntity.getPidsPresetId(), PIDSManager.getBuiltInPreset(defaultId));
    }
}
