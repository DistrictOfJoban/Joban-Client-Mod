package com.lx862.jcm.mod.data.pids.preset;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.jcm.mod.data.pids.scripting.PIDSScriptContext;
import com.lx862.jcm.mod.data.pids.scripting.PIDSScriptInstance;
import com.lx862.jcm.mod.data.pids.scripting.PIDSWrapper;
import com.lx862.jcm.mod.data.scripting.ParsedScript;
import com.lx862.jcm.mod.data.scripting.ScriptInstanceManager;
import com.lx862.jcm.mod.data.scripting.base.ScriptInstance;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ScriptPIDSPreset extends PIDSPresetBase {
    public final ParsedScript parsedScripts;
    private static final Identifier PLACEHOLDER_BACKGROUND = Constants.id("textures/block/pids/rv_default.png");

    public ScriptPIDSPreset(String id, @Nullable String name, ParsedScript parsedScripts) {
        super(id, name, false);
        this.parsedScripts = parsedScripts;
    }

    public static ScriptPIDSPreset parse(JsonObject rootJsonObject) {
        String id = rootJsonObject.get("id").getAsString();
        String name = id;
        if(rootJsonObject.has("name")) {
            name = rootJsonObject.get("name").getAsString();
        }

        List<Identifier> scriptsToLoad = new ArrayList<>();
        JsonArray arr = rootJsonObject.get("scripts").getAsJsonArray();
        for(int i = 0; i < arr.size(); i++) {
            scriptsToLoad.add(new Identifier(arr.get(i).getAsString()));
        }

        ParsedScript parsedScripts = new ParsedScript("PIDS", scriptsToLoad);
        return new ScriptPIDSPreset(id, name, parsedScripts);
    }

    @Override
    public void render(PIDSBlockEntity be, GraphicsHolder graphicsHolder, World world, BlockPos pos, Direction facing, ObjectArrayList<ArrivalResponse> arrivals, boolean[] rowHidden, float tickDelta, int x, int y, int width, int height) {
        PIDSWrapper wrapperObj = new PIDSWrapper(be);
        ScriptInstance scriptInstance = ScriptInstanceManager.getInstance(pos.asLong(), () -> new PIDSScriptInstance(pos, parsedScripts, wrapperObj));

        if(scriptInstance instanceof PIDSScriptInstance) {
            PIDSScriptInstance pidsScriptInstance = (PIDSScriptInstance)scriptInstance;
            scriptInstance.onRender(wrapperObj, () -> {
                pidsScriptInstance.components.clear();
                pidsScriptInstance.components.addAll(((PIDSScriptContext)scriptInstance.getScriptContext()).getDrawCalls());
            });

            graphicsHolder.translate(0, 0, -0.5);
            PIDSContext pidsContext = new PIDSContext(world, pos, be.getCustomMessages(), arrivals, tickDelta);
            for(PIDSComponent component : new ArrayList<>(pidsScriptInstance.components)) {
                graphicsHolder.translate(0, 0, -0.02);
                graphicsHolder.push();
                component.render(graphicsHolder, null, facing, pidsContext);
                graphicsHolder.pop();
            }
        }
    }

    @Override
    public List<PIDSComponent> getComponents(ObjectArrayList<ArrivalResponse> arrivals, String[] customMessages, boolean[] rowHidden, int x, int y, int screenWidth, int screenHeight, int rows, boolean hidePlatform) {
        return new ArrayList<>();
    }

    @Override
    public String getFont() {
        return "mtr:mtr";
    }

    @Override
    public @Nonnull Identifier getBackground() {
        return PLACEHOLDER_BACKGROUND;
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
