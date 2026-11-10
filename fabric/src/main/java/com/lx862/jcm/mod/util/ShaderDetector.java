package com.lx862.jcm.mod.util;

import java.lang.reflect.Method;

/**
 * Detects, purely by reflection (zero hard dependency, works on both
 * loaders and every MC version this mod builds for), whether a shader
 * pipeline is currently driving the world render.
 *
 * <h2>Why this exists (2026-11-10, PSD round 10)</h2>
 * The PSD backing's channel T — the no-depth-test glyph-pipeline
 * carrier that rides the SEE_THROUGH text layer — is only needed (and
 * only correct) on shader pipelines:
 *
 * <ul>
 *   <li><b>Without shaders</b>, MTR's queue flush order (by
 *       {@code QueuedRenderLayer} ordinal: {@code LIGHT} 0 → … →
 *       {@code EXTERIOR} 2 → … → {@code TEXT} 8, all in the same pass)
 *       means everything the script drew on the depth-tested queues
 *       (images/shapes on {@code LIGHT}) is flushed BEFORE the TEXT
 *       queue — and a no-depth-test full-window black rect submitted on
 *       the TEXT queue would then paint OVER those images. Carrier B
 *       (the depth-tested route-sign pipeline) alone gives the correct
 *       sandwich on every vanilla pipeline, so channel T is switched
 *       off there — script images, shapes and text keep their exact
 *       pre-round-9 layering.</li>
 *   <li><b>With Iris + a shader pack</b>, the same flush order is
 *       irrelevant for depth-tested content behind a translucent pane:
 *       the shader pipeline's restructured translucency phases discard
 *       every depth-tested quad behind the pane (nine user-tested
 *       rounds, see {@code PSDScriptRenderer#drawBackingGlyph}) — the
 *       script's own images included. What survives is the SEE_THROUGH
 *       text layer, which is exactly where channel T draws. There is
 *       nothing left for the black to cover <i>visibly</i>, and the
 *       script text (same queue, submitted later) still draws over the
 *       black.</li>
 * </ul>
 *
 * <h2>Detection</h2>
 * Iris exposes a stable v0 API ({@code net.irisshaders.iris.api.v0.IrisApi},
 * unchanged since Iris 1.2 in 2022):
 * {@code IrisApi.getInstance().isShaderPackInUse()}. The classes are
 * resolved once in the static initializer; a missing Iris, a changed
 * signature or any reflective failure degrades to {@code false}
 * (channel T off = the pre-round-9 no-shader behaviour — carrier B
 * carries the backing), never to a crash. The per-frame cost is one
 * reflective {@code Boolean} invocation, called once per
 * {@code renderAll} — {@code Boolean.valueOf} caches both values, so
 * the steady-state allocation is zero.
 *
 * <p>OptiFine's shader pipeline is deliberately NOT detected: its
 * public API is unstable and its translucency restructuring differs
 * from Iris's, so on OptiFine channel T stays off (the conservative,
 * never-covers-anything choice). If an OptiFine user ever reports the
 * shader backing missing, a round 11 can revisit.</p>
 */
public final class ShaderDetector {

    private static final Object IRIS_API;
    private static final Method IRIS_IS_SHADER_PACK_IN_USE;

    static {
        Object api = null;
        Method query = null;
        try {
            final Class<?> irisApi = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            api = irisApi.getMethod("getInstance").invoke(null);
            query = irisApi.getMethod("isShaderPackInUse");
            query.invoke(api); // fail fast here, not on the render thread every frame
        } catch (Throwable ignored) {
            api = null;
            query = null;
        }
        IRIS_API = api;
        IRIS_IS_SHADER_PACK_IN_USE = query;
    }

    /** True iff Iris is installed and a shader pack is currently in use
     *  (the "(internal)" vanilla pipeline counts as NOT in use). Safe to
     *  call every frame; degrades to {@code false} on any failure. */
    public static boolean isShaderPackInUse() {
        final Method query = IRIS_IS_SHADER_PACK_IN_USE;
        if (query == null || IRIS_API == null) return false;
        try {
            return (Boolean) query.invoke(IRIS_API);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private ShaderDetector() {
    }
}
