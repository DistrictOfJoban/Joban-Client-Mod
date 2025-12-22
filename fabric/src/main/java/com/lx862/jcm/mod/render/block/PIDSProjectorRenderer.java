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
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.StoredMatrixTransformations;

public class PIDSProjectorRenderer extends PIDSRenderer<PIDSProjectorBlockEntity> {
    public PIDSProjectorRenderer(Argument dispatcher) {
        super(dispatcher);
    }

    @Override
    public void renderPIDS(PIDSProjectorBlockEntity blockEntity, PIDSPresetBase pidsPreset, GraphicsHolder graphicsHolder, StoredMatrixTransformations storedMatrixTransformations, World world, BlockState state, BlockPos pos, Direction facing, ObjectArrayList<ArrivalResponse> arrivals, float tickDelta, boolean[] rowHidden) {
        float appliedScale = (float)blockEntity.getScale();
        float offsetX = (float)(0.5 - blockEntity.getOffsetX());
        float offsetY = (float)(0.5 + blockEntity.getOffsetY());
        float offsetZ = (float)(-0.5 - blockEntity.getOffsetZ());
        boolean showOutline = JCMUtil.playerHoldingBrush(PlayerEntity.cast(MinecraftClient.getInstance().getPlayerMapped()));

        storedMatrixTransformations.add(graphicsHolderNew -> {
            graphicsHolderNew.rotateYDegrees(90);

            graphicsHolderNew.translate(-0.5 + blockEntity.getOffsetX(), -0.5 - blockEntity.getOffsetY(), 0.5 + blockEntity.getOffsetZ());
            graphicsHolderNew.rotateXDegrees((float)blockEntity.getRotateX());
            graphicsHolderNew.rotateYDegrees((float)blockEntity.getRotateY());
            graphicsHolderNew.rotateZDegrees((float)blockEntity.getRotateZ());
        });

        // Draw projection effect
        if(showOutline && blockEntity.getRotateX() == 0 && blockEntity.getRotateY() == 0 && blockEntity.getRotateZ() == 0) {
            MainRenderer.scheduleRender(QueuedRenderLayer.LINES, (graphicsHolderNew, offset) -> {
//              graphicsHolderNew.push(); // Applied with storedMatrixTransformations.transform
                storedMatrixTransformations.transform(graphicsHolderNew, offset);
                graphicsHolderNew.createVertexConsumer(RenderLayer.getLines());

                graphicsHolderNew.drawLineInWorld(offsetX, offsetY, offsetZ, 0, 0, 0, 0xFFFF0000);
                graphicsHolderNew.drawLineInWorld(offsetX, offsetY, offsetZ, 0 + (1.785f * appliedScale), 0, 0, 0xFFFF0000);

                graphicsHolderNew.drawLineInWorld(offsetX, offsetY, offsetZ, 0, 0 + (1 * appliedScale), 0, 0xFFFF0000);
                graphicsHolderNew.drawLineInWorld(offsetX, offsetY, offsetZ, 0 + (1.785f * appliedScale), 0 + (1 * appliedScale), 0, 0xFFFF0000);
                graphicsHolderNew.pop();
            });
        }

        StoredMatrixTransformations newMatrices = storedMatrixTransformations.copy();
        newMatrices.add(graphicsHolderNew -> {
            graphicsHolderNew.scale(1.26315f, 1.26315f, 1.26315f);
            graphicsHolderNew.scale(appliedScale, appliedScale, appliedScale);
        });

        pidsPreset.render(blockEntity, graphicsHolder, newMatrices.copy(), world, blockEntity.getPos2(), facing, arrivals, rowHidden, tickDelta, 0, 0, 136, 76);

        // Border
        if(showOutline) {
            MainRenderer.scheduleRender(QueuedRenderLayer.LIGHT, (graphicsHolderNew, offset) -> {
//            graphicsHolderNew.push(); // Applied with storedMatrixTransformations.transform
                newMatrices.transform(graphicsHolderNew, offset);
                graphicsHolderNew.scale(PIDSPresetBase.BASE_SCALE, PIDSPresetBase.BASE_SCALE, PIDSPresetBase.BASE_SCALE);
                graphicsHolderNew.createVertexConsumer(RenderLayer.getBeaconBeam(Constants.id("textures/block/light_1.png"), false));
                RenderHelper.drawTexture(graphicsHolderNew, -1, -1, 0.1f, 138, 78, facing, 0xFFFF0000, MAX_RENDER_LIGHT);
                graphicsHolderNew.pop();
            });
        }
    }
}