package com.lx862.jcm.mod.scripting.jcm.pids;

import org.mtr.core.data.Platform;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.function.Consumer;

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

    public void forEach(Consumer<ArrivalWrapper> consumer) {
        for(ArrivalWrapper arrivalResponse : arrivals) {
            consumer.accept(arrivalResponse);
        }
    }

    public void forEach(long platformId, Consumer<ArrivalWrapper> consumer) {
        for(ArrivalWrapper arrivalResponse : arrivals) {
            if(arrivalResponse.platformId() != platformId) continue;
            consumer.accept(arrivalResponse);
        }
    }
}
