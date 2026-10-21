package com.lx862.mtrscripting.core.primitive;

import com.lx862.mtrscripting.lib.org.mozilla.javascript.Callable;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.Context;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.ContextFactory;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.Scriptable;

public class MTRScriptingContextFactory extends ContextFactory {
    private static final int MAX_EXECUTION_TIME = 10000;

    public static void init() {  // Change rhino global context factory
        ContextFactory.initGlobal(new MTRScriptingContextFactory());
    }

    @Override
    protected Context makeContext() {
        return new MTRScriptingContext(this);
    }

    @Override
    protected void observeInstructionCount(Context ctx, int instructionCount) {
        long startTime = ((MTRScriptingContext) ctx).startTime;
        if(startTime == -1) return;
        long currentTime = System.currentTimeMillis();
        long executionTime = (currentTime - startTime);
        if(executionTime > MAX_EXECUTION_TIME) {
            throw new RuntimeException("[MTR Scripting] Script execution exceeded maximum of " + (MAX_EXECUTION_TIME / 1000) + "s!");
        }
    }

    @Override
    protected Object doTopCall(Callable callable, Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
    {
        MTRScriptingContext mcx = (MTRScriptingContext)cx;
        mcx.startTime = System.currentTimeMillis();
        return super.doTopCall(callable, cx, scope, thisObj, args);
    }
}
