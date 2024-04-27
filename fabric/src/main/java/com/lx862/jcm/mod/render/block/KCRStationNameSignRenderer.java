package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.block.entity.KCRStationNameSignBlockEntity;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.render.IDrawingJoban;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.core.data.Station;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.InitClient;
import org.mtr.mod.data.IGui;

public class KCRStationNameSignRenderer extends JCMBlockEntityRenderer<KCRStationNameSignBlockEntity> {
    public KCRStationNameSignRenderer(Argument dispatcher) {
        super(dispatcher);
    }

    @Override
    public void renderCurated(KCRStationNameSignBlockEntity blockEntity, GraphicsHolder graphicsHolder, World world, BlockState state, BlockPos pos, float tickDelta, int light, int i1) {
        graphicsHolder.translate(0.5, 0.5, 0.5);
        rotateToBlockDirection(graphicsHolder, blockEntity);
        graphicsHolder.rotateZDegrees(180);
        graphicsHolder.rotateYDegrees(180);

        final Station station = InitClient.findStation(blockEntity.getPos2());
        final String stationName = station == null ? TextUtil.translatable("gui.mtr.untitled").getString() : station.getName();
        double offsetForExitDirection = BlockUtil.getProperty(state, BlockProperties.EXIT_ON_LEFT) ? -0.225 : 0.225;

        // Draw both sides
        for(int i = 0; i < 2; i++) {
            graphicsHolder.push();
            graphicsHolder.translate(offsetForExitDirection, 0, 0);
            if(i == 1) graphicsHolder.rotateYDegrees(180); // Other side
            graphicsHolder.translate(0, -0.05, -0.175);
            graphicsHolder.scale(0.021F, 0.021F, 0.021F);
            IDrawingJoban.drawStringWithFont(graphicsHolder, stationName, new Identifier("jsblock:kcr_sign"), IGui.HorizontalAlignment.CENTER, IGui.VerticalAlignment.CENTER, 0, 0, 60, 32, 1, 0xEEEEEE, false, GraphicsHolder.getDefaultLight(), null);
            graphicsHolder.pop();
        }
    }
}
