package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.block.entity.ButterflyLightBlockEntity;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.data.JCMClientStats;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.core.data.Platform;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.core.operation.ArrivalsResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongImmutableList;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.InitClient;
import org.mtr.mod.data.ArrivalsCache;

import java.util.ArrayList;
import java.util.List;

public class ButterflyLightRenderer extends JCMBlockEntityRenderer<ButterflyLightBlockEntity> {
    private static final Identifier BUTTERFLY_LIGHT_TEXTURE = new Identifier(Constants.MOD_ID, "textures/block/butterfly_light_dotmatrix.png");
    public ButterflyLightRenderer(Argument dispatcher) {
        super(dispatcher);
    }

    @Override
    public void renderCurated(ButterflyLightBlockEntity blockEntity, GraphicsHolder graphicsHolder, World world, BlockState state, BlockPos pos, float tickDelta, int light, int i1) {
        int startBlinkingSeconds = blockEntity.getStartBlinkingSeconds();
        Direction facing = BlockUtil.getProperty(state, BlockProperties.FACING);

        List<Platform> closestPlatforms = new ArrayList<>();
        InitClient.findClosePlatform(pos, 5, closestPlatforms::add);
        Platform closestPlatform = !closestPlatforms.isEmpty() ? closestPlatforms.get(0) : null;
        if (closestPlatform == null) return;

        ArrivalsResponse arrivals = ArrivalsCache.INSTANCE.requestArrivals(pos.asLong(), LongImmutableList.of(closestPlatform.getId()), 1, 0, true);
        ArrivalResponse firstArrival = arrivals.getArrivals().isEmpty() ? null : arrivals.getArrivals().get(0);
        if (firstArrival == null) return;

        boolean arrived = firstArrival.getArrival() <= System.currentTimeMillis();
        long dwellLeft = firstArrival.getDeparture() - System.currentTimeMillis();
        if (!arrived) return;

        long secondsLeft = dwellLeft / 1000;

        if(secondsLeft <= startBlinkingSeconds && JCMClientStats.getGameTick() % 40 > 20) {
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
