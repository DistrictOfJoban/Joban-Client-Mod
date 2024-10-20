package com.lx862.jcm.mod.data.pids.scripting;

import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ArrivalsWrapper {
    public final ObjectArrayList<ArrivalResponse> arrivalsResponse;
    public ArrivalsWrapper(ObjectArrayList<ArrivalResponse> arrivalsResponse) {
        this.arrivalsResponse = arrivalsResponse;
    }

    public ArrivalResponse get(int i) {
        return i >= arrivalsResponse.size() ? null : arrivalsResponse.get(i);
    }

    public boolean mixedCarLength() {
        int car = -1;
        for(ArrivalResponse arrivalResponse : arrivalsResponse) {
            if(car == -1) car = arrivalResponse.getCarCount();
            if(car != arrivalResponse.getCarCount()) return true;
        }
        return false;
    }
}
