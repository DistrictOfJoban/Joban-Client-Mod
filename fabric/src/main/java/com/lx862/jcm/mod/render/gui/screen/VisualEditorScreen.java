package com.lx862.jcm.mod.render.gui.screen;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.data.pids.PIDSManager;
import com.lx862.jcm.mod.data.pids.preset.JsonPIDSPreset;
import com.lx862.jcm.mod.data.pids.preset.MutableJsonPIDSPreset;
import com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase;
import com.lx862.jcm.mod.render.GuiHelper;
import com.lx862.jcm.mod.render.gui.screen.base.TitledScreen;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.*;
import org.mtr.mapping.tool.TextCase;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class VisualEditorScreen extends TitledScreen {
    private final File resourcePackFolder;
    private final CheckboxWidgetExtension[] hideRowCheckboxes = new CheckboxWidgetExtension[4];
    private final TextFieldWidgetExtension idTextField;
    private final TextFieldWidgetExtension backgroundTextField;
    private final TextFieldWidgetExtension colorTextField;
    private final CheckboxWidgetExtension showWeatherCheckbox;
    private final CheckboxWidgetExtension showClockCheckbox;
    private final JsonPIDSPreset originalPreset;
    private final MutableJsonPIDSPreset editedPreset;
    private final Screen previousScreen;
    public VisualEditorScreen(String presetId, Screen previousScreen) {
        super(false);
        this.resourcePackFolder = new File(org.mtr.mapping.holder.MinecraftClient.getInstance().getRunDirectoryMapped(), "resourcepacks");
        this.previousScreen = previousScreen;
        this.originalPreset = (JsonPIDSPreset) PIDSManager.getPreset(presetId);
        this.editedPreset = originalPreset.toMutable();

        this.backgroundTextField = new TextFieldWidgetExtension(20, 0, 200, 20, Integer.MAX_VALUE, TextCase.DEFAULT, null, null);
        this.backgroundTextField.setText2(editedPreset.getBackground().getNamespace() + ":" + editedPreset.getBackground().getPath());
        this.backgroundTextField.setChangedListener2(str -> {
            Identifier newId = Identifier.tryParse(str);
            if(newId != null) editedPreset.background = newId;
        });

        this.idTextField = new TextFieldWidgetExtension(20, 0, 200, 20, Integer.MAX_VALUE, TextCase.DEFAULT, null, null);
        this.idTextField.setText2(editedPreset.getId());
        this.idTextField.setChangedListener2(str -> editedPreset.setId(str));

        this.colorTextField = new TextFieldWidgetExtension(20, 0, 200, 20, Integer.MAX_VALUE, TextCase.DEFAULT, null, null);
        this.colorTextField.setText2(Integer.toHexString(editedPreset.getTextColor() - 0xFF000000));
        this.colorTextField.setChangedListener2(str -> {
            try {
                editedPreset.textColor = (255 << 24) + (int)Integer.parseInt(str, 16);
            } catch (Exception e) {
            }
        });

        this.showWeatherCheckbox = new CheckboxWidgetExtension(20, 0, 200, 20,  TextUtil.translatable(TextCategory.GUI, "pids_preset.pids_editor.weather"), true, isChecked -> {
            editedPreset.showWeather = isChecked;
        });

        this.showClockCheckbox = new CheckboxWidgetExtension(20, this.height / 6 + 120, 200, 20, TextUtil.translatable(TextCategory.GUI, "pids_preset.pids_editor.clock"), true, isChecked -> {
            editedPreset.showClock = isChecked;
        });
    }

    @Override
    protected void init2() {
        super.init2();

        idTextField.setY2(this.height / 6 + 20);
        addChild(new ClickableWidget(idTextField));

        backgroundTextField.setY2(this.height / 6 + 45);
        addChild(new ClickableWidget(backgroundTextField));

        colorTextField.setY2(this.height / 6 + 70);
        colorTextField.setText2(Integer.toHexString(editedPreset.getTextColor()));
        addChild(new ClickableWidget(colorTextField));

        showWeatherCheckbox.setY2(this.height / 6 + 95);
        addChild(new ClickableWidget(showWeatherCheckbox));

        showClockCheckbox.setY2(this.height / 6 + 120);
        addChild(new ClickableWidget(showClockCheckbox));

        int totalWidth = hideRowCheckboxes.length * 30;
        int horizontalGap = (this.width - totalWidth) / (hideRowCheckboxes.length + 1);
        int startX = horizontalGap;

        for (int i = 0; i < hideRowCheckboxes.length; i++) {
            int x = startX + i * (30 + horizontalGap) - 15;
            int y = this.height - 35;
            final int index = i;
            hideRowCheckboxes[i] = new CheckboxWidgetExtension(x, y, 20, 20, TextUtil.translatable(TextCategory.GUI, "pids_preset.pids_editor.hiderow", (i+1)), true, isChecked -> {
                editedPreset.rowHidden[index] = isChecked;
            });
            addChild(new ClickableWidget(hideRowCheckboxes[i]));
        }
        populateCurrentPresetFields();
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
        super.render(graphicsHolder, mouseX, mouseY, delta);
        renderPIDSPreview(graphicsHolder, editedPreset);

        // this was for tooltips
        // if (idTextField.isMouseOver(mouseX, mouseY)) {
        // renderWithTooltip(context, Text.of("The name of the template"), mouseX,
        // mouseY);
        // } else if (backgroundTextField.isMouseOver(mouseX, mouseY)) {
        // renderTooltip(matrices, Text.of("The background path of the PID"), mouseX,
        // mouseY);
        // } else if (colorTextField.isMouseOver(mouseX, mouseY)) {
        // renderTooltip(matrices, Text.of("The color of the text shown on the PID"),
        // mouseX, mouseY);
        // }
    }

    @Override
    public MutableText getScreenTitle() {
        return TextUtil.translatable(TextCategory.GUI, "pids_preset.pids_editor.title");
    }

    @Override
    public MutableText getScreenSubtitle() {
        return TextUtil.translatable(TextCategory.GUI, "pids_preset.pids_editor.editingpreset").append(":").append(" ").append(editedPreset.getId());
    }

    @Override
    public boolean isPauseScreen2() {
        return false;
    }

    @Override
    public void onClose2() {
        super.onClose2();
        MinecraftClient.getInstance().openScreen(
                new Screen(new EditorSaveScreen(originalPreset, editedPreset).withPreviousScreen(previousScreen))
        );
    }

    private void populateCurrentPresetFields() {
        idTextField.setText2(editedPreset.getId());
        backgroundTextField.setText2(editedPreset.getBackground().getNamespace() + ":" + editedPreset.getBackground().getPath());
        colorTextField.setText2(Integer.toHexString(editedPreset.getTextColor()));
        showWeatherCheckbox.setChecked(editedPreset.getShowWeather());
        showClockCheckbox.setChecked(editedPreset.getShowClock());

        for(int i = 0; i < 4; i++) {
            hideRowCheckboxes[i].setChecked(editedPreset.isRowHidden(i));
        }
    }

    private void renderPIDSPreview(GraphicsHolder context, JsonPIDSPreset pidsPreset) {
        Identifier backgroundId = pidsPreset.getBackground();
        Identifier frameTexture = new Identifier(Constants.MOD_ID, "textures/editor/frame.png");
        int textColor = pidsPreset.getTextColor();

        GuiDrawing guiDrawing = new GuiDrawing(context);

        int previewWidth = 160;
        int previewHeight = 90;

        int baseWidth = 427;
        double scaleFactor = (double)getWidthMapped() / baseWidth;
        double frameScaleFactor = 1.2;
        double scaledWidth = previewWidth * scaleFactor;
        double scaledHeight = previewHeight * scaleFactor;

        int startX = (getWidthMapped() / 2) + 30;
        int startY = (getHeightMapped() / 4);

        int frameX = startX - (int)(((previewWidth * 0.932 * scaleFactor * frameScaleFactor) - (previewWidth * scaleFactor)) / 2);
        int frameY = startY - (int)((previewHeight * scaleFactor * frameScaleFactor - previewHeight * scaleFactor) / 2);

        context.drawCenteredText("Preview", (int)(startX + (scaledWidth / 2)), startY - 20, 0xFFFFFF);

        context.push();
        context.translate((getWidthMapped() / 2.0) + 30, startY, 0);
        context.scale((float)scaleFactor, (float)scaleFactor, (float)scaleFactor);

        GuiHelper.drawTexture(guiDrawing, frameTexture, frameX, frameY, (int)(previewWidth * 0.932 * scaleFactor * frameScaleFactor), (int)(previewHeight * scaleFactor * frameScaleFactor));
        // drawTexture unaffected by matrices scale, probably bug in mappings
        GuiHelper.drawTexture(guiDrawing, backgroundId, startX, startY, previewWidth * scaleFactor, previewHeight * scaleFactor);
        if (pidsPreset.getShowWeather()) {
            GuiHelper.drawTexture(guiDrawing, new Identifier(Constants.MOD_ID, "textures/block/pids/weather_sunny.png"), startX + 7, startY, 11 * scaleFactor, 11 * scaleFactor);
        }

        World world = World.cast(MinecraftClient.getInstance().getWorldMapped());

        if (pidsPreset.getShowClock() && world != null) {
            long timeNow = WorldHelper.getTimeOfDay(world) + 6000;
            long hours = timeNow / 1000;
            long minutes = Math.round((timeNow - (hours * 1000)) / 16.8);
            String timeString = String.format("%02d:%02d", hours % 24, minutes % 60);

            context.drawText(timeString, 130, 5, 0xFFFFFF, false, GraphicsHolder.getDefaultLight());
        }

        {
            context.push();
            context.translate(0, 1, 0);
            if(editedPreset.topPadding) context.translate(0, 14, 0);
            context.scale(1.4F, 1.4F, 1.4F);
            for(int i = 0; i < 4; i++) {
                if (!pidsPreset.isRowHidden(i)) {
                    context.drawText(TextUtil.translatable(TextCategory.GUI, "pids_preset.pids_editor.station"), 5, (int)(i * 14), textColor, false, GraphicsHolder.getDefaultLight());
                }
            }
            context.pop();
        }

        context.pop();
    }

    private static boolean isZipContainsFile(File zipFile, String filePath) {
        try (ZipFile zf = new ZipFile(zipFile)) {
            ZipEntry entry = zf.getEntry(filePath);
            return entry != null;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}