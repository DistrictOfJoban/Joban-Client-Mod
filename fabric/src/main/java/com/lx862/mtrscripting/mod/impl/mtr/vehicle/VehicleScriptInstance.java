package com.lx862.mtrscripting.mod.impl.mtr.vehicle;

import com.lx862.mtrscripting.core.integration.MinecraftClientWrapper;
import com.lx862.mtrscripting.core.primitive.ParsedScript;
import com.lx862.mtrscripting.core.primitive.ScriptInstance;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import com.lx862.mtrscripting.mod.gui.hud.SortableScriptInstance;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.client.VehicleRidingMovement;
import org.mtr.mod.data.VehicleExtension;
import org.mtr.mod.render.PositionAndRotation;

public class VehicleScriptInstance extends ScriptInstance<VehicleWrapper> implements SortableScriptInstance {
    private final VehicleExtension vehicleExtension;
    public final VehicleScriptCallsHolder.Captured capturedScriptCalls;

    /* Live-vehicle id set, rebuilt ONCE per rendered frame by
       RenderVehiclesMixin (beginFrame). shouldInvalidate runs once per
       scripted vehicle per frame plus once per instance per
       clearDeadInstance tick — the old per-call stream() scan was O(V)
       each time, ~hundreds of thousands of stream ops per second with a
       few dozen scripted trains in a busy world. Null until the first
       rendered frame; the fallback keeps identical semantics for the
       pre-first-frame window. */
    private static volatile org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongOpenHashSet liveVehicleIds;

    public VehicleScriptInstance(VehicleScriptContext context, VehicleExtension vehicleExtension, ParsedScript script) {
        super(context, script);
        this.vehicleExtension = vehicleExtension;
        this.capturedScriptCalls = new VehicleScriptCallsHolder.Captured();
    }

    /** One O(V) pass per rendered frame (RenderVehiclesMixin, render thread). */
    public static void beginFrame() {
        final org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongOpenHashSet ids =
                new org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        for(final VehicleExtension vehicle : MinecraftClientData.getInstance().vehicles) {
            ids.add(vehicle.getId());
        }
        liveVehicleIds = ids;
    }

    public boolean shouldInvalidate() {
        if(MinecraftClient.getInstance().getWorldMapped() == null) return true;
        final org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongOpenHashSet ids = liveVehicleIds;
        if(ids != null) {
            return !ids.contains(vehicleExtension.getId());
        }
        return MinecraftClientData.getInstance().vehicles.stream().noneMatch(v -> v.getId() == vehicleExtension.getId());
    }

    @Override
    public int getSortScore() {
        if(VehicleRidingMovement.isRiding(getWrapperObject().getId())) {
            return Integer.MIN_VALUE;
        }

        ScriptVector3f playerPos = MinecraftClientWrapper.localPlayer().pos();
        double closestDistance = Double.MAX_VALUE;
        for(PositionAndRotation positionAndRotation : getWrapperObject().posAndRotations) {
            double distance = playerPos.distance(new ScriptVector3f(positionAndRotation.position));
            if(distance < closestDistance) {
                closestDistance = distance;
            }
        }

        return (int)(closestDistance * 1000);
    }
}
