package com.lx862.mtrscripting.mod.gui.hud;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class ScriptErrorNotifier {
    private static final Logger LOGGER = LogManager.getLogger("MTR Scripting via JCM");

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
        // Isolate every notification: one throwing callback must not block
        // the remaining notifications nor the queue cleanup.
        for (Runnable callback : queue) {
            try {
                callback.run();
            } catch (Throwable t) {
                LOGGER.error("[MTR Scripting via JCM] Error while running a script error notification!", t);
            }
        }
        queue.clear();
    }
}
