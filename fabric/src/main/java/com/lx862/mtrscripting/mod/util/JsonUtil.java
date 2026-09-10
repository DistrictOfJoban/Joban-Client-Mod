package com.lx862.mtrscripting.mod.util;

import com.google.gson.JsonObject;

public class JsonUtil {
    public static String getString(String key, JsonObject jsonObject) {
        assertExist(key, jsonObject);
        return jsonObject.get(key).getAsString();
    }

    public static String getString(String key, String fallbackValue, JsonObject jsonObject) {
        return jsonObject.has(key) ? jsonObject.get(key).getAsString() : fallbackValue;
    }

    public static boolean getBoolean(String key, JsonObject jsonObject) {
        assertExist(key, jsonObject);
        return jsonObject.get(key).getAsBoolean();
    }

    public static boolean getBoolean(String key, boolean fallbackValue, JsonObject jsonObject) {
        return jsonObject.has(key) ? jsonObject.get(key).getAsBoolean() : fallbackValue;
    }

    private static void assertExist(String key, JsonObject jsonObject) {
        if(!jsonObject.has(key)) {
            throw new IllegalStateException(String.format("Expected field \"%s\" but is missing!", key));
        }
    }
}
