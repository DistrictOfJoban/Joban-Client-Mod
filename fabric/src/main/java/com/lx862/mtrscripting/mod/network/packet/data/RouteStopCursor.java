package com.lx862.mtrscripting.mod.network.packet.data;

/**
 * Maps physical platform dwells to the depot's ordered route platforms.
 * Consecutive duplicates describe one stop, but the original index must be
 * retained for Route.getDestination(). A shared route boundary describes the
 * same dwell in both routes, including chains of one-platform routes.
 */
final class RouteStopCursor {
    private final long[][] platforms;
    private int routeIndex;
    private int platformIndex;

    RouteStopCursor(long[][] platforms) {
        this.platforms = platforms;
        skipEmptyRoutes();
    }

    boolean hasNext() {
        return routeIndex < platforms.length;
    }

    int routeIndex() {
        return routeIndex;
    }

    int platformIndex() {
        return platformIndex;
    }

    /**
     * Advance past this stop and its adjacent duplicates. Returns true only
     * when the next route starts at this same platform, so the caller should
     * reuse the current path's distance and turnback flag for that route.
     */
    boolean advance() {
        if (!hasNext()) return false;
        final long[] route = platforms[routeIndex];
        final long platformId = route[platformIndex];
        do {
            platformIndex++;
        } while (platformIndex < route.length && route[platformIndex] == platformId);
        if (platformIndex < route.length) return false;

        routeIndex++;
        platformIndex = 0;
        skipEmptyRoutes();
        return hasNext() && platforms[routeIndex][0] == platformId;
    }

    private void skipEmptyRoutes() {
        while (hasNext() && platforms[routeIndex].length == 0) routeIndex++;
    }
}
