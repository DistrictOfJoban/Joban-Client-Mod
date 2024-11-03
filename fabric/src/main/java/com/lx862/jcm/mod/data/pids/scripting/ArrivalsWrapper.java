package com.lx862.jcm.mod.data.pids.scripting;

import org.mtr.core.data.Platform;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ArrivalsWrapper {
    public final ObjectArrayList<ArrivalWrapper> arrivals;

    public ArrivalsWrapper(ObjectArrayList<ArrivalResponse> arrivalsResponse) {
        this.arrivals = new ObjectArrayList<>();
        for(ArrivalResponse arrivalResponse : arrivalsResponse) {
            this.arrivals.add(new ArrivalWrapper(arrivalResponse));
        }
    }

    public ArrivalWrapper get(int i) {
        return i >= arrivals.size() ? null : arrivals.get(i);
    }

    public boolean mixedCarLength() {
        int car = -1;
        for(ArrivalWrapper arrivalResponse : arrivals) {
            if(car == -1) car = arrivalResponse.carCount();
            if(car != arrivalResponse.carCount()) return true;
        }
        return false;
    }

    public ObjectArrayList<Platform> platforms() {
        ObjectArrayList<Platform> platforms = new ObjectArrayList<>();
        for(ArrivalWrapper wrapper : arrivals) {
            Platform plat = wrapper.platform();
            if(plat != null) {
                platforms.add(plat);
            }
        }
        return platforms;
    }
}
