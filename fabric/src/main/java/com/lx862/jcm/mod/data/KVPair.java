package com.lx862.jcm.mod.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.mtr.mapping.holder.Identifier;

import javax.annotation.Nullable;
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
                if((double)val % 1 == 0) {
                    val = jsonPrimitive.getAsInt();
                }
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

    /**
     * Get an optional identifier
     * @param str Key
     * @return The identifier, or null if the key does not exist
     */
    public @Nullable Identifier getIdentifier(String str) {
        return map.containsKey(str) ? new Identifier((String)map.get(str)) : null;
    }
}
