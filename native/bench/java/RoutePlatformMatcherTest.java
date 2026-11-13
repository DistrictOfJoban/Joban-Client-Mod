package com.lx862.mtrscripting.mod.impl.mtr.vehicle;

/** The response uses unique dwells while SimplifiedRoute retains raw platforms. */
public final class RoutePlatformMatcherTest {
    private static void expect(RoutePlatformMatcher matcher, long id, int index) {
        int actual = matcher.match(id);
        if (actual != index) throw new AssertionError("Platform " + id + ": expected " + index + ", got " + actual);
    }

    public static void main(String[] args) {
        RoutePlatformMatcher loop = new RoutePlatformMatcher(new long[]{10, 10, 20, 20, 10, 30});
        expect(loop, 10, 0);
        expect(loop, 20, 2);
        expect(loop, 10, 4);
        expect(loop, 30, 5);
        expect(loop, 10, -1);

        // A stale/missing platform must neither invent interchanges nor consume later matches.
        RoutePlatformMatcher stale = new RoutePlatformMatcher(new long[]{10, 20, 30, 20});
        expect(stale, 99, -1);
        expect(stale, 20, 1);
        expect(stale, 10, -1);
        expect(stale, 98, -1);
        expect(stale, 30, 2);
        expect(stale, 20, 3);
        expect(stale, 20, -1);
        expect(new RoutePlatformMatcher(new long[0]), 1, -1);
        expect(new RoutePlatformMatcher(new long[]{0, 0}), 0, 0);
        System.out.println("RoutePlatformMatcherTest: deduplication, forward loop matching and stale data passed");
    }
}
