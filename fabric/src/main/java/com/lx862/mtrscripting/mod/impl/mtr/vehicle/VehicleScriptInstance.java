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

    public VehicleScriptInstance(VehicleScriptContext context, VehicleExtension vehicleExtension, ParsedScript script) {
        super(context, script);
        this.vehicleExtension = vehicleExtension;
        this.capturedScriptCalls = new VehicleScriptCallsHolder.Captured();
    }

    public boolean shouldInvalidate() {
        boolean notInGame = MinecraftClient.getInstance().getWorldMapped() == null;
        return notInGame || MinecraftClientData.getInstance().vehicles.stream().noneMatch(v -> v.getId() == vehicleExtension.getId());
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
