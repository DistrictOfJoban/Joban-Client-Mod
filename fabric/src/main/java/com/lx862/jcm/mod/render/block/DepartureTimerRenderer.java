package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.block.entity.DepartureTimerBlockEntity;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.render.text.TextInfo;
import com.lx862.jcm.mod.render.text.TextRenderingManager;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.core.data.Platform;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.core.tool.Utilities;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongImmutableList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.InitClient;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.data.ArrivalsCacheClient;

import java.util.ArrayList;
import java.util.List;

public class DepartureTimerRenderer extends JCMBlockEntityRenderer<DepartureTimerBlockEntity> {
    public DepartureTimerRenderer(Argument dispatcher) {
        super(dispatcher);
    }

    @Override
    public void renderCurated(DepartureTimerBlockEntity blockEntity, GraphicsHolder graphicsHolder, World world, BlockState state, BlockPos pos, float tickDelta, int light, int i1) {
        Direction facing = IBlock.getStatePropertySafe(state, BlockProperties.FACING);

        graphicsHolder.push();
        graphicsHolder.translate(0.5, 0.5, 0.5);
        graphicsHolder.scale(0.018F, 0.018F, 0.018F);
        rotateToBlockDirection(graphicsHolder, blockEntity);
        graphicsHolder.rotateZDegrees(180);
        graphicsHolder.translate(-12.5, -2, -4.1);

        List<Platform> closestPlatforms = new ArrayList<>();
        InitClient.findClosePlatform(pos, 5, closestPlatforms::add);
        Platform closestPlatform = !closestPlatforms.isEmpty() ? closestPlatforms.get(0) : null;
        if (closestPlatform == null) return;

        ObjectArrayList<ArrivalResponse> arrivals = ArrivalsCacheClient.INSTANCE.requestArrivals(LongImmutableList.of(closestPlatform.getId()));
        ArrivalResponse firstArrival = Utilities.getElement(arrivals, 0);
        if (firstArrival == null) return;

        boolean arrived = firstArrival.getArrival() <= System.currentTimeMillis();
        long dwellLeft = Math.abs(firstArrival.getDeparture() - System.currentTimeMillis());
        if (!arrived) return;

        long seconds = dwellLeft / 1000;
        long mins = seconds / 60;

        TextRenderingManager.bind(graphicsHolder);
        TextRenderingManager.draw(graphicsHolder, new TextInfo(String.format("%d:%02d", mins % 10, seconds % 60)).withColor(0xFFEE2233).withFont("jsblock:deptimer"), facing, 0, 0);
        graphicsHolder.pop();
    }
}
