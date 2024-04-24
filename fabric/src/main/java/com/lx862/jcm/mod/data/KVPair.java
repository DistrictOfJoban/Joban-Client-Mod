package com.lx862.jcm.mod.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.HashMap;

public class KVPair {
    private final HashMap<String, Object> map;

    public KVPair() {
        this.map = new HashMap<>();
    }

    public KVPair(JsonObject jsonObject)  {
        this();
        jsonObject.entrySet().forEach(entry -> {
            Object val = null;
            JsonPrimitive jsonPrimitive = entry.getValue().getAsJsonPrimitive();
            if(jsonPrimitive.isBoolean()) {
                val = jsonPrimitive.getAsBoolean();
            }
            if(jsonPrimitive.isNumber()) {
                val = jsonPrimitive.getAsDouble();
            }
            if(jsonPrimitive.isString()) {
                val = jsonPrimitive.getAsString();
            }
            if(val != null) {
                with(entry.getKey(), val);
            }
        });
    }

    public KVPair with(String str, Object obj) {
        map.put(str, obj);
        return this;
    }

    public <T> T get(String str, T fallback) {
        return (T) map.getOrDefault(str, fallback);
    }
}
