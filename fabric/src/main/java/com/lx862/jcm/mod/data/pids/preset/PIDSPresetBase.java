package com.lx862.jcm.mod.data.pids.preset;

import com.google.gson.JsonObject;
import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.render.text.TextRenderingManager;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.core.operation.ArrivalsResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public abstract class PIDSPresetBase implements RenderHelper {
    protected String id;
    protected String name;
    public final boolean builtin;
    public PIDSPresetBase(String id, @Nullable String name, boolean builtin) {
        this.id = id;
        if(name == null) {
            this.name = id;
        } else {
            this.name = name;
        }
        this.builtin = builtin;
    }
    public PIDSPresetBase(String id) {
        this(id, null, false);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void drawAtlasBackground(GraphicsHolder graphicsHolder, int width, int height, Direction facing) {
        TextRenderingManager.bind(graphicsHolder);
        RenderHelper.drawTexture(graphicsHolder,0, height, 0, width, width, facing, ARGB_WHITE, MAX_RENDER_LIGHT);
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", id);
        jsonObject.addProperty("name", name == null ? id : name);
        return jsonObject;
    }

    public abstract List<PIDSComponent> getComponents(ObjectArrayList<ArrivalResponse> arrivals, String[] customMessages, boolean[] rowHidden, int x, int y, int screenWidth, int screenHeight, int rows, boolean hidePlatform);
    public abstract String getFont();
    public abstract @Nonnull Identifier getBackground();
    public abstract int getTextColor();
    public abstract boolean isRowHidden(int row);

    public abstract void render(PIDSBlockEntity be, GraphicsHolder graphicsHolder, World world, Direction facing, ObjectArrayList<ArrivalResponse> arrivals, boolean[] rowHidden, float tickDelta, int x, int y, int width, int height);
}