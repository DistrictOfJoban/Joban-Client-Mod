package com.lx862.jcm.mod.data.pids;

import com.lx862.jcm.mod.data.pids.base.PIDSPreset;

import java.util.HashMap;

public class PIDSManager {
    public static final HashMap<String, PIDSPreset> presetList = new HashMap<>();

    static {
        presetList.put("rv_pids", new RVPIDSPreset());
    }
}
