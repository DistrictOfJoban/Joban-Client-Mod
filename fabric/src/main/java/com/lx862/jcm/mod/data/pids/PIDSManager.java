package com.lx862.jcm.mod.data.pids;

import com.lx862.jcm.mod.data.pids.base.BuiltinPIDSPreset;

import java.util.HashMap;

public class PIDSManager {
    public static final HashMap<String, BuiltinPIDSPreset> builtInPreset = new HashMap<>();

    static {
        builtInPreset.put("rv_pids", new RVPIDSPreset());
    }
}
