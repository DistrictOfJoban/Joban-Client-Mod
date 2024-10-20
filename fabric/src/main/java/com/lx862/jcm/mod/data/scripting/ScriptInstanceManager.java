package com.lx862.jcm.mod.data.scripting;

import com.lx862.jcm.mod.data.pids.scripting.PIDSScriptContext;
import com.lx862.jcm.mod.data.scripting.base.ScriptInstance;
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
            instance.parsedScript.invokeCreateFunction(instance, new PIDSScriptContext(), null, () -> {});
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
                instance.parsedScript.invokeDisposeFunction(instance, new PIDSScriptContext(), null, () -> {
                    instances.remove(entry.getKey());
                });
            }
        }
        if(count > 0) {
            JCMLogger.info("[Scripting] Removed {} dead instance", count);
        }
    }

    public static void reset() {
        instances.clear();
    }
}
