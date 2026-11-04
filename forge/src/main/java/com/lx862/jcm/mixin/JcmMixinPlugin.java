package com.lx862.jcm.mixin;

import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/**
 * Mixin config plugin that guards JCM's MTR patching mixins against
 * third-party MTR builds whose internal class layout differs from the
 * official Minecraft Transit Railway release JCM is compiled against.
 *
 * <p>Motivation: upstream JCM 2.3.0-prerelease.3 added
 * {@code mtrpatch.CachedResourceMixin} (memory-leak fix) which
 * {@code @Shadow}s {@code CACHED_RESOURCES} / {@code canFetchCache} and
 * injects into {@code getData} / {@code tick} of
 * {@code org.mtr.mod.resource.CachedResource}. Custom MTR distributions
 * (e.g. the MTR-Yanyang server build) ship an older CachedResource
 * layout without those members, which makes the mixin fail during APPLY
 * and hard-crashes the client at startup
 * ({@code InvalidMixinException: @Shadow field CACHED_RESOURCES was not
 * located in the target class}).</p>
 *
 * <p>This plugin inspects the raw (un-transformed) target class bytes
 * before the mixin is applied and skips the guarded mixins when the
 * expected members are missing. Skipping only loses the optional
 * memory-leak patch — the game stays fully playable. When the class
 * cannot be inspected the plugin fails open (keeps upstream behavior),
 * so official MTR builds are completely unaffected.</p>
 */
public class JcmMixinPlugin implements IMixinConfigPlugin {

    /** Result of the CachedResource layout probe, cached across calls. */
    private Boolean cachedResourceModern;

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("mtrpatch.CachedResourceMixin")
                || mixinClassName.endsWith("mtrpatch.CachedResourceAccessorMixin")) {
            return shouldApplyCachedResourcePatch(mixinClassName, targetClassName);
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(org.spongepowered.asm.mixin.extensibility.IMixinInfo info) {
    }

    @Override
    public void postApply(org.spongepowered.asm.mixin.extensibility.IMixinInfo info) {
    }

    /**
     * Checks that {@code org.mtr.mod.resource.CachedResource} carries the
     * members the patch mixins reference. Runs once per config; the
     * verdict is cached.
     */
    private boolean shouldApplyCachedResourcePatch(String mixinClassName, String targetClassName) {
        if (cachedResourceModern == null) {
            cachedResourceModern = probeCachedResourceLayout(targetClassName);
        }
        if (!cachedResourceModern) {
            // System.err on purpose: the mixin bootstrap runs before mod
            // logging is guaranteed to be wired up, and this warning must
            // be visible in latest.log no matter what.
            System.err.println("[JCM] Skipped " + shortName(mixinClassName)
                    + " on " + targetClassName
                    + ": this MTR build does not ship the modern CachedResource layout"
                    + " (CACHED_RESOURCES/canFetchCache/tick). The upstream CachedResource"
                    + " memory-leak patch does not apply to this MTR build; everything else"
                    + " keeps working. (Seen with third-party MTR distributions such as"
                    + " MTR-Yanyang.)");
        }
        return cachedResourceModern;
    }

    private boolean probeCachedResourceLayout(String targetClassName) {
        try {
            final byte[] classBytes = readRawClassBytes(targetClassName);
            if (classBytes == null) {
                System.err.println("[JCM] Could not read " + targetClassName
                        + " bytecode before mixin apply; assuming official MTR layout.");
                return true; // fail open — official MTR path
            }
            return hasUtf8Constant(classBytes, "CACHED_RESOURCES")
                    && hasUtf8Constant(classBytes, "canFetchCache")
                    && hasUtf8Constant(classBytes, "expiry")
                    && hasUtf8Constant(classBytes, "getData")
                    && hasUtf8Constant(classBytes, "tick");
        } catch (Throwable t) {
            System.err.println("[JCM] CachedResource layout probe failed (" + t
                    + "); assuming official MTR layout.");
            return true; // fail open — official MTR path
        }
    }

    /**
     * Reads the raw class file through the classloader's resource lookup.
     * Resource access deliberately bypasses class loading, so the returned
     * bytes are the un-transformed contents of the MTR jar (mixin
     * transformations only happen at defineClass time).
     */
    private static byte[] readRawClassBytes(String className) throws IOException {
        final String resource = className.replace('.', '/') + ".class";
        try (InputStream is = JcmMixinPlugin.class.getClassLoader().getResourceAsStream(resource)) {
            if (is == null) {
                return null;
            }
            return is.readAllBytes();
        }
    }

    /**
     * Detects a {@code CONSTANT_Utf8_info} pool entry equal to {@code name}
     * without a real class-file parser. Every field, method and (most)
     * descriptor name the compiler emits ends up as such a constant, so an
     * exact length-prefixed match is a reliable membership test.
     */
    private static boolean hasUtf8Constant(byte[] classBytes, String name) {
        final byte[] nameBytes = name.getBytes(StandardCharsets.US_ASCII);
        if (nameBytes.length == 0 || nameBytes.length > 255) {
            return false;
        }
        final int limit = classBytes.length - 3 - nameBytes.length;
        outer:
        for (int i = 0; i < limit; i++) {
            if (classBytes[i] != 0x01 // CONSTANT_Utf8 tag
                    || classBytes[i + 1] != 0 // length high byte (names < 256 bytes)
                    || (classBytes[i + 2] & 0xFF) != nameBytes.length) {
                continue;
            }
            for (int j = 0; j < nameBytes.length; j++) {
                if (classBytes[i + 3 + j] != nameBytes[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    private static String shortName(String className) {
        final int idx = className.lastIndexOf('.');
        return idx < 0 ? className : className.substring(idx + 1);
    }
}
