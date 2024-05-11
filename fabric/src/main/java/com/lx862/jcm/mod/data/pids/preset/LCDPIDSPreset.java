package com.lx862.jcm.mod.data.pids.preset;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.config.ConfigEntry;
import com.lx862.jcm.mod.data.KVPair;
import com.lx862.jcm.mod.data.pids.preset.components.CustomTextComponent;
import com.lx862.jcm.mod.data.pids.preset.components.CustomTextureComponent;
import com.lx862.jcm.mod.data.pids.preset.components.DestinationComponent;
import com.lx862.jcm.mod.data.pids.preset.components.ETAComponent;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.jcm.mod.data.pids.preset.components.base.TextComponent;
import com.lx862.jcm.mod.data.pids.preset.components.base.TextureComponent;
import com.lx862.jcm.mod.render.text.TextOverflowMode;
import com.lx862.jcm.mod.render.text.TextAlignment;
import com.lx862.jcm.mod.render.text.TextRenderingManager;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LCDPIDSPreset extends PIDSPresetBase {
    private static final int PIDS_MARGIN = 5;
    private static final float ARRIVAL_TEXT_SCALE = 1.3F;
    public LCDPIDSPreset() {
        super("lcd_pids", "Hong Kong LCD PIDS", true);
    }

    public void render(PIDSBlockEntity be, GraphicsHolder graphicsHolder, World world, Direction facing, ObjectArrayList<ArrivalResponse> arrivals, boolean[] rowHidden, float tickDelta, int x, int y, int width, int height) {
        // Debug View Texture
        if(ConfigEntry.DEBUG_MODE.getBool() && ConfigEntry.NEW_TEXT_RENDERER.getBool()) {
            //TextureTextRenderer.stressTest(5);
            drawAtlasBackground(graphicsHolder, width, height, facing);
        }

        graphicsHolder.translate(0, 0, -0.5);

        List<PIDSComponent> components = getComponents(arrivals, be.getCustomMessages(), rowHidden, 0, 0, width, height, be.getRowAmount(), be.platformNumberHidden());
        List<PIDSComponent> textureComponents = components.stream().filter(e -> e instanceof TextureComponent).collect(Collectors.toList());
        List<PIDSComponent> textComponents = components.stream().filter(e -> e instanceof TextComponent).collect(Collectors.toList());

        // Texture
        graphicsHolder.push();
        for(PIDSComponent component : textureComponents) {
            graphicsHolder.push();
            component.render(graphicsHolder, null, facing, new PIDSContext(world, arrivals, tickDelta));
            graphicsHolder.pop();
            graphicsHolder.translate(0, 0, -0.003);
        }
        graphicsHolder.pop();

        // Text
        graphicsHolder.translate(0, 0, -0.5);
        TextRenderingManager.bind(graphicsHolder);
        for(PIDSComponent component : textComponents) {
            graphicsHolder.push();
            component.render(graphicsHolder, null, facing, new PIDSContext(world, arrivals, tickDelta));
            graphicsHolder.pop();
        }
    }

    @Override
    public List<PIDSComponent> getComponents(ObjectArrayList<ArrivalResponse> arrivals, String[] customMessages, boolean[] rowHidden, int x, int y, int screenWidth, int screenHeight, int rows, boolean hidePlatform) {
        int startX = x + PIDS_MARGIN;
        int startY = y + 6;
        int contentWidth = screenWidth - PIDS_MARGIN;
        List<PIDSComponent> components = new ArrayList<>();
        components.add(new CustomTextureComponent(x, y, screenWidth, screenHeight, new KVPair().with("textureId", getBackground().getNamespace() + ":" + getBackground().getPath())));

        /* Arrivals */
        int arrivalIndex = 0;
        double rowY = startY;
        for(int i = 0; i < rows; i++) {
            if(arrivalIndex >= arrivals.size()) continue;

            if(!customMessages[i].isEmpty()) {
                components.add(new CustomTextComponent(startX, rowY, 78 * ARRIVAL_TEXT_SCALE, 10, TextComponent.of(TextAlignment.LEFT, TextOverflowMode.STRETCH, getFont(), getTextColor(), ARRIVAL_TEXT_SCALE).with("text", customMessages[i])));
            } else {
                if(!rowHidden[i]) {
                    float destinationMaxWidth = !hidePlatform ? (44 * ARRIVAL_TEXT_SCALE) : (54 * ARRIVAL_TEXT_SCALE);
                    components.add(new DestinationComponent(startX, rowY, destinationMaxWidth, 10, TextComponent.of(TextAlignment.LEFT, TextOverflowMode.STRETCH, getFont(), getTextColor(), ARRIVAL_TEXT_SCALE).with("arrivalIndex", arrivalIndex)));
                    components.add(new ETAComponent(contentWidth, rowY, 22 * ARRIVAL_TEXT_SCALE, 20, TextComponent.of(TextAlignment.RIGHT, TextOverflowMode.STRETCH, getFont(), getTextColor(), ARRIVAL_TEXT_SCALE).with("arrivalIndex", arrivalIndex)));
                    arrivalIndex++;
                }
            }

            rowY += (screenHeight / 5.25) * ARRIVAL_TEXT_SCALE;
        }
        return components;
    }

    @Override
    public String getFont() {
        return "jsblock:pids_lcd";
    }

    @Override
    public @Nonnull Identifier getBackground() {
        return new Identifier(Constants.MOD_ID, "textures/block/pids/black.png");
    }

    @Override
    public int getTextColor() {
        return 0xFFEADD9A;
    }

    @Override
    public boolean isRowHidden(int row) {
        return false;
    }
}
