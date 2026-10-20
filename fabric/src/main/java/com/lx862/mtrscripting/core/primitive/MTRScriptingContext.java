package com.lx862.mtrscripting.core.primitive;

import com.lx862.mtrscripting.lib.org.mozilla.javascript.Context;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.ContextFactory;

public class MTRScriptingContext extends Context {
    long startTime;

    public MTRScriptingContext(ContextFactory contextFactory) {
        super(contextFactory);
        setInstructionObserverThreshold(10000);
    }
}
