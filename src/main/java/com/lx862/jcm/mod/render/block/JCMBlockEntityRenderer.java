package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.render.Renderable;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockEntityRenderer;
import org.mtr.mapping.mapper.GraphicsHolder;

public abstract class JCMBlockEntityRenderer<T extends BlockEntityExtension> extends BlockEntityRenderer<T> implements Renderable {

    public JCMBlockEntityRenderer(Argument argument) {
        super(argument);
    }

    public void scaleCentered(GraphicsHolder graphicsHolder, float x, float y, float z) {
        graphicsHolder.translate(0.5, 0.5, 0.5);
        graphicsHolder.scale(x, y, z);
        graphicsHolder.translate(-0.5, -0.5, -0.5);
    }

    public void rotateToBlockDirection(GraphicsHolder graphicsHolder, T blockEntity) {
        World world = blockEntity.getWorld2();
        if(world != null) {
            BlockState state = world.getBlockState(blockEntity.getPos2());
            Direction facing = BlockUtil.getProperty(state, BlockProperties.FACING);
            graphicsHolder.rotateYDegrees(-facing.asRotation());
        }
    }
}
