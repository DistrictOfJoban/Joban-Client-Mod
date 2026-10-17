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

        /* ---- MAGIC MTR extended types (RouteInterchange.getTransportType) ---- */

        // A MAGIC override on the route resolves to its id through the entry.
        final InterchangeRouteModes magic = new InterchangeRouteModes();
        magic.add(1, "6号线|Line 6||上行", TransportMode.TRAIN, "train_metro", 0xABCDEF);
        InterchangeRouteModes.Entry entry = magic.getEntry(1, "6号线|Line 6");
        check(entry != null && entry.mode == TransportMode.TRAIN, "magic entry keeps the enum mode");
        check(entry != null && "train_metro".equals(entry.typeId()), "magic entry surfaces the extended type");

        // Two routes in one name/color group agreeing on the type keep it.
        magic.add(1, "6号线|Line 6||下行", TransportMode.TRAIN, "train_metro", 0x123456);
        entry = magic.getEntry(1, "6号线|Line 6");
        check(entry != null && "train_metro".equals(entry.typeId()), "agreeing magic routes keep the type");

        // Conflicting magic types fall back to the agreed base mode id.
        magic.add(1, "6号线|Line 6||night", TransportMode.TRAIN, "train_light_rail", 0x999999);
        entry = magic.getEntry(1, "6号线|Line 6");
        check(entry != null && entry.mode == TransportMode.TRAIN, "mode still agrees after type conflict");
        check(entry != null && "train".equals(entry.typeId()), "conflicting magic types fall back to the base id");

        // Mixed modes keep both fields unknown, exactly like before.
        magic.add(2, "环线|Circle||a", TransportMode.BOAT, "train_metro", 1);
        magic.add(2, "环线|Circle||b", TransportMode.TRAIN, "train_metro", 2);
        entry = magic.getEntry(2, "环线|Circle");
        check(entry != null && entry.mode == null, "mixed mode stays unknown with types present");
        check(entry != null && "unknown".equals(entry.typeId()), "mixed mode entry reports unknown type");

        // No MAGIC data at all: the entry's type id is the base mode id.
        final InterchangeRouteModes plain = new InterchangeRouteModes();
        plain.add(3, "轮渡|Ferry", TransportMode.BOAT);
        entry = plain.getEntry(3, "轮渡");
        check(entry != null && "boat".equals(entry.typeId()), "base mode id without magic");

        System.out.println("PASS: simplified/full names, all modes, missing and ambiguous metadata, magic types");
    }

    private static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
    }
}
