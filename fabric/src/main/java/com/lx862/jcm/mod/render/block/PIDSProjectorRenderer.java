package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.block.entity.PIDSProjectorBlockEntity;
import com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.util.JCMUtil;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.GraphicsHolder;

public class PIDSProjectorRenderer extends PIDSRenderer<PIDSProjectorBlockEntity> {
    public PIDSProjectorRenderer(Argument dispatcher) {
        super(dispatcher);
    }

    @Override
    public void renderPIDS(PIDSProjectorBlockEntity blockEntity, PIDSPresetBase pidsPreset, GraphicsHolder graphicsHolder, World world, BlockState state, BlockPos pos, Direction facing, ObjectArrayList<ArrivalResponse> arrivals, float tickDelta, boolean[] rowHidden) {
        boolean showOutline = false;
        if(JCMUtil.playerHoldingBrush(PlayerEntity.cast(MinecraftClient.getInstance().getPlayerMapped()))) {
            final HitResult hitResult = MinecraftClient.getInstance().getCrosshairTargetMapped();
            if(hitResult != null && hitResult.getType() == HitResultType.BLOCK) {
                BlockHitResult bhr = BlockHitResult.cast(hitResult);
                showOutline = bhr.getBlockPos().equals(blockEntity.getPos2());
            }
        }

        graphicsHolder.rotateYDegrees(90);
        graphicsHolder.translate(-0.5 + blockEntity.getX(), -0.5 + blockEntity.getY(), 0.5 + blockEntity.getZ());
        graphicsHolder.scale(1/76F, 1/76F, 1/76F);

        graphicsHolder.scale(blockEntity.getScale(), blockEntity.getScale(), blockEntity.getScale());
        graphicsHolder.rotateXDegrees(blockEntity.getRotateX());
        graphicsHolder.rotateYDegrees(blockEntity.getRotateY());
        graphicsHolder.rotateZDegrees(blockEntity.getRotateZ());

        pidsPreset.render(blockEntity, graphicsHolder, world, blockEntity.getPos2(), facing, arrivals, rowHidden, tickDelta, 0, 0, 136, 76);

        if(showOutline) {
            graphicsHolder.createVertexConsumer(RenderLayer.getBeaconBeam(Constants.id("textures/block/light_1.png"), false));
            RenderHelper.drawTexture(graphicsHolder, -8, -1, 0.1f, 138, 78, facing, 0xFFFF0000, MAX_RENDER_LIGHT);
        }
    }
}