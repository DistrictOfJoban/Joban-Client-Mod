package com.lx862.jcm.mod.data.pids.scripting;

import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import org.mtr.core.data.Station;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mod.InitClient;

public class PIDSWrapper {
    private final PIDSBlockEntity be;
    private final ArrivalsWrapper arrivalsWrapper;
    public final String type;
    public final int width;
    public final int height;

    public PIDSWrapper(PIDSBlockEntity be, ObjectArrayList<ArrivalResponse> arrivalsResponse, int width, int height) {
        this.be = be;
        this.type = be.getPIDSType();
        this.width = width;
        this.height = height;
        this.arrivalsWrapper = new ArrivalsWrapper(arrivalsResponse);
    }

    public boolean isRowHidden(int i) {
        boolean[] rowHidden = be.getRowHidden();
        if(i >= rowHidden.length) {
            return false;
        } else {
            return rowHidden[i];
        }
    }

    public String getCustomMessage(int i) {
        String[] customMessages = be.getCustomMessages();
        return i >= customMessages.length ? null : customMessages[i];
    }

    public boolean isPlatformNumberHidden() {
        return be.platformNumberHidden();
    }

    public Station station() {
        return InitClient.findStation(be.getPos2());
    }

    public ArrivalsWrapper arrivals() {
        return arrivalsWrapper;
    }
}
