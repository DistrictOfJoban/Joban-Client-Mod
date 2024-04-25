package com.lx862.jcm.mod.render.gui.screen;

import com.lx862.jcm.mod.data.pids.PIDSManager;
import com.lx862.jcm.mod.data.pids.preset.PIDSContext;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.jcm.mod.data.pids.preset.components.base.TextComponent;
import com.lx862.jcm.mod.data.pids.preset.components.base.TextureComponent;
import com.lx862.jcm.mod.render.GuiHelper;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.render.gui.screen.base.TitledScreen;
import com.lx862.jcm.mod.render.text.TextInfo;
import com.lx862.jcm.mod.render.text.TextRenderingManager;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.core.operation.ArrivalsResponse;
import org.mtr.core.serializer.JsonReader;
import org.mtr.libraries.com.google.gson.JsonArray;
import org.mtr.libraries.com.google.gson.JsonObject;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;

import java.util.List;
import java.util.stream.Collectors;

public class PIDSDemoScreen extends TitledScreen implements RenderHelper, GuiHelper {
    public PIDSDemoScreen() {
        super(true);
    }

    @Override
    public MutableText getScreenTitle() {
        return TextUtil.literal("PIDS Component");
    }

    @Override
    public MutableText getScreenSubtitle() {
        return TextUtil.literal("Test Rendering");
    }

    @Override
    protected void init2() {
        super.init2();
        // 1.16 button texture will glitch out if the button is too wide, we need a clamp
        int btnWidth = GuiHelper.getButtonWidth(width * 0.6);

        ButtonWidgetExtension btn = new ButtonWidgetExtension(
                (width - btnWidth) / 2,
                height - 30,
                btnWidth,
                20,
                TextUtil.translatable("gui.done"),
                (b) -> onClose2()
        );

        addChild(new ClickableWidget(btn));
    }

    @Override
    public void drawBackground(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta) {
        super.drawBackground(graphicsHolder, mouseX, mouseY, tickDelta);
        drawPIDS(graphicsHolder, new GuiDrawing(graphicsHolder));
    }

    private void drawPIDS(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing) {
        JsonObject fakeArrivalsJson = new JsonObject();
        JsonArray fakeArrivalsArrayJson = new JsonArray();
        for(int i = 0; i < 4; i++) {
            JsonObject jo = new JsonObject();
            jo.addProperty("destination", "Arrival " + (i+1));
            jo.addProperty("arrival", 100 + System.currentTimeMillis() + (i * 10000));
            jo.addProperty("departure", 100 + System.currentTimeMillis() + ((i+1) * 10000));
            jo.addProperty("realtime", true);
            jo.addProperty("platformName", "2");
            fakeArrivalsArrayJson.add(jo);
        }
        fakeArrivalsJson.add("arrivals", fakeArrivalsArrayJson);

        ArrivalsResponse fakeArrivals = new ArrivalsResponse(new JsonReader(fakeArrivalsJson));

        List<PIDSComponent> components = PIDSManager.getPreset("rv_pids").getComponents(fakeArrivals, new String[]{"", "", "", ""}, new boolean[]{false, false, false, false}, 0, 0, 150, 80, 4, false);
        List<PIDSComponent> textureComponents = components.stream().filter(e -> e instanceof TextureComponent).collect(Collectors.toList());
        List<PIDSComponent> textComponents = components.stream().filter(e -> e instanceof TextComponent).collect(Collectors.toList());

        // Texture
        for(PIDSComponent component : textureComponents) {
            graphicsHolder.push();
            component.render(graphicsHolder, guiDrawing, Direction.NORTH, new PIDSContext(World.cast(MinecraftClient.getInstance().getWorldMapped()), fakeArrivals));
            graphicsHolder.pop();
        }

        // Text
        graphicsHolder.translate(0, 0, 0.5);
        TextRenderingManager.bind(graphicsHolder);
        for(PIDSComponent component : textComponents) {
            graphicsHolder.push();
            component.render(graphicsHolder, guiDrawing, Direction.NORTH, new PIDSContext(World.cast(MinecraftClient.getInstance().getWorldMapped()), fakeArrivals));
            graphicsHolder.pop();
        }
    }

    @Override
    public boolean isPauseScreen2() {
        return false;
    }
}
