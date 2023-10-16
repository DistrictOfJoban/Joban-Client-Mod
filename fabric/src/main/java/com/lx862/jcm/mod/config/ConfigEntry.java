package com.lx862.jcm.mod.config;

import java.util.function.Consumer;

public class ConfigEntry<T> {
    private T value;
    private final T defaultValue;
    private final String name;
    private final String description;
    private final Class<T> targetClass;
    private Consumer<ConfigEntry<T>> onSetCallback;

    public ConfigEntry(Class<T> targetClass, T defaultValue, String name, String description) {
        this.name = name;
        this.description = description;
        this.targetClass = targetClass;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public void setCallback(Consumer<ConfigEntry<T>> callback) {
        this.onSetCallback = callback;
    }

    public <U> void set(U newValue) {
        this.value = (T)newValue;
        onSetCallback.accept(this);
    }

    public String getTitle() {
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
    public void reset() {
        set(defaultValue);
    }
}
