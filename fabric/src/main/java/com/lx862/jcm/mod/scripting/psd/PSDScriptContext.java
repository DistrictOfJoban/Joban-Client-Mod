package com.lx862.jcm.mod.scripting.psd;

import com.lx862.jcm.mod.scripting.pids.PIDSScriptContext;
import com.lx862.mtrscripting.core.annotation.ApiInternal;

/**
 * The {@code ctx} argument of PSD scripts. Extends the PIDS script context
 * so the drawing API is identical ({@code Text/Texture/Rect} + auto z-ordering
 * via {@code ctx.draw(...)}), and so the native record replay in
 * {@code NativeContentDriver.apply} recognizes it unchanged.
 */
public class PSDScriptContext extends PIDSScriptContext {
    @ApiInternal
    public PSDScriptContext(String name) {
        super(name);
    }
}
