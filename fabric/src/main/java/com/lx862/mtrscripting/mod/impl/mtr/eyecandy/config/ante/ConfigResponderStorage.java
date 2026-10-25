package com.lx862.mtrscripting.mod.impl.mtr.eyecandy.config.ante;

import com.lx862.mtrscripting.core.annotation.ApiInternal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApiInternal
@Deprecated
public class ConfigResponderStorage {
    private static final Map<String, List<ConfigResponder>> eyecandyConfigResponders = new HashMap<>();

    public static void registerEyecandy(String modelId, ConfigResponder configResponder) {
        eyecandyConfigResponders.computeIfAbsent(modelId, k -> new ArrayList<>()).add(configResponder);
    }

    public static List<ConfigResponder> getEyecandyConfigResponders(String modelId) {
        return eyecandyConfigResponders.get(modelId);
    }

    public static void reset() {
        eyecandyConfigResponders.clear();
    }
}
