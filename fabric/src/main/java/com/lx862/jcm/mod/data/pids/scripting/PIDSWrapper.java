package com.lx862.jcm.mod.data.pids.scripting;

import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.core.operation.ArrivalsResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class PIDSWrapper {
    private final PIDSBlockEntity be;
    private final ArrivalsWrapper arrivalsWrapper;

    public PIDSWrapper(PIDSBlockEntity be, ObjectArrayList<ArrivalResponse> arrivalsResponse) {
        this.be = be;
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

    public ArrivalsWrapper arrivals() {
        return arrivalsWrapper;
    }
}
