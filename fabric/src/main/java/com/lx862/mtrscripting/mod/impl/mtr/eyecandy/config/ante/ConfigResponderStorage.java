package com.lx862.mtrscripting.mod.impl.mtr.eyecandy.config.ante;

import com.lx862.mtrscripting.core.annotation.ApiInternal;

import java.util.HashMap;
import java.util.Map;

@ApiInternal
@Deprecated
public class ConfigResponderStorage {
    private static final Map<String, ConfigResponder> eyecandyConfigResponders = new HashMap<>();

    public static void registerEyecandy(String modelId, ConfigResponder configResponder) {
        eyecandyConfigResponders.put(modelId, configResponder);
    }

    public static ConfigResponder getEyecandyConfigResponder(String modelId) {
        return eyecandyConfigResponders.get(modelId);
    }

    public static void reset() {
        eyecandyConfigResponders.clear();
    }
}
