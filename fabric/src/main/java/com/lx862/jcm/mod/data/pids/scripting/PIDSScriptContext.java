package com.lx862.jcm.mod.data.pids.scripting;

import com.google.gson.JsonParser;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.mtrscripting.scripting.base.ScriptContext;

import java.util.ArrayList;
import java.util.List;

public class PIDSScriptContext extends ScriptContext {
    private final List<DrawCall> drawCalls = new ArrayList<>();

    public PIDSComponent parseComponent(String str) {
        return PIDSComponent.parse(new JsonParser().parse(str).getAsJsonObject());
    }

    public void draw(Object obj) {
        if(obj instanceof DrawCall) {
            ((DrawCall)obj).validate();
            this.drawCalls.add((DrawCall)obj);
        } else {
            throw new IllegalArgumentException("1st parameter is not a DrawCall!");
        }
    }

    public List<DrawCall> getDrawCalls() {
        List<DrawCall> drawCalls = new ArrayList<>(this.drawCalls);
        this.drawCalls.clear();
        return drawCalls;
    }
}
