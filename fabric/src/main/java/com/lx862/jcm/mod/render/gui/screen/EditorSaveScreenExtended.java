package com.lx862.jcm.mod.render.gui.screen;

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
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.GuiDrawing;
import org.mtr.mapping.mapper.TextFieldWidgetExtension;
import org.mtr.mapping.tool.TextCase;

import java.io.File;

public class EditorSaveScreenExtended extends TitledScreen implements RenderHelper, GuiHelper {
    private static final Identifier PIDS_PREVIEW_BASE = new Identifier("jsblock:textures/gui/pids_preview.png");
    private final TextFieldWidgetExtension searchBox;
    private final File resourcePackFolder;
    private final ListViewWidget listViewWidget;
    private final PIDSPresetBase newPreset;
    private final PIDSPresetBase oldPreset;
    public EditorSaveScreenExtended(PIDSPresetBase oldPreset, PIDSPresetBase newPreset) {
        super(false);
        this.resourcePackFolder = new File(org.mtr.mapping.holder.MinecraftClient.getInstance().getRunDirectoryMapped(), "resourcepacks");
        this.listViewWidget = new ListViewWidget();
        this.searchBox = new TextFieldWidgetExtension(0, 0, 0, 22, 60, TextCase.DEFAULT, null, TextUtil.translatable(TextCategory.GUI, "widget.search").getString());
        this.newPreset = newPreset;
        this.oldPreset = oldPreset;
    }

    @Override
    protected void init2() {
        super.init2();
        int contentWidth = (int)Math.min((width * 0.75), MAX_CONTENT_WIDTH);
        int listViewHeight = (int)((height - 60) * 0.76);
        int startX = (width - contentWidth) / 2;
        int searchStartY = TEXT_PADDING * 5;
        int startY = searchStartY + (TEXT_PADDING * 3);

        listViewWidget.reset();
        addConfigEntries();
        searchBox.setX2(startX);
        searchBox.setY2(searchStartY);
        searchBox.setWidth2(contentWidth);
        searchBox.setChangedListener2(string -> {
            listViewWidget.setSearchTerm(string);
        });

        listViewWidget.setXYSize(startX, startY, contentWidth, listViewHeight);
        addChild(new ClickableWidget(listViewWidget));
        addChild(new ClickableWidget(searchBox));
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
        if(!PIDSManager.getCustomPresets().isEmpty()) {
            listViewWidget.addCategory(TextUtil.translatable(TextCategory.GUI, "pids_preset.listview.category.save_edits"));
            File[] resourcePacks = resourcePackFolder.listFiles();
            if (resourcePacks != null) {
                for (File pack : resourcePacks) {
                    if (pack.isDirectory()) {
                        File jsonFile = pack.toPath().resolve("assets").resolve(Constants.MOD_ID).resolve("joban_custom_resources.json").toFile();
                        if (jsonFile.exists()) {
                            String packName = pack.getName();
                            addResourcePackListing(packName);
                        }
                    }
                }
            }

            ButtonWidgetExtension exportBtn = new ButtonWidgetExtension(0, 0, 60, 20, TextUtil.translatable(TextCategory.GUI, "pids_preset.listview.widget.export"), (btn) -> {

            });

            HorizontalWidgetSet widgetSet2 = new HorizontalWidgetSet();
            widgetSet2.addWidget(new MappedWidget(exportBtn));
            widgetSet2.setXYSize(0, 0, 100, 20);

            addChild(new ClickableWidget(exportBtn));
            addChild(new ClickableWidget(widgetSet2));

            ContentItem contentItem2 = new ContentItem(TextUtil.translatable(TextCategory.GUI,"pids_preset.listview.widget.new"), new MappedWidget(widgetSet2), 26);

            listViewWidget.add(contentItem2);
        }
    }

    private void addResourcePackListing(String packName) {
        ButtonWidgetExtension saveBtn = new ButtonWidgetExtension(0, 0, 60, 20, TextUtil.translatable(TextCategory.GUI, "pids_preset.listview.widget.save"), (btn) -> {
            JCMResourceManager.updatePresetInResourcePack(oldPreset, newPreset, packName);
            onClose2();
        });

        HorizontalWidgetSet widgetSet = new HorizontalWidgetSet();
        widgetSet.addWidget(new MappedWidget(saveBtn));
        widgetSet.setXYSize(0, 0, 100, 20);

        addChild(new ClickableWidget(saveBtn));
        addChild(new ClickableWidget(widgetSet));
        ContentItem contentItem = new ContentItem(TextUtil.literal(packName), new MappedWidget(widgetSet), 26);

        listViewWidget.add(contentItem);
    }

    public static void drawPIDSPreview(PIDSPresetBase preset, GuiDrawing guiDrawing, int startX, int startY, int width, int height, boolean backgroundOnly) {
        final int offset = 6;

        // Background
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

    @Override
    public void onClose2() {
        JCMResourceManager.reloadResources();
        super.onClose2();
    }
}