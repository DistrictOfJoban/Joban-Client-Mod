package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.block.entity.LCDPIDSBlockEntity;
import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.data.pids.PIDSManager;
import com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;

public class LCDPIDSRenderer extends JCMBlockEntityRenderer<LCDPIDSBlockEntity> {
    public LCDPIDSRenderer(Argument dispatcher) {
        super(dispatcher);
    }

    @Override
    public void renderCurated(LCDPIDSBlockEntity blockEntity, GraphicsHolder graphicsHolder, World world, BlockState state, BlockPos pos, float tickDelta, int light, int i1) {
        PIDSPresetBase pidsPreset = getPreset(blockEntity);
        if(pidsPreset == null) return;

        Direction facing = BlockUtil.getProperty(state, BlockProperties.FACING);
        
        graphicsHolder.push();
        graphicsHolder.translate(0.5, 0.5, 0.5);
        graphicsHolder.scale(0.009F, 0.009F, 0.009F);
        graphicsHolder.rotateYDegrees(90 - facing.asRotation());
        graphicsHolder.rotateZDegrees(180);
        graphicsHolder.translate(-20, -14, -14.25);

        pidsPreset.render(blockEntity, graphicsHolder, world, facing, tickDelta, 0, 0, 153, 84, 0xFFFFFFFF, MAX_RENDER_LIGHT);
        graphicsHolder.pop();
    }

    private PIDSPresetBase getPreset(PIDSBlockEntity blockEntity) {
        return PIDSManager.getPreset(blockEntity.getPresetId(), PIDSManager.getPreset(blockEntity.getDefaultPresetId()));
    }
}
