package com.lx862.jcm.mod.config;

import java.util.HashMap;

public class ConfigStorage {
    public HashMap<String, ConfigEntry<?>> configList;

    public ConfigStorage() {
        this.configList = new HashMap<>();
    }

    public <T> ConfigEntry<T> registerConfig(String id, ConfigEntry<T> entry) {
        entry.setCallback((newEntry) -> updateConfig(id, newEntry));
        this.configList.put(id, entry);
        return entry;
    }

    private void updateConfig(String id, ConfigEntry<?> newEntry) {
        this.configList.put(id, newEntry);
    }
}
