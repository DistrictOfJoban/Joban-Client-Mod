package com.lx862.jcm.mod.scripting.train;

import com.lx862.jcm.mod.scripting.pids.PIDSScriptContext;
import com.lx862.jcm.mod.scripting.pids.PIDSWrapper;
import com.lx862.mtrscripting.scripting.ParsedScript;
import com.lx862.mtrscripting.scripting.base.ScriptInstance;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.VehicleExtension;

import java.util.ArrayList;
import java.util.List;

public class TrainScriptInstance extends ScriptInstance<PIDSWrapper> {
    private final VehicleExtension vehicleExtension;
    public final List<TrainModelDrawCall> drawCalls;

    public TrainScriptInstance(VehicleExtension vehicleExtension, ParsedScript script) {
        super(new PIDSScriptContext(), script);
        this.vehicleExtension = vehicleExtension;
        this.drawCalls = new ArrayList<>();
    }

    public boolean isDead() {
        return !MinecraftClientData.getInstance().vehicles.contains(vehicleExtension);
    }
}
