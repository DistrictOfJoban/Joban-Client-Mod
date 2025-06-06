package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.block.entity.ButterflyLightBlockEntity;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.render.RenderHelper;
import org.mtr.core.data.Platform;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.core.tool.Utilities;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongImmutableList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.InitClient;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.data.ArrivalsCacheClient;

import java.util.ArrayList;
import java.util.List;

public class ButterflyLightRenderer extends JCMBlockEntityRenderer<ButterflyLightBlockEntity> {
    private static final Identifier BUTTERFLY_LIGHT_TEXTURE = Constants.id("textures/block/butterfly_light_dotmatrix.png");

    public ButterflyLightRenderer(Argument dispatcher) {
        super(dispatcher);
    }

    @Override
    public void renderCurated(ButterflyLightBlockEntity blockEntity, GraphicsHolder graphicsHolder, World world, BlockState state, BlockPos pos, float tickDelta, int light, int i1) {
        int startBlinkingSeconds = blockEntity.getStartBlinkingSeconds();
        Direction facing = IBlock.getStatePropertySafe(state, BlockProperties.FACING);

        List<Platform> closestPlatforms = new ArrayList<>();
        InitClient.findClosePlatform(pos, 5, closestPlatforms::add);
        Platform closestPlatform = !closestPlatforms.isEmpty() ? closestPlatforms.get(0) : null;
        if (closestPlatform == null) return;

        ObjectArrayList<ArrivalResponse> arrivals = ArrivalsCacheClient.INSTANCE.requestArrivals(LongImmutableList.of(closestPlatform.getId()));
        ArrivalResponse firstArrival = Utilities.getElement(arrivals, 0);
        if (firstArrival == null) return;

        boolean arrived = firstArrival.getArrival() - ArrivalsCacheClient.INSTANCE.getMillisOffset() <= System.currentTimeMillis();
        long dwellLeft = firstArrival.getDeparture() - ArrivalsCacheClient.INSTANCE.getMillisOffset() - System.currentTimeMillis();
        if (!arrived) return;

        long secondsLeft = dwellLeft / 1000;

        if(secondsLeft <= startBlinkingSeconds && InitClient.getGameTick() % 40 > 20) {
            graphicsHolder.push();
            graphicsHolder.translate(0.5, 0.5, 0.5);
            graphicsHolder.scale(1/16F, 1/16F, 1/16F);
            rotateToBlockDirection(graphicsHolder, blockEntity);
            graphicsHolder.rotateZDegrees(180);
            graphicsHolder.translate(-8, 3, -2.05);

            graphicsHolder.createVertexConsumer(RenderLayer.getBeaconBeam(BUTTERFLY_LIGHT_TEXTURE, true));
            RenderHelper.drawTexture(graphicsHolder, 2, 0, 4, 12, 5, facing, ARGB_WHITE, MAX_RENDER_LIGHT);
            graphicsHolder.pop();
        }
    }
}
