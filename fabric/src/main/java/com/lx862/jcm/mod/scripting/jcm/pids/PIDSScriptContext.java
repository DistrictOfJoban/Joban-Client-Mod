package com.lx862.jcm.mod.scripting.jcm.pids;

import com.google.gson.JsonParser;
import com.lx862.jcm.mod.JCMClient;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.mtrscripting.core.ScriptContext;

import java.util.ArrayList;
import java.util.List;

public class PIDSScriptContext extends ScriptContext {
    private final List<PIDSDrawCall> drawCalls = new ArrayList<>();

    public PIDSScriptContext(String name) {
        super(name);
    }

    public boolean debugModeEnabled() {
        return JCMClient.getConfig().debug;
    }

    public PIDSComponent parseComponent(String str) {
        return PIDSComponent.parse(new JsonParser().parse(str).getAsJsonObject());
    }

    public void draw(Object obj) {
        if(obj instanceof PIDSDrawCall) {
            ((PIDSDrawCall)obj).validate();
            this.drawCalls.add((PIDSDrawCall)obj);
        } else {
            throw new IllegalArgumentException("1st parameter is not a DrawCall!");
        }
    }

    public List<PIDSDrawCall> getDrawCalls() {
        return new ArrayList<>(this.drawCalls);
    }

    @Override
    public void reset() {
        this.drawCalls.clear();
    }
}
