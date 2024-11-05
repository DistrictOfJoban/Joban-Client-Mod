package com.lx862.jcm.mod.data.pids.scripting;

import org.mtr.core.data.Platform;
import org.mtr.core.data.Route;
import org.mtr.core.data.SimplifiedRoute;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.core.operation.CarDetails;
import org.mtr.mod.client.MinecraftClientData;

import java.util.function.Consumer;

public class ArrivalWrapper {
    public final ArrivalResponse arrivalResponse;

    public ArrivalWrapper(ArrivalResponse arrivalResponse) {
        this.arrivalResponse = arrivalResponse;
    }

    public String destination() {
        return arrivalResponse.getDestination();
    }

    public long arrivalTime() {
        return arrivalResponse.getArrival();
    }

    public boolean arrived() {
        return arrivalTime() <= System.currentTimeMillis();
    }

    public long departureTime() {
        return arrivalResponse.getDeparture();
    }

    public boolean departed() {
        return departureTime() <= System.currentTimeMillis();
    }

    public long deviation() {
        return arrivalResponse.getDeviation();
    }

    public boolean realtime() {
        return arrivalResponse.getRealtime();
    }

    public long departureIndex() {
        return arrivalResponse.getDepartureIndex();
    }

    public boolean terminating() {
        return arrivalResponse.getIsTerminating();
    }

    public SimplifiedRoute route() {
        for(SimplifiedRoute route : MinecraftClientData.getInstance().simplifiedRoutes) {
            if(route.getId() == routeId()) {
                return route;
            }
        }
        return null;
    }

    public long routeId() {
        return arrivalResponse.getRouteId();
    }

    public String routeName() {
        return arrivalResponse.getRouteName();
    }

    public String routeNumber() {
        return arrivalResponse.getRouteNumber();
    }

    public int routeColor() {
        return arrivalResponse.getRouteColor();
    }

    public Route.CircularState circularState() {
        return arrivalResponse.getCircularState();
    }

    public Platform platform() {
        return MinecraftClientData.getInstance().platformIdMap.get(platformId());
    }

    public long platformId() {
        return arrivalResponse.getPlatformId();
    }

    public String platformName() {
        return arrivalResponse.getPlatformName();
    }

    public int carCount() {
        return arrivalResponse.getCarCount();
    }

    public void forEachCar(Consumer<CarDetails> consumer) {
        arrivalResponse.iterateCarDetails(consumer);
    }
}
