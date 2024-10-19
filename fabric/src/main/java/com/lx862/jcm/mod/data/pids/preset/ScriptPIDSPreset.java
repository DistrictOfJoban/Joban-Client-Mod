package com.lx862.jcm.mod.data.pids.preset;

import com.google.gson.JsonObject;
import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.jcm.mod.data.scripting.PIDSScriptInstance;
import com.lx862.jcm.mod.data.scripting.PIDSScriptObject;
import com.lx862.jcm.mod.data.scripting.ScriptInstanceManager;
import com.lx862.jcm.mod.data.scripting.base.ScriptInstance;
import com.lx862.jcm.mod.data.scripting.util.*;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaClass;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.Scriptable;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.ResourceManagerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ScriptPIDSPreset extends PIDSPresetBase {
    public final Scriptable scope;
    private static final Identifier PLACEHOLDER_BACKGROUND = Constants.id("textures/block/pids/rv_default.png");

    public ScriptPIDSPreset(String id, @Nullable String name, Scriptable scope) {
        super(id, name, false);
        this.scope = scope;
    }

    public static ScriptPIDSPreset parse(JsonObject rootJsonObject) {
        String id = rootJsonObject.get("id").getAsString();
        String name = id;
        if(rootJsonObject.has("name")) {
            name = rootJsonObject.get("name").getAsString();
        }

        Identifier scriptLocation = new Identifier(rootJsonObject.get("script").getAsString());

        String scriptText = ResourceManagerHelper.readResource(scriptLocation);
        if(!scriptText.isEmpty()) {
            try {
                Context cx = Context.enter();
                cx.setLanguageVersion(Context.VERSION_ES6);
                Scriptable scope = cx.initStandardObjects();
                scope.put("print", scope, new NativeJavaMethod(ScriptResourceUtil.class.getMethod("print", Object[].class), "print"));

                scope.put("Timing", scope, new NativeJavaClass(scope, TimingUtil.class));
                scope.put("StateTracker", scope, new NativeJavaClass(scope, StateTracker.class));
                scope.put("CycleTracker", scope, new NativeJavaClass(scope, CycleTracker.class));
                scope.put("RateLimit", scope, new NativeJavaClass(scope, RateLimit.class));
                scope.put("TextUtil", scope, new NativeJavaClass(scope, TextUtil.class));

                scope.put("MinecraftClient", scope, new NativeJavaClass(scope, MinecraftClientUtil.class));


                cx.evaluateString(scope, "\"use strict\"", "", 1, null);
                cx.evaluateString(scope, scriptText, scriptLocation.getNamespace() + ":" + scriptLocation.getPath(), 1, null);
                ScriptPIDSPreset preset = new ScriptPIDSPreset(id, name, scope);
                JCMLogger.info("Script for " + scriptLocation.getNamespace() + ":" + scriptLocation.getPath() + " has been parsed!");
                return preset;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public void render(PIDSBlockEntity be, GraphicsHolder graphicsHolder, World world, BlockPos pos, Direction facing, ObjectArrayList<ArrivalResponse> arrivals, boolean[] rowHidden, float tickDelta, int x, int y, int width, int height) {
        ScriptInstance scriptInstance = ScriptInstanceManager.getInstance(pos.asLong(), () -> new PIDSScriptInstance(pos, scope));
        PIDSScriptObject obj = new PIDSScriptObject(graphicsHolder, be.getCustomMessages(), be.getRowHidden(), be.platformNumberHidden());

        if(scriptInstance instanceof PIDSScriptInstance) {
            PIDSScriptInstance pidsScriptInstance = (PIDSScriptInstance)scriptInstance;
            scriptInstance.execute(obj, () -> {
                pidsScriptInstance.components.clear();
                pidsScriptInstance.components.addAll(obj.getDrawCalls());
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
