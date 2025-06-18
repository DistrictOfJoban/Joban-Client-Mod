package com.lx862.mtrscripting.core;

public abstract class ScriptContext {
    private final String name;

    public ScriptContext(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
    public abstract void reset();
}
