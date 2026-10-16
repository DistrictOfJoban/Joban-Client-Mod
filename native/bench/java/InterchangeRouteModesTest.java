package com.lx862.mtrscripting.mod.impl.mtr.vehicle;

import org.mtr.core.data.TransportMode;

/** Run against the actual MTR TransportMode enum, without a Minecraft world. */
public final class InterchangeRouteModesTest {
    public static void main(String[] args) {
        final InterchangeRouteModes modes = new InterchangeRouteModes();
        modes.add(1, "20号线|Line 20||上行||城市", TransportMode.TRAIN);
        modes.add(1, "20号线|Line 20||下行||城市", TransportMode.TRAIN);
        check(modes.get(1, "20号线|Line 20") == TransportMode.TRAIN, "simplified name joins both directions");
        check(modes.get(1, "20号线|Line 20||上行") == TransportMode.TRAIN, "full name joins the same route");
        check(modes.get(2, "20号线|Line 20") == null, "color remains part of identity");
        check(modes.get(1, "20号线") == null, "single language separators are preserved");
        for (TransportMode mode : TransportMode.values()) {
            modes.add(10 + mode.ordinal(), "Interchange|换乘||direction", mode);
            check(modes.get(10 + mode.ordinal(), "Interchange|换乘") == mode, "correct enum " + mode);
        }
        modes.add(1, "20号线|Line 20||unknown", null);
        check(modes.get(1, "20号线|Line 20") == TransportMode.TRAIN, "missing mode cannot erase known train");
        modes.add(1, "20号线|Line 20||boat", TransportMode.BOAT);
        modes.add(1, "20号线|Line 20||train", TransportMode.TRAIN);
        check(modes.get(1, "20号线|Line 20") == null, "ambiguous mixed mode stays unknown");
        check(modes.get(999, "absent") == null, "missing metadata is never assumed TRAIN");
        System.out.println("PASS: simplified/full names, all modes, missing and ambiguous metadata");
    }

    private static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
    }
}
