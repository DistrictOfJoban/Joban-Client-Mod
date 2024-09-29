package com.lx862.jcm.mod.data.pids.preset.components.base;

import com.google.gson.JsonObject;

@FunctionalInterface
public interface ComponentParser {
    PIDSComponent parse(double x, double y, double width, double height, JsonObject jsonObject);
}
