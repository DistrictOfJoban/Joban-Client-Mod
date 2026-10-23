package com.lx862.mtrscripting.mod.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lx862.jcm.mod.config.JCMClientConfig;
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

public interface ScriptResourceProvider {

    void parseCustom();

    void parseMtrResources(JsonObject rootObject, boolean isLegacyResource);

    void validate();

    void reset();

    static ParsedScript tryParseScript(String id, String scriptType, String contextName, JsonObject jsonObject, boolean isParsingMTR4, boolean useSnakeCase) {
        final List<ScriptContent> scripts = new ObjectArrayList<>();
        final String scriptFilesKey = isParsingMTR4 ? "scriptLocations" : useSnakeCase ? "script_files" : "scriptFiles";
        final String scriptTextsKey = isParsingMTR4 ? "prependExpressions" : useSnakeCase ? "script_texts" : "scriptTexts";
        final String scriptInputKey = isParsingMTR4 ? "input" : "scriptInput";
        final String rateLimitKey = isParsingMTR4 ? "rateLimit" : useSnakeCase ? "script_rate_limit" : "scriptRateLimit";
        final String languageKey = isParsingMTR4 ? "language" : useSnakeCase ? "script_lang" : "scriptLang";
        String scriptLang = jsonObject.has(languageKey) ? jsonObject.get(languageKey).getAsString() : ScriptingLanguage.RHINO_JS;
        double rateLimit = jsonObject.has(rateLimitKey) ? jsonObject.get(rateLimitKey).getAsDouble() : 0;

        if (jsonObject.has(scriptFilesKey) || jsonObject.has(scriptTextsKey)) {
            // Parse script input and pass to the script
            if(jsonObject.has(scriptInputKey)) {
                String str = jsonObject.get(scriptInputKey).toString();
                Identifier scriptLocationSource = MTRScriptingMod.id("internal/script_input/" + scriptType + "/" + id);
                scripts.add(new ScriptContent(scriptLocationSource, "const SCRIPT_INPUT = " + str + ";"));
            }

            if(jsonObject.has(scriptTextsKey)) {
                JsonArray scriptTextArray = jsonObject.get(scriptTextsKey).getAsJsonArray();
                for(int i = 0; i < scriptTextArray.size(); i++) {
                    Identifier scriptLocationSource = MTRScriptingMod.id("internal/script_texts/" + scriptType + "/" + id + "/line" + i);
                    String scriptText = scriptTextArray.get(i).getAsString();
                    scripts.add(new ScriptContent(scriptLocationSource, scriptText));
                }
            }

            if(jsonObject.has(scriptFilesKey)) {
                JsonArray scriptFilesArray = jsonObject.get(scriptFilesKey).getAsJsonArray();
                for(int i = 0; i < scriptFilesArray.size(); i++) {
                    Identifier scriptLocationSource = new Identifier(scriptFilesArray.get(i).getAsString());
                    String scriptText = ResourceManagerHelper.readResource(scriptLocationSource);
                    if(scriptText.isEmpty()) {
                        MTRScriptingModClient.LOGGER.warn("[MTR Scripting via JCM] Script {}:{} is missing (or empty)!", scriptLocationSource.getNamespace(), scriptLocationSource.getPath());
                        continue;
                    }

                    scripts.add(new ScriptContent(scriptLocationSource, scriptText));
                }
            }
        }

        try {
            ParsedScript.Configuration configuration = ParsedScript.Configuration.create(scripts, scriptLang, rateLimit);
            return scripts.isEmpty() ? null : MTRContentScripting.getScriptManager().parseScript(id + " (" + scriptType + ")", contextName, configuration, JCMClientConfig.INSTANCE.scripting.scriptDebugMode.value());
        } catch (Exception e) {
            ScriptResourceProvider.logError("parsing " + scriptType + " script (" + id + ")", e);
            return null;
        }
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
