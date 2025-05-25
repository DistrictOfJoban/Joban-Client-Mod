package com.lx862.jcm.mod.scripting.mtr.vehicle;

import com.lx862.jcm.mod.scripting.mtr.sound.SoundCall;
import com.lx862.mtrscripting.core.ParsedScript;
import com.lx862.mtrscripting.core.ScriptInstance;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.VehicleExtension;

import java.util.ArrayList;
import java.util.List;

public class VehicleScriptInstance extends ScriptInstance<VehicleWrapper> {
    private final VehicleExtension vehicleExtension;
    public final List<VehicleModelDrawCall> carModelDrawCalls;
    private final List<SoundCall> carSoundCalls = new ArrayList<>();
    private final List<SoundCall> announceSoundCalls = new ArrayList<>();

    public VehicleScriptInstance(VehicleScriptContext context, VehicleExtension vehicleExtension, ParsedScript script) {
        super(context, script);
        this.vehicleExtension = vehicleExtension;
        this.carModelDrawCalls = new ArrayList<>();
    }

    public void setCarModelDrawCalls(List<VehicleModelDrawCall> calls) {
        this.carModelDrawCalls.clear();
        this.carModelDrawCalls.addAll(calls);
    }

    public void setAnnounceSoundCalls(List<SoundCall> calls) {
        this.announceSoundCalls.clear();
        this.announceSoundCalls.addAll(calls);
    }

    public void setCarSoundCalls(List<SoundCall> calls) {
        this.carSoundCalls.clear();
        this.carSoundCalls.addAll(calls);
    }

    public boolean isDead() {
        return !MinecraftClientData.getInstance().vehicles.contains(vehicleExtension);
    }
}
