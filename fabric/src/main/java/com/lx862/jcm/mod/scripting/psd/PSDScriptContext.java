package com.lx862.jcm.mod.scripting.psd;

import com.lx862.jcm.mod.scripting.pids.PIDSScriptContext;
import com.lx862.mtrscripting.core.annotation.ApiInternal;

/**
 * The {@code ctx} argument of PSD scripts. Extends the PIDS script context
 * so the drawing API is identical ({@code Text/Texture/Rect} + auto z-ordering
 * via {@code ctx.draw(...)}), and so the native record replay in
 * {@code NativeContentDriver.apply} recognizes it unchanged.
 *
 * <p>Yanyang: the z-order step is NEGATED. In the PIDS block canvas frame the
 * +z axis points toward the viewer, so {@code ctx.draw}'s auto z-ordering
 * (later calls offset by {@code +n * step}) correctly layers later draws in
 * front. The PSD canvas frames (glass sandwich, psd_top band — see
 * {@code PSDScriptRenderer}) use MTR's RenderRouteBase convention where +z
 * points toward the TRACK, away from the platform viewer; a positive step
 * there would hide every later draw behind the earlier ones (text behind its
 * background). Negating the step keeps the documented "later draws on top"
 * behavior on PSD surfaces.</p>
 */
public class PSDScriptContext extends PIDSScriptContext {
    @ApiInternal
    public PSDScriptContext(String name) {
        super(name);
        setZOrderStep(-0.0002);
    }
}
