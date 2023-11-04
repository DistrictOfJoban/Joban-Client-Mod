package com.lx862.jcm.mod.data.pids;

import com.lx862.jcm.mod.data.pids.base.PIDSPresetBase;

import java.util.HashMap;

public class PIDSManager {
    public static final HashMap<String, PIDSPresetBase> presetList = new HashMap<>();

    static {
        presetList.put("rv_pids", new RVPIDSPreset());
    }
}
