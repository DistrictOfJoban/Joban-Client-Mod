package com.lx862.mtrscripting.mod.network.packet.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Runs the actual production cursor with route/path edge cases. */
public final class RouteStopCursorTest {
    private static List<String> dwell(RouteStopCursor cursor) {
        List<String> positions = new ArrayList<>();
        if (!cursor.hasNext()) return positions;
        do {
            positions.add(cursor.routeIndex() + ":" + cursor.platformIndex());
        } while (cursor.advance());
        return positions;
    }

    private static void expect(RouteStopCursor cursor, String... positions) {
        List<String> actual = dwell(cursor);
        if (!actual.equals(Arrays.asList(positions))) {
            throw new AssertionError("Expected " + Arrays.toString(positions) + ", got " + actual);
        }
    }

    public static void main(String[] args) {
        // A,A,B must use original destination index 2 for B, not compact index 1.
        RouteStopCursor duplicates = new RouteStopCursor(new long[][]{{10, 10, 20, 20, 30}});
        expect(duplicates, "0:0");
        expect(duplicates, "0:2");
        expect(duplicates, "0:4");
        expect(duplicates);

        // Shared terminal is emitted again with the NEXT route's source index.
        RouteStopCursor shared = new RouteStopCursor(new long[][]{{10, 20}, {20, 20, 30}});
        expect(shared, "0:0");
        expect(shared, "0:1", "1:0");
        expect(shared, "1:2");
        expect(shared);

        // A one-stop next route must complete immediately, not overflow on the next dwell.
        RouteStopCursor single = new RouteStopCursor(new long[][]{{10, 20}, {20, 20}, {20}, {}, {20, 30}});
        expect(single, "0:0");
        expect(single, "0:1", "1:0", "2:0", "4:0");
        expect(single, "4:1");
        expect(single);

        // Empty routes produce no empty response entries; unshared routes need a new dwell.
        RouteStopCursor empty = new RouteStopCursor(new long[][]{{}, {10}, {}, {20}, {}});
        expect(empty, "1:0");
        expect(empty, "3:0");
        expect(empty);
        expect(new RouteStopCursor(new long[][]{{}, {}}));
        expect(new RouteStopCursor(new long[0][]));

        // Non-consecutive repeats are real return visits, and id zero is not a sentinel.
        RouteStopCursor loop = new RouteStopCursor(new long[][]{{0, 10, 0}});
        expect(loop, "0:0");
        expect(loop, "0:1");
        expect(loop, "0:2");
        expect(loop);
        System.out.println("RouteStopCursorTest: duplicate indices, shared/single/empty routes and return visits passed");
    }
}
