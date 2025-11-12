package com.lx862.jcm.mod.scripting.jcm.pids;

import com.google.gson.JsonParser;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.jcm.mod.scripting.mtr.MTRScriptContext;

public class PIDSScriptContext extends MTRScriptContext {

    public PIDSScriptContext(String name) {
        super(name);
    }

    public PIDSComponent parseComponent(String str) {
        return PIDSComponent.parse(new JsonParser().parse(str).getAsJsonObject());
    }

    public void draw(Object obj) {
        if(obj instanceof PIDSDrawCall) {
            ((PIDSDrawCall)obj).validate();
            renderManager().queue((PIDSDrawCall)obj);
        } else {
            throw new IllegalArgumentException("1st parameter is not a DrawCall!");
        }
    }
}
