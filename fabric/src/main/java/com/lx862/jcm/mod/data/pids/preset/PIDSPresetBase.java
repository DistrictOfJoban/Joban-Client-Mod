package com.lx862.jcm.mod.data.pids.preset;

import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.render.text.TextRenderingManager;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.render.StoredMatrixTransformations;

import javax.annotation.Nullable;
import java.util.List;

public abstract class PIDSPresetBase implements RenderHelper {
    public static final float BASE_SCALE = 1/96F;
    private final String id;
    private final String name;
    private final List<String> blacklist;
    protected final Identifier thumbnail;
    public final boolean builtin;

    public PIDSPresetBase(String id, @Nullable String name, Identifier thumbnail, List<String> blacklistedPIDS, boolean builtin) {
        this.id = id;
        if(name == null) {
            this.name = id;
        } else {
            this.name = name;
        }
        this.blacklist = blacklistedPIDS;
        this.builtin = builtin;
        this.thumbnail = thumbnail;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Identifier getThumbnail() {
        return thumbnail;
    }

    public boolean typeAllowed(String pidsType) {
        return !blacklist.contains(pidsType);
    }

    public void drawAtlasBackground(GraphicsHolder graphicsHolder, int width, int height, Direction facing) {
        TextRenderingManager.bind(graphicsHolder);
        RenderHelper.drawTexture(graphicsHolder,0, height, 0, width, width, facing, ARGB_WHITE, MAX_RENDER_LIGHT);
    }

    public abstract int getTextColor();
    public abstract boolean isRowHidden(int row);

    public abstract void render(PIDSBlockEntity be, GraphicsHolder graphicsHolder, StoredMatrixTransformations storedMatrixTransformations, World world, BlockPos pos, Direction facing, ObjectArrayList<ArrivalResponse> arrivals, boolean[] rowHidden, float tickDelta, int x, int y, int width, int height);
}