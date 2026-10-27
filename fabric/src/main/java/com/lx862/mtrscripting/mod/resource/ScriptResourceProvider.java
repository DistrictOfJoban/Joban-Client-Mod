package com.lx862.mtrscripting.mod.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lx862.jcm.mod.config.JCMClientConfig;
import com.lx862.jcm.nativeapi.NativeScriptManager;
import com.lx862.mtrscripting.core.api.ScriptingLanguage;
import com.lx862.mtrscripting.core.integration.VanillaTextWrapper;
import com.lx862.mtrscripting.core.primitive.ParsedScript;
import com.lx862.mtrscripting.core.primitive.ScriptContent;
import com.lx862.mtrscripting.mod.MTRScriptingMod;
import com.lx862.mtrscripting.mod.MTRScriptingModClient;
import com.lx862.mtrscripting.mod.impl.mtr.MTRContentScripting;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ResourceManagerHelper;
import org.mtr.mapping.mapper.TextHelper;

import java.util.List;
import java.util.Locale;

public interface ScriptResourceProvider {

    void parseCustom();

    void parseMtrResources(JsonObject rootObject, boolean isLegacyResource);

    void validate();

    void reset();

    /**
     * Yanyang: parse one script entry, which may freely mix JavaScript and C++.
     *
     * <p>The 2.3.0-beta.2 format has no {@code language} field: every
     * {@code scriptLocations} entry is classified by the mod itself —
     * {@code *.js} (case-insensitive) is JavaScript, anything else is a C++
     * native declaration:
     * <ul>
     *   <li>{@code mtr:my_pack/natives/sp1900_train_num} — platform-agnostic
     *       stem, expanded per OS/arch (see {@code NativeScriptManager});</li>
     *   <li>{@code mtr:my_pack/natives/linux-x64/libfoo.so} — full library
     *       path, bound to the OS its extension implies;</li>
     *   <li>{@code {"windows": ..., "linux": ..., "macos": ...}} — explicit
     *       per-platform map.</li>
     * </ul>
     * Any number of both kinds may appear in one entry. The JS parts (plus
     * {@code prependExpressions} and {@code input}) concatenate into one
     * ParsedScript; the C++ parts all load under the same script id via
     * NativeScriptManager. The render mixins drive BOTH sides for mixed ids:
     * each side captures into its own replay registry, so their draw calls
     * never collide, and a missing/failed part degrades gracefully instead
     * of breaking the other.
     *
     * <p>Legacy declarations keep working: {@code "language": "cpp"} keeps
     * the old native-only entry semantics (nativeLibrary/nativeLibraries plus
     * non-.js scriptLocations; JS parts are ignored with a hint), and
     * {@code "language": "javascript"/"js"} forces every scriptLocations
     * entry down the JS path (escape hatch for extensionless JS paths).
     * {@code nativeLibrary}/{@code nativeLibraries} may also be combined
     * with .js scriptLocations entries without any language field.
     */
    static ParsedScript tryParseScript(String id, String scriptType, String contextName, JsonObject jsonObject, boolean isParsingMTR4, boolean useSnakeCase) {
        final String scriptFilesKey = isParsingMTR4 ? "scriptLocations" : useSnakeCase ? "script_files" : "scriptFiles";
        final String scriptTextsKey = isParsingMTR4 ? "prependExpressions" : useSnakeCase ? "script_texts" : "scriptTexts";
        final String scriptInputKey = isParsingMTR4 ? "input" : "scriptInput";
        final String rateLimitKey = isParsingMTR4 ? "rateLimit" : useSnakeCase ? "script_rate_limit" : "scriptRateLimit";
        final String languageKey = isParsingMTR4 ? "language" : useSnakeCase ? "script_lang" : "scriptLang";
        String scriptLang = jsonObject.has(languageKey) ? jsonObject.get(languageKey).getAsString() : ScriptingLanguage.RHINO_JS;
        double rateLimit = jsonObject.has(rateLimitKey) ? jsonObject.get(rateLimitKey).getAsDouble() : 0;

        final boolean explicitCpp = isNativeCppScript(jsonObject);
        final boolean explicitJs = !explicitCpp && isExplicitJavascript(jsonObject);
        /* Extensionless entries are native stems only under the MTR 4
           "scriptLocations" key; the legacy script_files/scriptFiles keys
           historically accepted extensionless JS paths, so only unambiguous
           native forms (.so/.dll/.dylib, per-platform objects) count there. */
        final boolean mtr4Locations = isParsingMTR4;

        /* Yanyang: the MTR 3 / NTE docs' example uses "script_input" while
           upstream's code reads "scriptInput" — accept both so packs written
           straight from the documentation keep working. */
        JsonElement inputElement = null;
        if(jsonObject.has(scriptInputKey) && !jsonObject.get(scriptInputKey).isJsonNull()) {
            inputElement = jsonObject.get(scriptInputKey);
        } else if(!isParsingMTR4 && useSnakeCase && jsonObject.has("script_input") && !jsonObject.get("script_input").isJsonNull()) {
            inputElement = jsonObject.get("script_input");
        }

        final List<ScriptContent> scripts = new ObjectArrayList<>();
        final List<JsonElement> nativeDeclarations = new ObjectArrayList<>();
        final List<ScriptContent> jsFiles = new ObjectArrayList<>();

        /* scriptLocations / script_files / scriptFiles: per-entry language
           detection — *.js is JavaScript, anything else is a C++ module. */
        if(jsonObject.has(scriptFilesKey) && !jsonObject.get(scriptFilesKey).isJsonNull()) {
            JsonArray scriptFilesArray = jsonObject.get(scriptFilesKey).getAsJsonArray();
            for(int i = 0; i < scriptFilesArray.size(); i++) {
                final JsonElement entryElement = scriptFilesArray.get(i);
                if(!explicitJs && isNativeScriptLocationEntry(entryElement, mtr4Locations)) {
                    // C++ native declaration — stem, full library path, or per-platform map
                    nativeDeclarations.add(entryElement);
                    continue;
                }
                if(explicitCpp) {
                    MTRScriptingModClient.LOGGER.warn("[MTR Scripting via JCM] Ignoring JavaScript location {} in native (language=cpp) script \"{}\" — remove the language field to mix JS and C++.", entryElement, id);
                    continue;
                }
                if(!entryElement.isJsonPrimitive()) {
                    MTRScriptingModClient.LOGGER.warn("[MTR Scripting via JCM] Unsupported non-string script location {} in script \"{}\"!", entryElement, id);
                    continue;
                }

                Identifier scriptLocationSource = new Identifier(entryElement.getAsString());
                String scriptText = ResourceManagerHelper.readResource(scriptLocationSource);
                if(scriptText.isEmpty()) {
                    MTRScriptingModClient.LOGGER.warn("[MTR Scripting via JCM] Script {}:{} is missing (or empty)!", scriptLocationSource.getNamespace(), scriptLocationSource.getPath());
                    continue;
                }

                jsFiles.add(new ScriptContent(scriptLocationSource, scriptText));
            }
        }

        /* prependExpressions / script_texts — pure JS, ignored in native-only
           (language=cpp) entries exactly like the JS locations above. */
        final boolean hasTexts = !explicitCpp && jsonObject.has(scriptTextsKey) && !jsonObject.get(scriptTextsKey).isJsonNull();

        /* Assemble the JS part in the order the JS-only pipeline used:
           SCRIPT_INPUT first, then the expressions, then the script files.
           The JS part only exists when there is actual JS code (at least one
           readable .js file or one expression) — input alone doesn't spin up
           an empty script, so a native-only entry stays native-only. */
        if(!jsFiles.isEmpty() || hasTexts) {
            if(inputElement != null) {
                Identifier scriptLocationSource = MTRScriptingMod.id("internal/script_input/" + scriptType + "/" + id);
                scripts.add(new ScriptContent(scriptLocationSource, "const SCRIPT_INPUT = " + inputElement.toString() + ";"));
            }

            if(hasTexts) {
                JsonArray scriptTextArray = jsonObject.get(scriptTextsKey).getAsJsonArray();
                for(int i = 0; i < scriptTextArray.size(); i++) {
                    Identifier scriptLocationSource = MTRScriptingMod.id("internal/script_texts/" + scriptType + "/" + id + "/line" + i);
                    scripts.add(new ScriptContent(scriptLocationSource, scriptTextArray.get(i).getAsString()));
                }
            }

            scripts.addAll(jsFiles);
        }

        /* Legacy native fields (Yanyang): nativeLibrary/nativeLibraries are
           always C++ declarations regardless of the language field, and can
           be combined with .js scriptLocations entries in the same entry. */
        collectLegacyNativeFields(jsonObject, useSnakeCase, nativeDeclarations);

        if(!nativeDeclarations.isEmpty()) {
            /* All C++ modules for this id load here (idempotent across the
               double parse pass); rendering dispatches through
               NativeScriptManager exactly like a pure native entry. */
            NativeScriptManager.configure(id, inputElement, null);
            final JsonArray merged = new JsonArray();
            for(JsonElement declaration : nativeDeclarations) {
                merged.add(declaration);
            }
            NativeScriptManager.loadManyFromDeclaration(id, merged);
        }

        try {
            ParsedScript.Configuration configuration = ParsedScript.Configuration.create(scripts, scriptLang, rateLimit);
            return scripts.isEmpty() ? null : MTRContentScripting.getScriptManager().parseScript(id + " (" + scriptType + ")", contextName, configuration, JCMClientConfig.INSTANCE.scripting.scriptDebugMode.value());
        } catch (Exception e) {
            ScriptResourceProvider.logError("parsing " + scriptType + " script (" + id + ")", e);
            return null;
        }
    }

