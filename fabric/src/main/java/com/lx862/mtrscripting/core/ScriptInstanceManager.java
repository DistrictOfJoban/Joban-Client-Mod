package com.lx862.mtrscripting.core;

import com.lx862.mtrscripting.core.primitive.ScriptInstance;
import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.primitive.UniqueKey;
import com.lx862.mtrscripting.core.util.DataReaderJS;
import com.lx862.mtrscripting.core.util.ScriptResourceUtil;

import java.util.*;
import java.util.function.Supplier;

/**
 * Manages multiple instances of executing scripts
 */
@ApiInternal
public class ScriptInstanceManager {
    private final Map<UniqueKey, ScriptInstance> instances = new HashMap<>();
    private int elapsedTick = 0;

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



    public int tick() {
        elapsedTick++;
        int removedInstance = 0;
        for(Map.Entry<UniqueKey, ScriptInstance> entry : new HashMap<>(instances).entrySet()) {
            if(entry.getValue().shouldInvalidate()) {
                ScriptInstance<?> instance = entry.getValue();
                removedInstance++;
                instances.remove(entry.getKey());
                if(instance.isCreateFunctionInvoked()) {
                    instance.getScript().invokeDisposeFunctions(instance, null);
                }
            }
        }

        if(elapsedTick % DataReaderJS.CACHE_TIMEOUT_TICK == 0) {
            ScriptResourceUtil.resetCache();
        }
        return removedInstance;
    }

    /**
     * Dispose and clear all script instance
     */
    public void reset() {
        for(ScriptInstance<?> instance : new HashMap<>(instances).values()) {
            instance.getScript().invokeDisposeFunctions(instance, null);
        }
        instances.clear();
    }
}
