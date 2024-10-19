package com.lx862.jcm.mod.data.scripting;

import com.google.gson.JsonParser;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;

public class ScriptContext {
    public PIDSComponent parseComponent(String str) {
        return PIDSComponent.parse(new JsonParser().parse(str).getAsJsonObject());
    }
}
