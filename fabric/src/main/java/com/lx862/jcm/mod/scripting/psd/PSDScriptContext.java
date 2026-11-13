package com.lx862.jcm.mod.scripting.psd;

import com.lx862.jcm.mod.scripting.pids.PIDSScriptContext;
import com.lx862.mtrscripting.core.annotation.ApiInternal;

/**
 * The {@code ctx} argument of PSD scripts. Extends the PIDS script context
 * so the drawing API is identical ({@code Text/Texture/Rect} + auto z-ordering
 * via {@code ctx.draw(...)}), and so the native record replay in
 * {@code NativeContentDriver.apply} recognizes it unchanged.
 *
 * <p>Use the positive PIDS z-order step. PIDSDrawCall converts that step
 * to a negative translation in the screen's local coordinates, matching
 * RenderDrawCall's rotation around script matrices. Local +Z points toward
 * the track on PSD surfaces, so later draws move toward the platform and
 * stay in front of their background. Negating the step here would negate
 * it twice and place later text behind the solid panel.</p>
 */
public class PSDScriptContext extends PIDSScriptContext {
    @ApiInternal
    public PSDScriptContext(String name) {
        super(name);
        setZOrderStep(0.0002);
    }
}