    /** True when the entry declares "language": "cpp" (native-only mode). */
    static boolean isNativeCppScript(JsonObject jsonObject) {
        final JsonElement language = jsonObject.get("language");
        return language != null && language.isJsonPrimitive() && "cpp".equalsIgnoreCase(language.getAsString());
    }

    /** True when the entry declares "language": "javascript" or "js"
        (force every scriptLocations entry down the JS path). */
    static boolean isExplicitJavascript(JsonObject jsonObject) {
        final JsonElement language = jsonObject.get("language");
        if(language == null || !language.isJsonPrimitive()) return false;
        final String value = language.getAsString().toLowerCase(Locale.ROOT);
        return "javascript".equals(value) || "js".equals(value);
    }

    /**
     * Classify one scriptLocations entry: JavaScript or C++ native.
     *
     * <p>{@code *.js} (case-insensitive) is JavaScript. A JSON object is a
     * per-platform native map. {@code .so/.dll/.dylib} paths are native.
     * An extensionless string is a native stem under the MTR 4
     * {@code scriptLocations} key (the beta.2 packs name JS files with .js),
     * and stays JavaScript under the legacy {@code script_files}/
     * {@code scriptFiles} keys for backwards compatibility.
     */
    static boolean isNativeScriptLocationEntry(JsonElement entry, boolean mtr4LocationsKey) {
        if(entry == null || entry.isJsonNull()) return false;
        if(!entry.isJsonPrimitive()) return true; /* per-platform map {"windows": ..., ...} */
        final String lower = entry.getAsString().toLowerCase(Locale.ROOT);
        if(lower.endsWith(".js")) return false;
        if(lower.endsWith(".so") || lower.endsWith(".dll") || lower.endsWith(".dylib")) return true;
        return mtr4LocationsKey;
    }

