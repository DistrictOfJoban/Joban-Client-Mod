package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.config.ClientConfig;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.util.BlockUtil;
import net.fabricmc.loader.api.FabricLoader;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockEntityRenderer;
import org.mtr.mapping.mapper.GraphicsHolder;

public abstract class JCMBlockEntityRenderer<T extends BlockEntityExtension> extends BlockEntityRenderer<T> implements RenderHelper {

    public JCMBlockEntityRenderer(Argument argument) {
        super(argument);
    }

    @Override
    public void render(T blockEntity, float tickDelta, GraphicsHolder graphicsHolder, int light, int i1) {
        if(ClientConfig.DISABLE_RENDERING.get() || blockEntity.getWorld2() == null) return;
        if(blockEntity.getWorld2().getBlockState(blockEntity.getPos2()).isAir()) return;

        try {
            renderCurated(blockEntity, tickDelta, graphicsHolder, light, i1);
        } catch (Exception e) {
            if(FabricLoader.getInstance().isDevelopmentEnvironment()) {
                e.printStackTrace();
            } else {
                throw e;
            }
        }

    }

    /**
     * Same as the default block entity render method, but only called in safe condition and when rendering are not disabled
     */
    public abstract void renderCurated(T blockEntity, float tickDelta, GraphicsHolder graphicsHolder, int light, int i1);

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
