package com.lx862.jcm.config;

public class ConfigEntry {
    public final String title;
    public final String description;
    private String valueString = null;
    private boolean valueBoolean = false;

    public ConfigEntry(String title, String description, String value) {
        this.title = title;
        this.description = description;
        this.valueString = value;
    }

    public ConfigEntry(String title, String description, boolean value) {
        this.title = title;
        this.description = description;
        this.valueBoolean = value;
    }

    public Object getValue() {
        if(valueString != null) return valueString;
        else return valueBoolean;
    }
}
