package com.lx862.mtrscripting.core.api;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.annotation.ValueNullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class AbstractScriptContext {
    private final String name;
    private final Map<String, Object> debugInfoMap;

    public AbstractScriptContext(String name) {
        this.name = name;
        this.debugInfoMap = new HashMap<>();
    }

    public String getName() {
        return this.name;
    }

    public void setDebugInfo(String key, @ValueNullable Object value) {
        debugInfoMap.put(key, value);
    }

    @ApiInternal
    public Set<Map.Entry<String, Object>> getDebugInfo() {
        return debugInfoMap.entrySet();
    }

    @ApiInternal
    public abstract void resetForNextRun();
}
