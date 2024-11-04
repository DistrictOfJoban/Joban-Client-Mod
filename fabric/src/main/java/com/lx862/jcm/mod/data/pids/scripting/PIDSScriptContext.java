package com.lx862.jcm.mod.data.pids.scripting;

import com.google.gson.JsonParser;
import com.lx862.jcm.mod.data.JCMClientStats;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.mtrscripting.scripting.base.ScriptContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.lx862.jcm.mod.data.pids.preset.components.base.TextComponent.SWITCH_LANG_DURATION;

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

    public String cycleString(String mtrString) {
        List<String> split = new ArrayList<>(Arrays.asList(mtrString.split("\\|")));
        if(split.isEmpty()) return "";

        return split.get(((int) JCMClientStats.getGameTick() / SWITCH_LANG_DURATION) % split.size());
    }

    public List<DrawCall> getDrawCalls() {
        List<DrawCall> drawCalls = new ArrayList<>(this.drawCalls);
        this.drawCalls.clear();
        return drawCalls;
    }
}
