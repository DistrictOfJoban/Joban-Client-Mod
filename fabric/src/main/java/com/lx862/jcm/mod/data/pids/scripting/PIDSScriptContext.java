package com.lx862.jcm.mod.data.pids.scripting;

import com.google.gson.JsonParser;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.jcm.mod.data.scripting.base.ScriptContext;

import java.util.ArrayList;
import java.util.List;

public class PIDSScriptContext extends ScriptContext {
    private final List<PIDSComponent> drawCalls = new ArrayList<>();

    public PIDSComponent parseComponent(String str) {
        return PIDSComponent.parse(new JsonParser().parse(str).getAsJsonObject());
    }

    public void drawComponent(PIDSComponent component) {
        if(component != null) {
            this.drawCalls.add(component);
        }
    }

    public List<PIDSComponent> getDrawCalls() {
        List<PIDSComponent> drawCalls = new ArrayList<>(this.drawCalls);
        this.drawCalls.clear();
        return drawCalls;
    }
}
