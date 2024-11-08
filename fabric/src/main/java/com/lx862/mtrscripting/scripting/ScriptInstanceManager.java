package com.lx862.mtrscripting.scripting;

import com.lx862.mtrscripting.scripting.base.ScriptInstance;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ScriptInstanceManager {
    private final Map<Long, ScriptInstance> instances = new HashMap<>();

    public <T> ScriptInstance<T> getInstance(String id, long position, Supplier<ScriptInstance<T>> getInstance) {
        ScriptInstance<T> existingInstance = instances.get(position);
        if(existingInstance != null) {
            if(!existingInstance.id.equals(id)) {
                existingInstance.parsedScripts.invokeDisposeFunction(existingInstance, () -> {});
                instances.remove(position);
            } else {
                return existingInstance;
            }
        }

        ScriptInstance<T> newInstance = getInstance.get();
        newInstance.parsedScripts.invokeCreateFunction(newInstance, () -> {});
        instances.put(position, newInstance);
        return newInstance;
    }

    public void clearDeadInstance() {
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
            ScriptManager.LOGGER.debug("[Scripting] Removed {} dead instance", count);
        }
    }

    public void reset() {
        for(Map.Entry<Long, ScriptInstance> entry : new HashMap<>(instances).entrySet()) {
            ScriptInstance instance = entry.getValue();
            instance.parsedScripts.invokeDisposeFunction(instance, () -> {});
        }
        instances.clear();
    }
}
