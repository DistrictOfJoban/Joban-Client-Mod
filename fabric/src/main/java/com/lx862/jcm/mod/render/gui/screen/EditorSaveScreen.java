package com.lx862.jcm.mod.render.gui.screen;

import com.google.gson.*;
import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.data.pids.PIDSManager;
import com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase;
import com.lx862.jcm.mod.render.GuiHelper;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.render.gui.screen.base.TitledScreen;
import com.lx862.jcm.mod.render.gui.widget.ContentItem;
import com.lx862.jcm.mod.render.gui.widget.HorizontalWidgetSet;
import com.lx862.jcm.mod.render.gui.widget.ListViewWidget;
import com.lx862.jcm.mod.render.gui.widget.MappedWidget;
import com.lx862.jcm.mod.resources.JCMResourceManager;
import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.GuiDrawing;

import java.io.File;
import java.io.FileReader;

public class EditorSaveScreen extends TitledScreen implements RenderHelper, GuiHelper {
    private static final Identifier PIDS_PREVIEW_BASE = new Identifier("jsblock:textures/gui/pids_preview.png");
    private final File resourcePackFolder;
    private final ListViewWidget listViewWidget;
    private final PIDSPresetBase oldPreset;
    private final PIDSPresetBase newPreset;
    private final String associatedRP;
    public EditorSaveScreen(PIDSPresetBase oldPreset, PIDSPresetBase newPreset) {
        super(false);
        this.resourcePackFolder = new File(org.mtr.mapping.holder.MinecraftClient.getInstance().getRunDirectoryMapped(), "resourcepacks");
        this.listViewWidget = new ListViewWidget();
        this.newPreset = newPreset;
        this.oldPreset = oldPreset;
        this.associatedRP = getAssociatedResourcePack(this.oldPreset.getId());
    }

    @Override
    protected void init2() {
        super.init2();
        int contentWidth = (int)Math.min((width * 0.75), MAX_CONTENT_WIDTH);
        int listViewHeight = (int)((height - 60) * 0.76);
        int startX = (width - contentWidth) / 2;
        int startY = TEXT_PADDING * 6;

        listViewWidget.reset();
        addConfigEntries();
        listViewWidget.setXYSize(startX, startY, contentWidth, listViewHeight);
        addChild(new ClickableWidget(listViewWidget));
//
//        if(associatedRP == null) {
//            openSaveAsScreen();
//        }
    }

    @Override
    public MutableText getScreenTitle() {
        return TextUtil.translatable(TextCategory.GUI, "pids_preset.pids_editor.title");
    }

    @Override
    public MutableText getScreenSubtitle() {
        return TextUtil.translatable(TextCategory.GUI, "pids_save.subtitle", newPreset.getId());
    }

    public void addConfigEntries() {
        listViewWidget.addCategory(TextUtil.translatable(TextCategory.GUI, "pids_preset.listview.category.save_edits"));
        addResourcePackListing(PIDSManager.getPreset(newPreset.getId()));
    }

    private void addResourcePackListing(PIDSPresetBase preset) {
        if(associatedRP == null) return;

        ButtonWidgetExtension saveBtn = new ButtonWidgetExtension(0, 0, 60, 20, TextUtil.translatable(TextCategory.GUI, "pids_preset.listview.widget.save"), (btn) -> {
            onClose2();
        });

        ButtonWidgetExtension saveAsBtn = new ButtonWidgetExtension(0, 0, 60, 20, TextUtil.translatable(TextCategory.GUI, "pids_preset.listview.widget.saveas"), (btn) -> openSaveAsScreen());

        HorizontalWidgetSet widgetSet = new HorizontalWidgetSet();
        widgetSet.addWidget(new MappedWidget(saveBtn));
        widgetSet.setXYSize(0, 0, 100, 20);

        HorizontalWidgetSet widgetSet2 = new HorizontalWidgetSet();
        widgetSet2.addWidget(new MappedWidget(saveAsBtn));
        widgetSet2.setXYSize(0, 0, 100, 20);

        addChild(new ClickableWidget(saveBtn));
        addChild(new ClickableWidget(saveAsBtn));
        addChild(new ClickableWidget(widgetSet));
        addChild(new ClickableWidget(widgetSet2));
        ContentItem contentItem = new ContentItem(TextUtil.literal(associatedRP), new MappedWidget(widgetSet), 26);
        ContentItem contentItem2 = new ContentItem(TextUtil.translatable(TextCategory.GUI,"pids_preset.listview.widget.title.saveas"), new MappedWidget(widgetSet2), 26);

        contentItem.setIconCallback((guiDrawing, startX, startY, width, height) -> {
            drawPIDSPreview(preset, guiDrawing, startX, startY, width, height, false);
        });
        listViewWidget.add(contentItem);
        listViewWidget.add(contentItem2);
    }

    private void openSaveAsScreen() {
        MinecraftClient.getInstance().openScreen(
                new Screen(new EditorSaveScreenExtended(oldPreset, newPreset).withPreviousScreen(new Screen(this)))
        );
    }

    public static void drawPIDSPreview(PIDSPresetBase preset, GuiDrawing guiDrawing, int startX, int startY, int width, int height, boolean backgroundOnly) {
        final int offset = 6;

        GuiHelper.drawTexture(guiDrawing, PIDS_PREVIEW_BASE, startX, startY, width, height);
        if(preset == null) return;
        
        GuiHelper.drawTexture(guiDrawing, preset.getBackground(), startX+0.5, startY+offset+0.5, width-1, height-offset-4);

        if(!backgroundOnly) {
            double perRow = height / 8.5;
            double rowHeight = Math.max(0.5, height / 24.0);
            for(int i = 0; i < 4; i++) {
                if(preset.isRowHidden(i)) continue;
                double curY = startY + offset + ((i+1) * perRow);
                GuiHelper.drawRectangle(guiDrawing, startX+1.5, curY, width * 0.55, rowHeight, preset.getTextColor());
                GuiHelper.drawRectangle(guiDrawing, startX + (width * 0.65), curY, rowHeight, rowHeight, preset.getTextColor());
                GuiHelper.drawRectangle(guiDrawing, startX + (width * 0.75), curY, (width * 0.2)-0.5, rowHeight, preset.getTextColor());
            }
        }
    }

    private String getAssociatedResourcePack(String targetPresetId) {
        for(File file : resourcePackFolder.listFiles()) {
            if(file.isDirectory()) {
                File jsonFile = resourcePackFolder.toPath().resolve(file.getName()).resolve("assets").resolve(Constants.MOD_ID).resolve("joban_custom_resources.json").toFile();
                if(jsonFile.exists()) {
                    JsonArray presetsInRP;
                    try {
                        presetsInRP = new JsonParser().parse(new FileReader(jsonFile)).getAsJsonObject().get("pids_images").getAsJsonArray();
                    } catch (Exception e) {
                        JCMLogger.debug("Cannot read PIDS Preset from Resource Pack!");
                        continue;
                    }

                    for(int i = 0; i < presetsInRP.size(); i++) {
                        PIDSPresetBase pidsPreset = PIDSManager.parsePreset(presetsInRP.get(i).getAsJsonObject());
                        if(pidsPreset != null && pidsPreset.getId().equals(oldPreset.getId())) {
                            return file.getName();
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void onClose2() {
        if(associatedRP != null) {
            JCMResourceManager.updatePresetInResourcePack(oldPreset, newPreset, associatedRP);
            JCMResourceManager.reloadResources();
        }

        super.onClose2();
    }
}