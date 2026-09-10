package com.lx862.mtrscripting.mod.impl.mtr.eyecandy.config;

import java.util.List;

public class EyecandyCustomConfig {

    private final String description;
    private final List<Entry> entries;

    public EyecandyCustomConfig(String description, List<Entry> entries) {
        this.description = description;
        this.entries = entries;
    }

    public String description() {
        return this.description;
    }

    public List<Entry> entries() {
        return this.entries;
    }

    public record Entry(String id, String name, Type type, String validation, boolean optional) {
        public enum Type {
            STRING,
            INTEGER,
            DOUBLE,
            BOOLEAN
        }
    }
}
