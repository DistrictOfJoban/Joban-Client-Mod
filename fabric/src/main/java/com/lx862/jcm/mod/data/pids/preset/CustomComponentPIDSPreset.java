package com.lx862.jcm.mod.data.pids.preset;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.jcm.mod.util.JCMLogger;
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

public class CustomComponentPIDSPreset extends PIDSPresetBase {
    public final List<PIDSComponent> components;
    private static final Identifier PLACEHOLDER_BACKGROUND = Constants.id("textures/block/pids/rv_default.png");
    public CustomComponentPIDSPreset(String id, @Nullable String name, boolean builtin) {
        super(id, name, builtin);
        this.components = new ArrayList<>();
    }

    public static CustomComponentPIDSPreset parse(JsonObject rootJsonObject) {
        String id = rootJsonObject.get("id").getAsString();
        String name = id;
        if(rootJsonObject.has("name")) {
            name = rootJsonObject.get("name").getAsString();
        }
        boolean builtin = rootJsonObject.has("builtin") && rootJsonObject.get("builtin").getAsBoolean();

        String componentFile = ResourceManagerHelper.readResource(new Identifier(rootJsonObject.get("file").getAsString()));
        JsonArray jsonArray = new JsonParser().parse(componentFile).getAsJsonArray();

        CustomComponentPIDSPreset preset = new CustomComponentPIDSPreset(id, name, builtin);
        for(int i = 0; i < jsonArray.size(); i++) {
            JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();
            PIDSComponent component = PIDSComponent.parse(jsonObject);
            if(component != null) {
                preset.components.add(component);
            } else {
                JCMLogger.warn("Unknown component parsed for preset {}", id);
            }
        }

        return preset;
    }

    @Override
    public void render(PIDSBlockEntity be, GraphicsHolder graphicsHolder, World world, BlockPos pos, Direction facing, ObjectArrayList<ArrivalResponse> arrivals, boolean[] rowHidden, float tickDelta, int x, int y, int width, int height) {
        graphicsHolder.translate(0, 0, -0.5);

        PIDSContext pidsContext = new PIDSContext(world, pos, be.getCustomMessages(), arrivals, tickDelta);

        // Texture
        for(PIDSComponent component : components) {
            graphicsHolder.translate(0, 0, -0.02);
            graphicsHolder.push();
            component.render(graphicsHolder, null, facing, pidsContext);
            graphicsHolder.pop();
        }
    }

    @Override
    public List<PIDSComponent> getComponents(ObjectArrayList<ArrivalResponse> arrivals, String[] customMessages, boolean[] rowHidden, int x, int y, int screenWidth, int screenHeight, int rows, boolean hidePlatform) {
        return components;
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
