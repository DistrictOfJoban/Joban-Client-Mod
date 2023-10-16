package com.lx862.jcm.mod.config;

import java.util.function.Consumer;

public class ConfigEntry<T> {
    private T value;
    private final String name;
    private final String description;
    private final Class<T> targetClass;
    private Consumer<ConfigEntry<T>> onSetCallback;

    public ConfigEntry(Class<T> targetClass, T defaultValue, String name, String description) {
        this.value = defaultValue;
        this.name = name;
        this.description = description;
        this.targetClass = targetClass;
    }

    public void setCallback(Consumer<ConfigEntry<T>> callback) {
        this.onSetCallback = callback;
    }

    public <U> void set(U newValue) {
        this.value = (T)newValue;
        onSetCallback.accept(this);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public T get() {
        return value;
    }
    public boolean is(Class<?> cls) {
        return targetClass == cls;
    }
}
