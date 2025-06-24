package com.lx862.mtrscripting.core;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.util.Map;

public abstract class ScriptContext {
    private final String name;
    private final Object2ObjectArrayMap<String, Object> debugInfo;

    public ScriptContext(String name) {
        this.name = name;
        this.debugInfo = new Object2ObjectArrayMap<>();
    }

    public String getName() {
        return this.name;
    }

    public void setDebugInfo(String key, Object value) {
        debugInfo.put(key, value);
    }

    public ObjectSet<Map.Entry<String, Object>> getDebugInfo() {
        return debugInfo.entrySet();
    }

    public abstract void reset();
}
