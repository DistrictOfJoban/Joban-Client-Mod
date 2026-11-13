package com.lx862.mtrscripting.mod.impl.mtr.vehicle;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/** Matches compact stop data to the original route indices in travel order. */
final class RoutePlatformMatcher {
    private final Map<Long, ArrayDeque<Integer>> indices = new HashMap<>();
    private int nextIndex;

    RoutePlatformMatcher(long[] platformIds) {
        for (int i = 0; i < platformIds.length; i++) {
            // The server represents adjacent repeated platforms as one dwell.
            if (i == 0 || platformIds[i] != platformIds[i - 1]) {
                indices.computeIfAbsent(platformIds[i], ignored -> new ArrayDeque<>()).addLast(i);
            }
        }
    }

    int match(long platformId) {
        ArrayDeque<Integer> candidates = indices.get(platformId);
        if (candidates == null) return -1;
        // Retire earlier visits rather than reusing the first occurrence on a loop.
        while (!candidates.isEmpty() && candidates.peekFirst() < nextIndex) candidates.removeFirst();
        if (candidates.isEmpty()) return -1;
        int result = candidates.removeFirst();
        nextIndex = result + 1;
        return result;
    }
}
