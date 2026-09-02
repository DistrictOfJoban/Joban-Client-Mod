package com.lx862.mtrscripting.mod.impl.mtr.vehicle;

import org.mtr.core.data.VehicleCar;
import org.mtr.core.tool.Vector;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import org.mtr.mod.data.VehicleExtension;

public final class VehicleRenderDataCache {
    private static final Object LOCK = new Object();
    private static Thread renderingThread;
    private static VehicleExtension cachedVehicle;
    private static ObjectArrayList<ObjectObjectImmutablePair<VehicleCar, ObjectArrayList<ObjectObjectImmutablePair<Vector, Vector>>>> cachedVehicleCarsAndPositions;

    private VehicleRenderDataCache() {
    }

    public static void startRendering() {
        synchronized (LOCK) {
            renderingThread = Thread.currentThread();
            cachedVehicle = null;
            cachedVehicleCarsAndPositions = null;
        }
    }

    public static ObjectArrayList<ObjectObjectImmutablePair<VehicleCar, ObjectArrayList<ObjectObjectImmutablePair<Vector, Vector>>>> getVehicleCarsAndPositions(VehicleExtension vehicle) {
        final Thread currentThread = Thread.currentThread();
        synchronized (LOCK) {
            if (renderingThread == currentThread) {
                if (cachedVehicle == vehicle && cachedVehicleCarsAndPositions != null) {
                    return cachedVehicleCarsAndPositions;
                }

                cachedVehicleCarsAndPositions = vehicle.getVehicleCarsAndPositions();
                cachedVehicle = vehicle;
                return cachedVehicleCarsAndPositions;
            }
        }
        return vehicle.getVehicleCarsAndPositions();
    }

    public static void finishRendering() {
        synchronized (LOCK) {
            if (renderingThread == Thread.currentThread()) {
                renderingThread = null;
                cachedVehicle = null;
                cachedVehicleCarsAndPositions = null;
            }
        }
    }
}
