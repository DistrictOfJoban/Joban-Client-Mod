package com.lx862.jcm.mod.scripting.pids;

import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.annotation.ValueNullable;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import org.mtr.core.data.Station;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongImmutableList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mod.InitClient;

public class PIDSWrapper {
    private final PIDSBlockEntity be;
    private final ArrivalsWrapper arrivalsWrapper;
    private final LongImmutableList targetPlatformIds;
    public final String type;
    public final int rows;
    public final int width;
    public final int height;

    @ApiInternal
    public PIDSWrapper(PIDSBlockEntity be, LongImmutableList targetPlatformIds, ObjectArrayList<ArrivalResponse> arrivalsResponse, int width, int height) {
        this.be = be;
        this.type = be.getPIDSType();
        this.width = width;
        this.height = height;
        this.rows = be.getRowAmount();
        this.targetPlatformIds = targetPlatformIds;
        this.arrivalsWrapper = new ArrivalsWrapper(arrivalsResponse);
    }

    public LongImmutableList getTargetPlatformIds() {
        return targetPlatformIds;
    }

    public ScriptVector3f blockPos() {
        return new ScriptVector3f(be.getPos2());
    }

    public boolean isKeyBlock() {
        return be.isKeyBlock();
    }

    public boolean isRowHidden(int i) {
        boolean[] rowHidden = be.getRowHidden();
        if(i >= rowHidden.length) {
            return false;
        } else {
            return rowHidden[i];
        }
    }

    public @ValueNullable String getCustomMessage(int i) {
        String[] customMessages = be.getCustomMessages();
        return i >= customMessages.length ? null : customMessages[i];
    }

    public boolean isPlatformNumberHidden() {
        return be.platformNumberHidden();
    }

    public @ValueNullable Station station() {
        return InitClient.findStation(be.getPos2());
    }

    public ArrivalsWrapper arrivals() {
        return arrivalsWrapper;
    }
}
