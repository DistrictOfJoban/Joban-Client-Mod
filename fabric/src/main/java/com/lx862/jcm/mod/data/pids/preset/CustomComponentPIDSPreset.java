package com.lx862.jcm.mod.data.pids.preset;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.config.ConfigEntry;
import com.lx862.jcm.mod.data.pids.preset.components.*;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.jcm.mod.data.pids.preset.components.base.TextComponent;
import com.lx862.jcm.mod.data.pids.preset.components.base.TextureComponent;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.render.TextOverflowMode;
import com.lx862.jcm.mod.render.text.TextAlignment;
import com.lx862.jcm.mod.render.text.TextRenderingManager;
import com.lx862.jcm.mod.render.text.font.FontManager;
import com.lx862.jcm.mod.resources.mcmeta.McMetaManager;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.core.operation.ArrivalsResponse;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.RenderLayer;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.ResourceManagerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CustomComponentPIDSPreset extends PIDSPresetBase {
    public final List<PIDSComponent> components;
    private static final Identifier PLACEHOLDER_BACKGROUND = new Identifier(Constants.MOD_ID, "textures/block/pids/rv_default.png");
    public CustomComponentPIDSPreset(String id, @Nullable String name) {
        super(id, name, false);
        this.components = new ArrayList<>();
    }

    public static CustomComponentPIDSPreset parse(JsonObject rootJsonObject) {
        String id = rootJsonObject.get("id").getAsString();
        String name = id;
        if(rootJsonObject.has("name")) {
            name = rootJsonObject.get("name").getAsString();
        }

        String componentFile = ResourceManagerHelper.readResource(new Identifier(rootJsonObject.get("file").getAsString()));
        JsonArray jsonArray = new JsonParser().parse(componentFile).getAsJsonArray();

        CustomComponentPIDSPreset preset = new CustomComponentPIDSPreset(id, name);
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
    public void render(PIDSBlockEntity be, GraphicsHolder graphicsHolder, World world, Direction facing, ArrivalsResponse arrivals, boolean[] rowHidden, float tickDelta, int x, int y, int width, int height) {
        graphicsHolder.translate(0, 0, -0.5);

        List<PIDSComponent> textureComponents = components.stream().filter(e -> e instanceof TextureComponent).collect(Collectors.toList());
        List<PIDSComponent> textComponents = components.stream().filter(e -> e instanceof TextComponent).collect(Collectors.toList());

        // Texture
        for(PIDSComponent component : textureComponents) {
            graphicsHolder.push();
            component.render(graphicsHolder, null, world, facing);
            graphicsHolder.pop();
        }

        // Text
        graphicsHolder.translate(0, 0, -0.5);
        TextRenderingManager.bind(graphicsHolder);
        for(PIDSComponent component : textComponents) {
            graphicsHolder.push();
            component.render(graphicsHolder, null, world, facing);
            graphicsHolder.pop();
        }
    }

    @Override
    public List<PIDSComponent> getComponents(ArrivalsResponse arrivals, String[] customMessages, boolean[] rowHidden, int x, int y, int screenWidth, int screenHeight, int rows, boolean hidePlatform) {
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