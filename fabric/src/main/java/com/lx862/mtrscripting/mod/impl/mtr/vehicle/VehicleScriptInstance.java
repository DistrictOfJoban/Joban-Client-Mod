package com.lx862.mtrscripting.mod.impl.mtr.vehicle;

import com.lx862.mtrscripting.core.primitive.ParsedScript;
import com.lx862.mtrscripting.core.primitive.ScriptInstance;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.VehicleExtension;

public class VehicleScriptInstance extends ScriptInstance<VehicleWrapper> {
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
}
