package com.lx862.mtrscripting.scripting;

import com.lx862.mtrscripting.scripting.base.ScriptInstance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ScriptInstanceManager {
    private final Map<UniqueKey, ScriptInstance> instances = new HashMap<>();

    public <T> ScriptInstance<T> getInstance(UniqueKey id, Supplier<ScriptInstance<T>> getInstance) {
        ScriptInstance<T> existingInstance = instances.get(id);
        if(existingInstance != null) {
            return existingInstance;
        }

        ScriptInstance<T> newInstance = getInstance.get();
        newInstance.parsedScripts.invokeCreateFunction(newInstance, () -> {});
        instances.put(id, newInstance);
        return newInstance;
    }

    public void clearDeadInstance() {
        int count = 0;
        for(Map.Entry<UniqueKey, ScriptInstance> entry : new HashMap<>(instances).entrySet()) {
            if(entry.getValue().isDead()) {
                ScriptInstance<?> instance = entry.getValue();
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
        for(ScriptInstance<?> instance : new HashMap<>(instances).values()) {
            instance.parsedScripts.invokeDisposeFunction(instance, () -> {});
        }
        instances.clear();
    }
}
