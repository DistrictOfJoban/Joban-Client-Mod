package com.lx862.mtrscripting;

import com.lx862.mtrscripting.core.ScriptInstance;
import com.lx862.mtrscripting.data.UniqueKey;

import java.util.*;
import java.util.function.Supplier;

/**
 * Manages multiple instances of executing scripts
 */
public class ScriptInstanceManager {
    private final Map<UniqueKey, ScriptInstance> instances = new HashMap<>();

    public <T> ScriptInstance<T> getInstance(UniqueKey id, Supplier<ScriptInstance<T>> getInstance) {
        ScriptInstance<T> existingInstance = getInstance(id);
        if(existingInstance != null) {
            return existingInstance;
        }

        ScriptInstance<T> newInstance = getInstance.get();
        instances.put(id, newInstance);
        return newInstance;
    }

    public <T> ScriptInstance<T> getInstance(UniqueKey id) {
        return instances.get(id);
    }

    public Map<UniqueKey, ScriptInstance> getInstances() {
        return new HashMap<>(this.instances);
    }



    public int clearDeadInstance() {
        int count = 0;
        for(Map.Entry<UniqueKey, ScriptInstance> entry : new HashMap<>(instances).entrySet()) {
            if(entry.getValue().shouldInvalidate()) {
                ScriptInstance<?> instance = entry.getValue();
                count++;
                instances.remove(entry.getKey());
                if(instance.isCreateFunctionInvoked()) {
                    instance.getScript().invokeDisposeFunctions(instance, () -> {});
                }
            }
        }
        return count;
    }

    /**
     * Dispose and clear all script instance
     */
    public void reset() {
        for(ScriptInstance<?> instance : new HashMap<>(instances).values()) {
            instance.getScript().invokeDisposeFunctions(instance, () -> {});
        }
        instances.clear();
    }
}
