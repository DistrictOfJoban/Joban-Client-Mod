package com.lx862.jcm.mod.scripting.mtr.vehicle;

import com.lx862.jcm.mod.scripting.jcm.pids.PIDSWrapper;
import com.lx862.mtrscripting.core.ParsedScript;
import com.lx862.mtrscripting.core.ScriptInstance;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.VehicleExtension;

import java.util.ArrayList;
import java.util.List;

public class VehicleScriptInstance extends ScriptInstance<PIDSWrapper> {
    private final VehicleExtension vehicleExtension;
    public final List<VehicleModelDrawCall> drawCalls;

    public VehicleScriptInstance(VehicleExtension vehicleExtension, ParsedScript script) {
        super(new VehicleScriptContext(), script);
        this.vehicleExtension = vehicleExtension;
        this.drawCalls = new ArrayList<>();
    }

    public boolean isDead() {
        return !MinecraftClientData.getInstance().vehicles.contains(vehicleExtension);
    }
}
