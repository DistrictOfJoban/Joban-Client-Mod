package com.lx862.jcm.mod.util;

import org.mtr.mapping.render.shader.ModShaderHandler;

import java.lang.reflect.Method;

/**
 * Optional Iris queries without a compile-time dependency on the shader mod.
 * API methods are resolved independently: an older API may expose one query
 * without the other. Only the method handles are cached; render-pass state
 * must be read at the point where rendering decisions are made.
 */
public final class ShaderDetector {

    private static final Object IRIS_API;
    private static final Method IRIS_IS_SHADER_PACK_IN_USE;
    private static final Method IRIS_IS_RENDERING_SHADOW_PASS;

    static {
        Object api = null;
        Class<?> apiClass = null;
        try {
            apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            api = apiClass.getMethod("getInstance").invoke(null);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            apiClass = null;
        }
        IRIS_API = api;
        IRIS_IS_SHADER_PACK_IN_USE = resolve(apiClass, "isShaderPackInUse");
        IRIS_IS_RENDERING_SHADOW_PASS = resolve(apiClass, "isRenderingShadowPass");
    }

    private static Method resolve(Class<?> apiClass, String name) {
        if (apiClass == null) return null;
        try {
            return apiClass.getMethod(name);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    /** Iris's currently enabled shader-pack state; false if unavailable. */
    public static boolean isShaderPackInUse() {
        final Boolean result = query(IRIS_IS_SHADER_PACK_IN_USE);
        return result != null && result;
    }

    /**
     * The current render pass, never the previous MainRenderer invocation's
     * cached value. Separate HEAD mixins may execute in either order, so a
     * pass gate cannot rely on OptimizedRenderer's JCM-cached shadow flag.
     * If Iris's query is unavailable or fails, ask MTR's live detector
     * directly rather than treating an unknown pass as a camera pass.
     */
    public static boolean isRenderingShadowPass() {
        final Boolean result = query(IRIS_IS_RENDERING_SHADOW_PASS);
        return result != null ? result : ModShaderHandler.renderingShadows();
    }

    private static Boolean query(Method method) {
        if (method == null || IRIS_API == null) return null;
        try {
            return (Boolean) method.invoke(IRIS_API);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    private ShaderDetector() {
    }
}
