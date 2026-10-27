package com.lx862.mtrscripting.core.rhinojs;

import com.lx862.mtrscripting.lib.org.mozilla.javascript.Context;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.ContextFactory;

public class MTRRhinoContext extends Context {
    long startTime = -1;

    public MTRRhinoContext(ContextFactory contextFactory) {
        super(contextFactory);
        setInstructionObserverThreshold(10000);
    }
}
