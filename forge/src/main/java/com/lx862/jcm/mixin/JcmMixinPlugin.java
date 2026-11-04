package com.lx862.jcm.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

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
 * {@code @Shadow}s {@code CACHED_RESOURCES} /
 * {@code canFetchCache} and injects into {@code getData} /
 * {@code tick} of {@code org.mtr.mod.resource.CachedResource}.
 * {@code @Shadow} matching requires the field <b>descriptor</b> to
 * match, not just the name: official MTR 4.0.x declares
 * {@code ObjectArrayList<CachedResource<?>> CACHED_RESOURCES}, while
 * some custom MTR distributions (e.g. older MTR-Yanyang builds) ship a
 * {@code Set}-backed rewrite. The mixin then fails during APPLY and
 * hard-crashes the client at startup
 * ({@code InvalidMixinException: @Shadow field CACHED_RESOURCES was not
 * located in the target class}).</p>
 *
 * <p>This plugin inspects the raw (un-transformed) target class bytes
 * before the mixin is applied and skips the guarded mixins when the
 * expected members are missing or the {@code CACHED_RESOURCES} field
 * descriptor is not the official
 * {@code Lorg/mtr/libraries/it/unimi/dsi/fastutil/objects/ObjectArrayList;}
 * — i.e. an {@code ObjectArrayList} from MTR's shaded fastutil. Skipping
 * only loses the optional memory-leak patch — the game stays fully
 * playable. When the class bytes cannot be read at all the plugin fails
 * open (keeps upstream behavior), so official MTR builds are completely
 * unaffected.</p>
 */
public class JcmMixinPlugin implements IMixinConfigPlugin {

    /** Descriptor of CachedResource.CACHED_RESOURCES on official MTR 4.0.x. */
    private static final String CACHED_RESOURCES_FIELD_NAME = "CACHED_RESOURCES";
    private static final String CACHED_RESOURCES_FIELD_DESC = "Lorg/mtr/libraries/it/unimi/dsi/fastutil/objects/ObjectArrayList;";

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

    // No @Override on the apply callbacks on purpose: which of the two
    // signatures matches the interface depends on the SpongeMixin version
    // on the compile classpath, and an @Override on the non-matching
    // overload would fail that build leg. Interface conformance is
    // structural at runtime, the annotation is not required.
    public void preApply(IMixinInfo info) {
    }

    public void postApply(IMixinInfo info) {
    }

