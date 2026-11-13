package com.lx862.jcm.test;

import com.lx862.jcm.mod.util.ShaderDetector;
import org.mtr.mapping.render.shader.ModShaderHandler;

/** Drives the production optional-API adapter with changing live pass state. */
public final class ShaderPassTest {
    private static Class<?> api;
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static void set(String name, boolean value) throws Exception {
        api.getField(name).setBoolean(null, value);
    }
    public static void main(String[] args) throws Exception {
        String scenario = args[0];
        boolean available = !scenario.equals("absent") && !scenario.equals("broken_factory");
        if (!scenario.equals("absent")) api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
        boolean hasPack = available && !scenario.equals("no_pack");
        boolean hasShadow = available && !scenario.equals("no_shadow");

        if (hasPack) {
            set("pack", true);
            check(ShaderDetector.isShaderPackInUse(), "pack query missing");
            set("pack", false);
            check(!ShaderDetector.isShaderPackInUse(), "pack state was cached");
            set("failPack", true);
            check(!ShaderDetector.isShaderPackInUse(), "failed pack query must degrade to false");
        } else {
            check(!ShaderDetector.isShaderPackInUse(), "unavailable pack query must degrade to false");
        }
        if (hasShadow) {
            // Deliberately oppose the fallback's state, as occurs when a pass
            // cache still holds the previous shadow/camera invocation.
            ModShaderHandler.liveShadow = false;
            set("shadow", true);
            check(ShaderDetector.isRenderingShadowPass(), "live Iris shadow pass lost to stale false");
            ModShaderHandler.liveShadow = true;
            set("shadow", false);
            check(!ShaderDetector.isRenderingShadowPass(), "camera pass lost to stale true");
            check(ModShaderHandler.calls == 0, "fallback used despite working shadow API");
            // A failed shader-pack query cannot disable a working pass query.
            set("shadow", true);
            check(ShaderDetector.isRenderingShadowPass(), "independent method resolution lost shadow query");
            set("failShadow", true);
        }
        ModShaderHandler.liveShadow = true;
        check(ShaderDetector.isRenderingShadowPass(), "unknown pass must use live fallback, not false");
        ModShaderHandler.liveShadow = false;
        check(!ShaderDetector.isRenderingShadowPass(), "fallback state was cached");
        check(ModShaderHandler.calls == 2, "fallback must query the current pass each time");
        if (hasShadow) {
            set("failShadow", false);
            set("shadow", true);
            check(ShaderDetector.isRenderingShadowPass(), "transient failure permanently disabled Iris query");
            check(ModShaderHandler.calls == 2, "recovered API still uses fallback");
        }
        System.out.println("Shader pass adapter: " + scenario + " passed");
    }
}
