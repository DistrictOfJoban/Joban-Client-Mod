package com.lx862.jcm.mod.data.pids.preset;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.scripting.jcm.JCMScripting;
import com.lx862.jcm.mod.scripting.jcm.pids.PIDSScriptContext;
import com.lx862.jcm.mod.scripting.jcm.pids.PIDSScriptInstance;
import com.lx862.jcm.mod.scripting.jcm.pids.PIDSWrapper;
import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.mtrscripting.api.ScriptResultCall;
import com.lx862.mtrscripting.core.ParsedScript;
import com.lx862.mtrscripting.data.ScriptContent;
import com.lx862.mtrscripting.data.UniqueKey;
import com.lx862.mtrscripting.core.ScriptInstance;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.ResourceManagerHelper;
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

    public static ScriptPIDSPreset parse(JsonObject rootJsonObject) throws Exception {
        final String id = rootJsonObject.get("id").getAsString();
        final String name = rootJsonObject.has("name") ? rootJsonObject.get("name").getAsString() : null;
        final boolean builtin = rootJsonObject.has("builtin") && rootJsonObject.get("builtin").getAsBoolean();
        final Identifier thumbnail = rootJsonObject.has("thumbnail") ? new Identifier(rootJsonObject.get("thumbnail").getAsString()) : DEFAULT_THUMBNAIL;

        final List<ScriptContent> scripts = new ObjectArrayList<>();
        if(rootJsonObject.has("scriptFiles")) {
            JsonArray scriptFilesArray = rootJsonObject.get("scriptFiles").getAsJsonArray();
            for(int i = 0; i < scriptFilesArray.size(); i++) {
                Identifier scriptLocation = new Identifier(scriptFilesArray.get(i).getAsString());
                String scriptText = ResourceManagerHelper.readResource(scriptLocation);
                if(scriptText.isEmpty()) {
                    JCMLogger.warn("Script {}:{} is either missing, or the file content is empty!", scriptLocation.getNamespace(), scriptLocation.getPath());
                    continue;
                }

                scripts.add(new ScriptContent(scriptLocation, scriptText));
            }
        }

        if(rootJsonObject.has("scriptTexts")) {
            JsonArray scriptTextArray = rootJsonObject.get("scriptTexts").getAsJsonArray();
            for(int i = 0; i < scriptTextArray.size(); i++) {
                Identifier scriptLocation = new Identifier(Constants.MOD_ID, "script_texts/jcm/pids/" + id + "/line" + i);
                String scriptText = scriptTextArray.get(i).getAsString();
                scripts.add(new ScriptContent(scriptLocation, scriptText));
            }
        }

        List<String> blackList = new ArrayList<>();
        if(rootJsonObject.has("blacklist")) {
            JsonArray blacklistedPIDS = rootJsonObject.getAsJsonArray("blacklist");
            for(int i = 0; i < blacklistedPIDS.size(); i++) {
                blackList.add(blacklistedPIDS.get(i).getAsString());
            }
        }

        ParsedScript parsedScripts = JCMScripting.getScriptManager().parseScript(id, "PIDS", scripts);
        return new ScriptPIDSPreset(id, name, thumbnail, blackList, builtin, parsedScripts);
    }

    @Override
    public void render(PIDSBlockEntity be, GraphicsHolder graphicsHolder, World world, BlockPos pos, Direction facing, ObjectArrayList<ArrivalResponse> arrivals, boolean[] rowHidden, float tickDelta, int x, int y, int width, int height) {
        PIDSWrapper pidsState = new PIDSWrapper(be, arrivals, width, height);
        ScriptInstance<PIDSWrapper> scriptInstance = JCMScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("jcm", "pids", getId(), pos.data), () -> new PIDSScriptInstance(be, getId(), parsedScripts, pidsState));

        if(scriptInstance instanceof PIDSScriptInstance) {
            PIDSScriptInstance pidsScriptInstance = (PIDSScriptInstance) scriptInstance;

            scriptInstance.setWrapperObject(pidsState);
            scriptInstance.getScript().invokeRenderFunctions(scriptInstance, () -> {
                pidsScriptInstance.drawCalls.clear();
                pidsScriptInstance.drawCalls.addAll(((PIDSScriptContext)scriptInstance.getScriptContext()).getDrawCalls());
                scriptInstance.getScriptContext().reset();
            });

            graphicsHolder.translate(0, 0, -0.5);
            for(ScriptResultCall resultCalls : new ArrayList<>(pidsScriptInstance.drawCalls)) {
                if(resultCalls == null) continue;
                graphicsHolder.translate(0, 0, -0.02);
                graphicsHolder.push();
                resultCalls.run(world, graphicsHolder, new StoredMatrixTransformations(), facing, MAX_RENDER_LIGHT);
                graphicsHolder.pop();
            }
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
}