    // SpongeMixin < 0.8.5 (the loaders of MC 1.17.1 – 1.19.4) declares the
    // apply callbacks with four arguments and the un-shaded ASM ClassNode.
    // Defining them as overloads lets this one class satisfy whichever
    // interface variant the target loader ships; the unused overload is
    // never invoked (its empty body never resolves ClassNode at runtime).
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
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
                    + ": this MTR build does not ship the official 4.0.x CachedResource layout"
                    + " (CACHED_RESOURCES as shaded ObjectArrayList, canFetchCache, getData, tick)."
                    + " The upstream CachedResource memory-leak patch does not apply to this MTR"
                    + " build; everything else keeps working. (Seen with third-party MTR"
                    + " distributions such as older MTR-Yanyang builds.)");
        }
        return cachedResourceModern;
    }

    private boolean probeCachedResourceLayout(String targetClassName) {
        final byte[] classBytes;
        try {
            classBytes = readRawClassBytes(targetClassName);
        } catch (Throwable t) {
            System.err.println("[JCM] CachedResource layout probe failed (" + t
                    + "); assuming official MTR layout.");
            return true; // fail open — official MTR path
        }
        if (classBytes == null) {
            System.err.println("[JCM] Could not read " + targetClassName
                    + " bytecode before mixin apply; assuming official MTR layout.");
            return true; // fail open — official MTR path
        }
        final boolean namesPresent = hasUtf8Constant(classBytes, CACHED_RESOURCES_FIELD_NAME)
                && hasUtf8Constant(classBytes, "canFetchCache")
                && hasUtf8Constant(classBytes, "expiry")
                && hasUtf8Constant(classBytes, "getData")
                && hasUtf8Constant(classBytes, "tick");
        if (!namesPresent) {
            return false;
        }
        // Names alone are not enough: @Shadow also matches the field
        // descriptor. A Set-backed CACHED_RESOURCES rewrite (MTR-Yanyang
        // dd51ae10) carries all five member names but a different field
        // type, which still crashes the mixin APPLY. Parse the real field
        // table when possible; if the parse is impossible for an exotic
        // class file, fall back to the name-based verdict.
        final Boolean descriptorOk = cachedResourcesFieldHasDescriptor(classBytes);
        return descriptorOk == null ? namesPresent : descriptorOk;
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
     * Minimal constant-pool + field-table walk that returns whether the
     * class declares a field named {@code CACHED_RESOURCES} with the
     * official ObjectArrayList descriptor. Returns {@code null} when the
     * class file cannot be parsed (unknown constant pool tag, truncated
     * data, ...); callers then fall back to the name-based heuristic.
     */
    private static Boolean cachedResourcesFieldHasDescriptor(byte[] classBytes) {
        try {
            int pos = 8;
            final int cpCount = u2(classBytes, pos);
            pos += 2;
            final String[] utf8 = new String[cpCount];
            for (int i = 1; i < cpCount; i++) {
                final int tag = classBytes[pos++] & 0xFF;
                switch (tag) {
                    case 1: { // CONSTANT_Utf8
                        final int len = u2(classBytes, pos);
                        pos += 2;
                        utf8[i] = new String(classBytes, pos, len, StandardCharsets.US_ASCII);
                        pos += len;
                        break;
                    }
                    case 7: case 8: case 16: case 19: case 20: // 2-byte entries
                        pos += 2;
                        break;
                    case 15: // CONSTANT_MethodHandle
                        pos += 3;
                        break;
                    case 3: case 4: case 9: case 10: case 11: case 12: case 17: case 18: // 4-byte entries
                        pos += 4;
                        break;
                    case 5: case 6: // 8-byte entries, take two pool slots
                        pos += 8;
                        i++;
                        break;
                    default:
                        return null; // unknown tag — let the caller fall back
                }
            }
            pos += 6; // access_flags, this_class, super_class
            final int interfaceCount = u2(classBytes, pos);
            pos += 2 + 2 * interfaceCount;
            // Per the JVM spec the class-level attribute table comes AFTER the
            // method table, so the field table starts right here.
            final int fieldCount = u2(classBytes, pos);
            pos += 2;
            for (int i = 0; i < fieldCount; i++) {
                pos += 2; // access_flags
                final int nameIdx = u2(classBytes, pos);
                final int descIdx = u2(classBytes, pos + 2);
                pos += 4;
                final String name = nameIdx > 0 && nameIdx < cpCount ? utf8[nameIdx] : null;
                final String desc = descIdx > 0 && descIdx < cpCount ? utf8[descIdx] : null;
                if (CACHED_RESOURCES_FIELD_NAME.equals(name)) {
                    return CACHED_RESOURCES_FIELD_DESC.equals(desc);
                }
                pos = skipAttributes(classBytes, pos);
            }
            return false; // no CACHED_RESOURCES field at all
        } catch (Throwable t) {
            return null; // unreadable structure — fall back to names
        }
    }

    /** Skips an attributes table (count already consumed); returns the new offset. */
    private static int skipAttributes(byte[] classBytes, int pos) {
        final int count = u2(classBytes, pos);
        pos += 2;
        for (int i = 0; i < count; i++) {
            pos += 2; // attribute_name_index
            final int len = u4(classBytes, pos);
            pos += 4 + len;
        }
        return pos;
    }

    private static int u2(byte[] b, int off) {
        return ((b[off] & 0xFF) << 8) | (b[off + 1] & 0xFF);
    }

    private static int u4(byte[] b, int off) {
        return ((b[off] & 0xFF) << 24) | ((b[off + 1] & 0xFF) << 16) | ((b[off + 2] & 0xFF) << 8) | (b[off + 3] & 0xFF);
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
