package com.lx862.mtrscripting.core.api;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.annotation.ValueNullable;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public abstract class AbstractScriptContext {
    private final String name;
    private final Map<String, Object> debugInfoMap;
    private final Set<AutoCloseable> disposable;
    private int keylessDebugInfoCounter = 1;

    public AbstractScriptContext(String name) {
        this.name = name;
        this.debugInfoMap = new LinkedHashMap<>();
        this.disposable = new HashSet<>();
    }

    public String getName() {
        return this.name;
    }

    public void setDebugInfo(@ValueNullable Object value) {
        String key = keylessDebugInfoCounter == 1 ? "<Untitled>" : "<Untitled" + keylessDebugInfoCounter + ">";
        this.setDebugInfo(key, value);
        keylessDebugInfoCounter++;
    }

    public void setDebugInfo(String key, @ValueNullable Object value) {
        debugInfoMap.put(key, value);
    }

    @ApiInternal
    public void addDisposable(AutoCloseable autoCloseable) {
        disposable.add(autoCloseable);
    }

    @ApiInternal
    public Map<String, Object> getDebugInfo() {
        return debugInfoMap;
    }

    @ApiInternal
    public void resetForNextRun() {
        keylessDebugInfoCounter = 1;
    }

    /**
     * Dispose all objects that may leak when a reference is no longer held.
     */
    @ApiInternal
    public void dispose() {
        disposable.forEach(e -> {
            try {
                e.close();
            } catch (Exception ignored) {
            }
        });
        disposable.clear();
    }
}
