package com.lx862.jcm.config;

public class ConfigEntry<T> {
    private T value;
    private String name;
    private String description;

    public ConfigEntry(T defaultValue, String name, String description) {
        this.value = defaultValue;
        this.name = name;
        this.description = description;
    }

    public T getValue() {
        return value;
    }
}