    /** Append the legacy nativeLibrary/nativeLibraries declarations (if any)
        to the collected native declaration list, in declaration order. */
    static void collectLegacyNativeFields(JsonObject jsonObject, boolean useSnakeCase, List<JsonElement> nativeDeclarations) {
        final JsonElement many = jsonObject.get(nativeLibrariesKey(useSnakeCase));
        if(many != null && !many.isJsonNull()) {
            if(many.isJsonArray()) {
                for(JsonElement e : many.getAsJsonArray()) {
                    if(e != null && !e.isJsonNull()) nativeDeclarations.add(e);
                }
            } else {
                nativeDeclarations.add(many);
            }
            return;
        }
        final JsonElement single = jsonObject.get(nativeLibraryKey(useSnakeCase));
        if(single != null && !single.isJsonNull()) {
            nativeDeclarations.add(single);
        }
    }

    /** nativeLibrary field name: "nativeLibrary" (MTR 4 / NTE) or
        "native_library" (snake_case legacy entries). */
    static String nativeLibraryKey(boolean useSnakeCase) {
        return useSnakeCase ? "native_library" : "nativeLibrary";
    }

    /** Multi-library field name: "nativeLibraries" (MTR 4) or
        "native_libraries" (snake_case legacy entries).
        Yanyang extension: MTR's vehicle schema carries ONE scriptId, so a
        pack that needs several C++ libraries for one train lists them here
        and they all run under that id, in declaration order. */
    static String nativeLibrariesKey(boolean useSnakeCase) {
        return useSnakeCase ? "native_libraries" : "nativeLibraries";
    }

    static void logError(String action, Exception e) {
        if(JCMClientConfig.INSTANCE.scripting.scriptDebugMode.value()) {
            MTRScriptingModClient.LOGGER.error("[MTR Scripting] Error while {}!", action, e);
            MTRContentScripting.getScriptManager().scriptErrorNotifier.queue(() -> {
                MinecraftClient.getInstance().getPlayerMapped().sendMessage(Text.cast(VanillaTextWrapper.literal("[MTR] Error while " + action + "!").withColor("RED").withHoverText(VanillaTextWrapper.literal(e.getMessage()).withColor("RED")).impl()), false);
                MinecraftClient.getInstance().getPlayerMapped().sendMessage(Text.cast(TextHelper.setStyle(TextHelper.literal("See Console for details."), Style.getEmptyMapped().withColor(TextFormatting.RED))), false);
            });
        } else {
            MTRScriptingModClient.LOGGER.error("[MTR Scripting] Error while {}: {}", action, e.getMessage());
            MTRScriptingModClient.LOGGER.error("(Enable debug mode to see more information)");
        }
    }
}
