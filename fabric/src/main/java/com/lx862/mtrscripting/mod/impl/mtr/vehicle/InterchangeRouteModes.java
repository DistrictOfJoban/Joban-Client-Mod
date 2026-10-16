package com.lx862.mtrscripting.mod.impl.mtr.vehicle;

import org.mtr.core.data.TransportMode;

import java.util.HashMap;
import java.util.Map;

/** Joins simplified interchange names to the original routes' transport modes. */
final class InterchangeRouteModes {
    private final Map<String, TransportMode> modes = new HashMap<>();

    private static String key(int color, String name) {
        // SimplifiedRoute.addInterchangeRoutes removes the first || suffix.
        // Keep whitespace and single | language separators exactly as MTR does.
        final int suffix = name.indexOf("||");
        return color + "\u0000" + (suffix < 0 ? name : name.substring(0, suffix));
    }

    void add(int color, String name, TransportMode mode) {
        if (name == null || mode == null) return;
        final String key = key(color, name);
        if (!modes.containsKey(key)) modes.put(key, mode);
        else if (modes.get(key) != mode) modes.put(key, null);
        // A mixed-mode name/color group has no single truthful transport mode.
        // Retain UNKNOWN rather than assuming that all interchange routes are trains.
    }

    TransportMode get(int color, String name) {
        return name == null ? null : modes.get(key(color, name));
    }
}
