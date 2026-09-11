package com.lx862.mtrscripting.core.util;

 import com.lx862.mtrscripting.core.annotation.ApiInternal;
 import com.lx862.mtrscripting.lib.org.mozilla.javascript.WrappedException;
import com.lx862.mtrscripting.mod.MTRScriptingModClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackgroundWorkerJS {
    private ExecutorService workerThread = Executors.newFixedThreadPool(1);

    @ApiInternal
    public BackgroundWorkerJS() {
    }

    public void submit(Runnable scriptTask) {
        workerThread.submit(() -> {
            try {
                scriptTask.run();
            } catch (Exception e) {
                Throwable underlyingException = e instanceof WrappedException ? ((WrappedException)e).getWrappedException() : e;

                if(!(underlyingException  instanceof InterruptedException)) {
                    MTRScriptingModClient.LOGGER.error("Script error while running background task!", e);
                }
            }
        });
    }

    @ApiInternal
    public void reset() {
        workerThread.shutdownNow();
        workerThread = Executors.newFixedThreadPool(1);
    }
}
