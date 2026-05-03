package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.block.base.DirectionalBlock;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.resource.ComplexModelStorage;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockEntityRenderer;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.block.BlockEyeCandy;
import org.mtr.mod.client.CustomResourceLoader;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.StoredMatrixTransformations;
import org.mtr.mod.resource.OptimizedModelWrapper;

public class ComplexModelRenderer<T extends BlockEntityExtension> extends BlockEntityRenderer<T> {
    private final Identifier modelLocation;

    public ComplexModelRenderer(Identifier modelLocation, BlockEntityRenderer.Argument dispatcher) {
        super(dispatcher);
        this.modelLocation = modelLocation;
        ComplexModelStorage.requestModelLoad(modelLocation);
    }

    public void render(T blockEntity, float tickDelta, GraphicsHolder graphicsHolder, int light, int overlay) {
        World world = blockEntity.getWorld2();
        if (world != null) {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            ClientPlayerEntity clientPlayerEntity = minecraftClient.getPlayerMapped();
            if (clientPlayerEntity != null) {
                StoredMatrixTransformations storedMatrixTransformations = new StoredMatrixTransformations((double)0.5F + (double)blockEntity.getPos2().getX(), (double)blockEntity.getPos2().getY(), (double)0.5F + (double)blockEntity.getPos2().getZ());

                BlockPos blockPos = blockEntity.getPos2();
                Direction facing = Direction.convert(blockEntity.getCachedState2().get(new Property<>(DirectionalBlock.FACING.data)));
                OptimizedModelWrapper model = ComplexModelStorage.getModel(modelLocation);

                if(model != null) {
                    StoredMatrixTransformations storedMatrixTransformationsNew = storedMatrixTransformations.copy();
                    storedMatrixTransformationsNew.add((graphicsHolderNew) -> {
                        graphicsHolderNew.rotateYDegrees(180.0F - facing.asRotation());
                        graphicsHolderNew.rotateZDegrees(180F);
                    });
                    MainRenderer.scheduleRender(QueuedRenderLayer.TEXT, (gfxHolder, offset) -> {
                        storedMatrixTransformationsNew.transform(gfxHolder, offset);
                        CustomResourceLoader.OPTIMIZED_RENDERER_WRAPPER.queue(model, gfxHolder, light);
                        gfxHolder.pop();
                    });
                }
            }
        }
    }

    public boolean rendersOutsideBoundingBox2(BlockEyeCandy.BlockEntity blockEntity) {
        return true;
    }

    public boolean isInRenderDistance(BlockEyeCandy.BlockEntity blockEntity, Vector3d position) {
        return true;
    }
}
