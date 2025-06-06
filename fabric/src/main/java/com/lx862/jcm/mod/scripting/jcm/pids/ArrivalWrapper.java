package com.lx862.jcm.mod.scripting.jcm.pids;

import org.mtr.core.data.Platform;
import org.mtr.core.data.Route;
import org.mtr.core.data.SimplifiedRoute;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.core.operation.CarDetails;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.ArrivalsCacheClient;

import java.util.ArrayList;
import java.util.List;

public class ArrivalWrapper {
    public final ArrivalResponse arrivalResponse;
    private final List<CarDetails> cars;

    public ArrivalWrapper(ArrivalResponse arrivalResponse) {
        this.arrivalResponse = arrivalResponse;
        this.cars = new ArrayList<>();
        arrivalResponse.iterateCarDetails(cars::add);
    }

    public String destination() {
        return arrivalResponse.getDestination();
    }

    public long arrivalTime() {
        return arrivalResponse.getArrival() - ArrivalsCacheClient.INSTANCE.getMillisOffset();
    }

    public boolean arrived() {
        return arrivalTime() <= System.currentTimeMillis();
    }

    public long departureTime() {
        return arrivalResponse.getDeparture() - ArrivalsCacheClient.INSTANCE.getMillisOffset();
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

    public List<CarDetails> cars() {
        return cars;
    }
}
