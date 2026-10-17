package com.lx862.jcm.mod.data.pids.preset;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.config.JCMClientConfig;
import com.lx862.jcm.mod.scripting.JCMScripting;
import com.lx862.jcm.mod.scripting.pids.PIDSScriptContext;
import com.lx862.jcm.mod.scripting.pids.PIDSScriptInstance;
import com.lx862.jcm.mod.scripting.pids.PIDSWrapper;
import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.mtrscripting.core.primitive.ParsedScript;
import com.lx862.mtrscripting.core.primitive.ScriptContent;
import com.lx862.mtrscripting.core.primitive.UniqueKey;
import com.lx862.mtrscripting.core.primitive.ScriptInstance;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongImmutableList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.ResourceManagerHelper;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.render.StoredMatrixTransformations;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ScriptPIDSPreset extends PIDSPresetBase {
    private static final Identifier DEFAULT_THUMBNAIL = Constants.id("textures/gui/pids_preview_js.png");
    public final ParsedScript parsedScripts;

    public ScriptPIDSPreset(String id, @Nullable String name, Identifier thumbnail, List<String> blacklist, boolean builtin, ParsedScript parsedScripts) {
        super(id, name, thumbnail, blacklist, builtin);
        this.parsedScripts = parsedScripts;
    }

    /**
     * Yanyang: a PIDS preset entry may freely mix JavaScript and C++.
     *
     * <p>{@code scriptLocations} (the 2.3.0-beta.2 key) classifies every entry
     * by itself — {@code *.js} is JavaScript, anything else is a C++ module
     * (platform-agnostic stem, full library path, or per-platform map). The
     * legacy {@code scriptFiles} key keeps its JS semantics (extensionless
     * entries are JS) but also accepts unambiguous native forms. Explicit
     * {@code "language"} values are honored as overrides: {@code "cpp"} keeps
     * the old native-only entry semantics, {@code "javascript"/"js"} forces
     * every location down the JS path. {@code nativeLibrary}/
     * {@code nativeLibraries} may be combined with .js entries.
     */
    public static ScriptPIDSPreset parse(JsonObject rootJsonObject) throws Exception {
        final String id = rootJsonObject.get("id").getAsString();

        final String name = rootJsonObject.has("name") ? rootJsonObject.get("name").getAsString() : null;
        final boolean builtin = rootJsonObject.has("builtin") && rootJsonObject.get("builtin").getAsBoolean();
        final Identifier thumbnail = rootJsonObject.has("thumbnail") ? new Identifier(rootJsonObject.get("thumbnail").getAsString()) : DEFAULT_THUMBNAIL;

        final boolean explicitCpp = com.lx862.mtrscripting.mod.resource.ScriptResourceProvider.isNativeCppScript(rootJsonObject);
        final boolean explicitJs = !explicitCpp && com.lx862.mtrscripting.mod.resource.ScriptResourceProvider.isExplicitJavascript(rootJsonObject);

        final List<ScriptContent> scripts = new ObjectArrayList<>();
        final List<JsonElement> nativeDeclarations = new ArrayList<>();
        final List<ScriptContent> jsFiles = new ObjectArrayList<>();

        final JsonElement inputElement = (rootJsonObject.has("scriptInput") && !rootJsonObject.get("scriptInput").isJsonNull())
                ? rootJsonObject.get("scriptInput") : null;

        /* Legacy scriptFiles key: extensionless entries stay JavaScript,
           .so/.dll/.dylib paths and per-platform objects are native. */
        if(rootJsonObject.has("scriptFiles") && !rootJsonObject.get("scriptFiles").isJsonNull()) {
            JsonArray scriptFilesArray = rootJsonObject.get("scriptFiles").getAsJsonArray();
            for(int i = 0; i < scriptFilesArray.size(); i++) {
                collectScriptLocationEntry(scriptFilesArray.get(i), false, explicitJs, explicitCpp, id, jsFiles, nativeDeclarations);
            }
        }

        /* 2.3.0-beta.2 scriptLocations key: same auto-detection as the
           vehicle/lift/eyecandy script entries — extensionless = native. */
        if(rootJsonObject.has("scriptLocations") && !rootJsonObject.get("scriptLocations").isJsonNull()) {
            JsonArray scriptLocationsArray = rootJsonObject.get("scriptLocations").getAsJsonArray();
            for(int i = 0; i < scriptLocationsArray.size(); i++) {
                collectScriptLocationEntry(scriptLocationsArray.get(i), true, explicitJs, explicitCpp, id, jsFiles, nativeDeclarations);
            }
        }

        /* scriptTexts — pure JS, ignored in native-only (language=cpp)
           entries exactly like the JS locations above. */
        final boolean hasTexts = !explicitCpp && rootJsonObject.has("scriptTexts") && !rootJsonObject.get("scriptTexts").isJsonNull();

        /* Assemble the JS part: SCRIPT_INPUT first, then the texts, then the
           files. The JS part only exists when there is actual JS code —
           input alone doesn't spin up an empty script, so a native-only
           preset stays native-only. */
        if(!jsFiles.isEmpty() || hasTexts) {
            if(inputElement != null) {
                Identifier scriptLocationSource = Constants.id("internal/pids/internal_script_input/" + id);
                scripts.add(new ScriptContent(scriptLocationSource, "const SCRIPT_INPUT = " + inputElement.toString() + ";"));
            }

            if(hasTexts) {
                JsonArray scriptTextArray = rootJsonObject.get("scriptTexts").getAsJsonArray();
                for(int i = 0; i < scriptTextArray.size(); i++) {
                    Identifier scriptLocationSource = Constants.id("internal/pids/internal_script_texts/" + id + "/line" + i);
                    scripts.add(new ScriptContent(scriptLocationSource, scriptTextArray.get(i).getAsString()));
                }
            }

            scripts.addAll(jsFiles);
        }

        /* Legacy native fields: nativeLibrary/nativeLibraries are always C++
           declarations and can be combined with .js scriptLocations entries. */
        com.lx862.mtrscripting.mod.resource.ScriptResourceProvider.collectLegacyNativeFields(rootJsonObject, false, nativeDeclarations);

        if(!nativeDeclarations.isEmpty()) {
            com.lx862.jcm.nativeapi.NativeScriptManager.configure(id, inputElement, null);
            final JsonArray merged = new JsonArray();
            for(JsonElement declaration : nativeDeclarations) {
                merged.add(declaration);
            }
            com.lx862.jcm.nativeapi.NativeScriptManager.loadManyFromDeclaration(id, merged);
        }

        List<String> blackList = new ArrayList<>();
        if(rootJsonObject.has("blacklist")) {
            JsonArray blacklistedPIDS = rootJsonObject.getAsJsonArray("blacklist");
            for(int i = 0; i < blacklistedPIDS.size(); i++) {
                blackList.add(blacklistedPIDS.get(i).getAsString());
            }
        }

        ParsedScript script = null;
        if(!scripts.isEmpty()) {
            try {
                script = JCMScripting.getScriptManager().parseScript(id + " (pids)", "pids", scripts, JCMClientConfig.INSTANCE.scripting.scriptDebugMode.value());
            } catch (Exception e) {
                logError("parsing PIDS script (" + id + ")", e);
                script = null; /* keep the C++ modules alive — degrade gracefully */
            }
        }
        if(script == null && nativeDeclarations.isEmpty()) return null;
        return new ScriptPIDSPreset(id, name, thumbnail, blackList, builtin, script);
    }

    /** Classify one script location entry and dispatch it to the JS file
        list or the native declaration list (shared classification rules with
        the MTR script entries in ScriptResourceProvider). */
    private static void collectScriptLocationEntry(JsonElement entryElement, boolean mtr4LocationsKey,
            boolean explicitJs, boolean explicitCpp, String id,
            List<ScriptContent> jsFiles, List<JsonElement> nativeDeclarations) {
        if(!explicitJs && com.lx862.mtrscripting.mod.resource.ScriptResourceProvider.isNativeScriptLocationEntry(entryElement, mtr4LocationsKey)) {
            nativeDeclarations.add(entryElement);
            return;
        }
        if(explicitCpp) {
            JCMLogger.warn("Ignoring JavaScript location {} in native (language=cpp) PIDS preset \"{}\" — remove the language field to mix JS and C++.", entryElement, id);
            return;
        }
        if(!entryElement.isJsonPrimitive()) {
            JCMLogger.warn("Unsupported non-string script location {} in PIDS preset \"{}\"!", entryElement, id);
            return;
        }
        Identifier scriptLocationSource = new Identifier(entryElement.getAsString());
        String scriptText = ResourceManagerHelper.readResource(scriptLocationSource);
        if(scriptText.isEmpty()) {
            JCMLogger.warn("Script {}:{} is either missing, or the file content is empty!", scriptLocationSource.getNamespace(), scriptLocationSource.getPath());
            return;
        }

        jsFiles.add(new ScriptContent(scriptLocationSource, scriptText));
    }

    @Override
    public void render(PIDSBlockEntity be, GraphicsHolder graphicsHolder, StoredMatrixTransformations storedMatrixTransformations, World world, BlockPos pos, Direction facing, LongImmutableList targetPlatformIds, ObjectArrayList<ArrivalResponse> arrivals, boolean[] rowHidden, float tickDelta, int x, int y, int width, int height, int light) {
        PIDSWrapper pidsWrapper = new PIDSWrapper(be, targetPlatformIds, arrivals, width, height);
        /* Yanyang: mixed entries run the C++ modules first, then the JS part
           below — each side captures into its own replay managers, so their
           draw calls never collide. A failed or platform-skipped native part
           simply doesn't run; a failed JS part keeps the native side alive. */
        if (com.lx862.jcm.nativeapi.NativeScriptManager.isLoaded(getId())) {
            com.lx862.jcm.nativeapi.NativeContentDriver.pids(getId(),pidsWrapper,world,storedMatrixTransformations,facing,light);
        }
        if (parsedScripts == null) {
            return;
        }
        ScriptInstance<PIDSWrapper> scriptInstance = JCMScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("pids", getId(), pos.getX(), pos.getY(), pos.getZ()), () -> new PIDSScriptInstance(be, parsedScripts, pidsWrapper));

        if(scriptInstance instanceof PIDSScriptInstance) {
            PIDSScriptInstance pidsScriptInstance = (PIDSScriptInstance) scriptInstance;
            PIDSScriptContext scriptContext = (PIDSScriptContext)scriptInstance.getContextObject();

            scriptInstance.setWrapperObject(pidsWrapper);
            scriptInstance.getScript().invokeRenderFunctions(scriptInstance, () -> {
                pidsScriptInstance.saveRenderCalls(scriptContext.renderManager());
                pidsScriptInstance.saveSoundCalls(scriptContext.soundManager());
                scriptContext.resetForNextRun();
            });

            graphicsHolder.translate(0, 0, -0.005);
            pidsScriptInstance.getRenderManager().invoke(world, storedMatrixTransformations, facing, light);
            pidsScriptInstance.getSoundManager().invoke(world, pidsWrapper.blockPos());
        }
    }

    @Override
    public int getTextColor() {
        return 0;
    }

    @Override
    public boolean isRowHidden(int row) {
        return false;
    }

    public static void logError(String action, Exception e) {
        if(JCMClientConfig.INSTANCE.scripting.scriptDebugMode.value()) {
            JCMLogger.error("[JCM] Error while " + action + "!", e);

            JCMScripting.getScriptManager().scriptErrorNotifier.queue(() -> {
                MinecraftClient.getInstance().getPlayerMapped().sendMessage(Text.cast(TextHelper.setStyle(TextHelper.literal("[JCM] Error while " + action + "!"), Style.getEmptyMapped().withColor(TextFormatting.RED))), false);
                MinecraftClient.getInstance().getPlayerMapped().sendMessage(Text.cast(TextHelper.setStyle(TextHelper.literal("See Console for details."), Style.getEmptyMapped().withColor(TextFormatting.RED))), false);
            });
        } else {
            JCMLogger.error("[JCM] Error while " + action + ": " + e.getMessage());
            JCMLogger.error("(Enable debug mode to see more information)");
        }
    }
}
