package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.JCMClient;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockEntityRenderer;
import org.mtr.mapping.mapper.GraphicsHolder;

import javax.annotation.Nonnull;

public abstract class JCMBlockEntityRenderer<T extends BlockEntityExtension> extends BlockEntityRenderer<T> implements RenderHelper {
    public JCMBlockEntityRenderer(Argument dispatcher) {
        super(dispatcher);
    }

    @Override
    public void render(@Nonnull T blockEntity, float tickDelta, @Nonnull GraphicsHolder graphicsHolder, int light, int i1) {
        try {
            World world = blockEntity.getWorld2();
            BlockPos pos = blockEntity.getPos2();
            if(JCMClient.getConfig().disableRendering || world == null) return;

            if(world.getBlockState(pos).isAir()) return;
            BlockState state = world.getBlockState(pos);

            renderCurated(blockEntity, graphicsHolder, world, state, pos, tickDelta, light, i1);
        } catch (Exception e) {
            JCMLogger.error("An exception occurred while rendering Block Entity!");
            JCMLogger.error("This is a bug, please report to the developers of Joban Client Mod!");
            throw new Error("An exception occurred while rendering Block Entity.", e);
        }
    }

    /**
     * Same as the default block entity render method, but only called in safe condition and when rendering are not disabled
     */
    public abstract void renderCurated(T blockEntity, GraphicsHolder graphicsHolder, World world, BlockState state, BlockPos pos, float tickDelta, int light, int i1);
    public void rotateToBlockDirection(GraphicsHolder graphicsHolder, T blockEntity) {
        World world = blockEntity.getWorld2();
        if(world != null) {
            BlockState state = world.getBlockState(blockEntity.getPos2());
            Direction facing = BlockUtil.getProperty(state, BlockProperties.FACING);
            graphicsHolder.rotateYDegrees(-facing.asRotation());
        }
    }
}
