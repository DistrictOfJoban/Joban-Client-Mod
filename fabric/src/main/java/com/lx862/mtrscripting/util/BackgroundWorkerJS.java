package com.lx862.mtrscripting.util;

import com.lx862.mtrscripting.mod.MTRScripting;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackgroundWorkerJS {
    private static final ExecutorService backgroundWorker = Executors.newFixedThreadPool(1);

    public static void submit(Runnable scriptTask) {
        backgroundWorker.submit(() -> {
            try {
                scriptTask.run();
            } catch (Exception e) {
                MTRScripting.LOGGER.error("Script error while running background task!", e);
            }
        });
    }
}
