package com.lx862.jcm.mod.data.pids.preset;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.JCMClient;
import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.data.pids.scripting.DrawCall;
import com.lx862.jcm.mod.data.pids.scripting.PIDSScriptContext;
import com.lx862.jcm.mod.data.pids.scripting.PIDSScriptInstance;
import com.lx862.jcm.mod.data.pids.scripting.PIDSWrapper;
import com.lx862.mtrscripting.scripting.ParsedScript;
import com.lx862.mtrscripting.scripting.base.ScriptInstance;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ScriptPIDSPreset extends PIDSPresetBase {
    private static final Identifier DEFAULT_THUMBNAIL = Constants.id("textures/gui/pids_preview_js.png");
    public final ParsedScript parsedScripts;

    public ScriptPIDSPreset(String id, @Nullable String name, Identifier thumbnail, List<String> blacklist, ParsedScript parsedScripts) {
        super(id, name, thumbnail, blacklist, false);
        this.parsedScripts = parsedScripts;
    }

    public static ScriptPIDSPreset parse(JsonObject rootJsonObject) throws Exception {
        final String id = rootJsonObject.get("id").getAsString();
        final String name = rootJsonObject.has("name") ? rootJsonObject.get("name").getAsString() : null;
        final Identifier thumbnail = rootJsonObject.has("thumbnail") ? new Identifier(rootJsonObject.get("thumbnail").getAsString()) : DEFAULT_THUMBNAIL;

        List<Identifier> scriptsToLoad = new ArrayList<>();
        JsonArray scripts = rootJsonObject.get("scripts").getAsJsonArray();
        for(int i = 0; i < scripts.size(); i++) {
            scriptsToLoad.add(new Identifier(scripts.get(i).getAsString()));
        }

        List<String> blackList = new ArrayList<>();
        if(rootJsonObject.has("blacklist")) {
            JsonArray blacklistedPIDS = rootJsonObject.getAsJsonArray("blacklist");
            for(int i = 0; i < blacklistedPIDS.size(); i++) {
                blackList.add(blacklistedPIDS.get(i).getAsString());
            }
        }

        ParsedScript parsedScripts = new ParsedScript("PIDS", scriptsToLoad);
        return new ScriptPIDSPreset(id, name, thumbnail, blackList, parsedScripts);
    }

    @Override
    public void render(PIDSBlockEntity be, GraphicsHolder graphicsHolder, World world, BlockPos pos, Direction facing, ObjectArrayList<ArrivalResponse> arrivals, boolean[] rowHidden, float tickDelta, int x, int y, int width, int height) {
        ScriptInstance<PIDSWrapper> scriptInstance = JCMClient.scriptManager.instanceManager.getInstance(getId(), pos.asLong(), () -> new PIDSScriptInstance(getId(), pos, parsedScripts));

        if(scriptInstance instanceof PIDSScriptInstance) {
            PIDSScriptInstance pidsScriptInstance = (PIDSScriptInstance) scriptInstance;
            PIDSWrapper pidsState = new PIDSWrapper(be, arrivals, width, height);

            scriptInstance.updateWrapperObject(pidsState);
            scriptInstance.parsedScripts.invokeRenderFunction(scriptInstance, () -> {
                pidsScriptInstance.drawCalls.clear();
                pidsScriptInstance.drawCalls.addAll(((PIDSScriptContext)scriptInstance.getScriptContext()).getDrawCalls());
            });

            graphicsHolder.translate(0, 0, -0.5);
            for(DrawCall drawCall : new ArrayList<>(pidsScriptInstance.drawCalls)) {
                if(drawCall == null) continue;
                graphicsHolder.translate(0, 0, -0.02);
                graphicsHolder.push();
                drawCall.draw(graphicsHolder, facing);
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
