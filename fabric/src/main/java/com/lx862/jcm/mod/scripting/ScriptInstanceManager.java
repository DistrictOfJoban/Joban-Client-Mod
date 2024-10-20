package com.lx862.jcm.mod.scripting;

import com.lx862.jcm.mod.scripting.base.ScriptInstance;
import com.lx862.jcm.mod.util.JCMLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ScriptInstanceManager {
    private final static Map<Long, ScriptInstance> instances = new HashMap<>();

    public static ScriptInstance getInstance(long uuid, Supplier<ScriptInstance> getInstance) {
        if(instances.containsKey(uuid)) {
            return instances.get(uuid);
        } else {
            ScriptInstance instance = getInstance.get();
            instance.parsedScripts.invokeCreateFunction(instance, () -> {});
            instances.put(uuid, instance);
            return instance;
        }
    }

    public static void clearDeadInstance() {
        int count = 0;
        for(Map.Entry<Long, ScriptInstance> entry : new HashMap<>(instances).entrySet()) {
            if(entry.getValue().isDead()) {
                ScriptInstance instance = entry.getValue();
                count++;
                instance.parsedScripts.invokeDisposeFunction(instance, () -> {
                    instances.remove(entry.getKey());
                });
            }
        }
        if(count > 0) {
            JCMLogger.debug("[Scripting] Removed {} dead instance", count);
        }
    }

    public static void reset() {
        instances.clear();
    }
}
