package com.lx862.mtrscripting.mod.gui;

import java.util.ArrayList;
import java.util.List;

public class ScriptErrorNotifier {
    private final List<Runnable> queue = new ArrayList<>();

    public void queue(Runnable callback) {
        queue.add(callback);
    }

    /**
     * Forget all previous notifications
     */
    public void reset() {
        queue.clear();
    }

    /**
     * Called when timing is appropriate to begin notify. (i.e. Player is in a world, thus they can be notified)
     */
    public void flush() {
        queue.forEach(Runnable::run);
        queue.clear();
    }
}
