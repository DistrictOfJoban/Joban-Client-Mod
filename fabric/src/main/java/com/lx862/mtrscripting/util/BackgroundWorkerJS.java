package com.lx862.mtrscripting.util;

 import com.lx862.mtrscripting.lib.org.mozilla.javascript.WrappedException;
import com.lx862.mtrscripting.mod.MTRScripting;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackgroundWorkerJS {
    private ExecutorService workerThread = Executors.newFixedThreadPool(1);

    public void submit(Runnable scriptTask) {
        workerThread.submit(() -> {
            try {
                scriptTask.run();
            } catch (Exception e) {
                Throwable underlyingException = e instanceof WrappedException ? ((WrappedException)e).getWrappedException() : e;

                if(!(underlyingException  instanceof InterruptedException)) {
                    MTRScripting.LOGGER.error("Script error while running background task!", e);
                }
            }
        });
    }

    public void reset() {
        workerThread.shutdownNow();
        workerThread = Executors.newFixedThreadPool(1);
    }
}
