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
        graphicsHolder.rotateYDegrees(90);
        float scale = (float)blockEntity.getScale();
        boolean showOutline = JCMUtil.playerHoldingBrush(PlayerEntity.cast(MinecraftClient.getInstance().getPlayerMapped()));
        graphicsHolder.translate(-0.5 + blockEntity.getX(), -0.5 - blockEntity.getY(), 0.5 + blockEntity.getZ());

        graphicsHolder.rotateXDegrees((float)blockEntity.getRotateX());
        graphicsHolder.rotateYDegrees((float)blockEntity.getRotateY());
        graphicsHolder.rotateZDegrees((float)blockEntity.getRotateZ());

        // Draw projection effect
        if(showOutline && blockEntity.getRotateX() == 0 && blockEntity.getRotateY() == 0 && blockEntity.getRotateZ() == 0) {
            graphicsHolder.push();
            graphicsHolder.createVertexConsumer(RenderLayer.getLines());

            float offsetX = (float)(0.5 - blockEntity.getX());
            float offsetY = (float)(0.5 + blockEntity.getY());
            float offsetZ = (float)(-0.5 - blockEntity.getZ());

            graphicsHolder.drawLineInWorld(offsetX, offsetY, offsetZ, 0, 0, 0, 0xFFFF0000);
            graphicsHolder.drawLineInWorld(offsetX, offsetY, offsetZ, 0 + (1.785f * scale), 0, 0, 0xFFFF0000);

            graphicsHolder.drawLineInWorld(offsetX, offsetY, offsetZ, 0, 0 + (1 * scale), 0, 0xFFFF0000);
            graphicsHolder.drawLineInWorld(offsetX, offsetY, offsetZ, 0 + (1.785f * scale), 0 + (1 * scale), 0, 0xFFFF0000);
            graphicsHolder.pop();
        }

        graphicsHolder.scale(1/76F, 1/76F, 1/76F);
        graphicsHolder.scale(scale, scale, scale);
        pidsPreset.render(blockEntity, graphicsHolder, world, blockEntity.getPos2(), facing, arrivals, rowHidden, tickDelta, 0, 0, 136, 76);

        // Border
        if(showOutline) {
            graphicsHolder.createVertexConsumer(RenderLayer.getBeaconBeam(Constants.id("textures/block/light_1.png"), false));
            RenderHelper.drawTexture(graphicsHolder, -8, -1, 0.1f, 138, 78, facing, 0xFFFF0000, MAX_RENDER_LIGHT);
        }
    }
}