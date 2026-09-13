package com.lx862.mtrscripting.mod.resource;

import com.google.gson.JsonObject;

public interface ScriptResourceProvider<T> {

    void parseCustom();

    void parseMtrResources(JsonObject rootElement, boolean isLegacyResource);

    void validate();

    void reset();

    T getScriptEntry(String id);
}
