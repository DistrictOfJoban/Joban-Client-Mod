package com.lx862.jcm.mod.render.gui.screen;

import com.google.gson.*;
import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.data.pids.PIDSManager;
import com.lx862.jcm.mod.data.pids.preset.JsonPIDSPreset;
import com.lx862.jcm.mod.data.pids.preset.MutableJsonPIDSPreset;
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
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.GuiDrawing;
import org.mtr.mapping.mapper.TextFieldWidgetExtension;
import org.mtr.mapping.tool.TextCase;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class EditorSaveScreen extends TitledScreen implements RenderHelper, GuiHelper {
    private static final Identifier PIDS_PREVIEW_BASE = new Identifier("jsblock:textures/gui/pids_preview.png");
    private final File resourcePackFolder;
    private final ListViewWidget listViewWidget;
    private final String selectedPreset;
    private Object2ObjectOpenHashMap<String, String> presetIdToResourcePack = new Object2ObjectOpenHashMap<>();
    private final List<JsonPIDSPreset> presets;
    private MutableJsonPIDSPreset ourPreset;
    public EditorSaveScreen(MutableJsonPIDSPreset ourPreset, List<JsonPIDSPreset> presets, Object2ObjectOpenHashMap<String, String> presetIdToResourcePack) {
        super(false);
        this.resourcePackFolder = new File(org.mtr.mapping.holder.MinecraftClient.getInstance().getRunDirectoryMapped(), "resourcepacks");
        this.listViewWidget = new ListViewWidget();
        this.ourPreset = ourPreset;
        this.selectedPreset = ourPreset.getId();
        this.presets = presets;
        this.presetIdToResourcePack = presetIdToResourcePack;
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
    }

    @Override
    public MutableText getScreenTitle() {
        return TextUtil.translatable(TextCategory.GUI, "pids_preset.pids_editor.title");
    }

    @Override
    public MutableText getScreenSubtitle() {
        return TextUtil.translatable(TextCategory.GUI, "pids_save.subtitle", selectedPreset);
    }

    public void addConfigEntries() {
        if(!PIDSManager.getCustomPresets().isEmpty()) {
            listViewWidget.addCategory(TextUtil.translatable(TextCategory.GUI, "pids_preset.listview.category.save_edits"));
            addPreset(PIDSManager.getPreset(selectedPreset));
        }
    }

    private void addPreset(PIDSPresetBase preset) {
        ButtonWidgetExtension saveBtn = new ButtonWidgetExtension(0, 0, 60, 20, TextUtil.translatable(TextCategory.GUI, "pids_preset.listview.widget.save"), (btn) -> {
            onClose2();
        });

        ButtonWidgetExtension saveAsBtn = new ButtonWidgetExtension(0, 0, 60, 20, TextUtil.translatable(TextCategory.GUI, "pids_preset.listview.widget.saveas"), (btn) -> {
            MinecraftClient.getInstance().openScreen(
                    new Screen(new EditorSaveScreenExtended(ourPreset, presets, presetIdToResourcePack).withPreviousScreen(new Screen(this)))
            );
        });

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
        ContentItem contentItem = new ContentItem(TextUtil.literal(preset.getName()), new MappedWidget(widgetSet), 26);
        ContentItem contentItem2 = new ContentItem(TextUtil.translatable(TextCategory.GUI,"pids_preset.listview.widget.title.saveas"), new MappedWidget(widgetSet2), 26);

        contentItem.setIconCallback((guiDrawing, startX, startY, width, height) -> {
            drawPIDSPreview(preset, guiDrawing, startX, startY, width, height, false);
        });
        listViewWidget.add(contentItem);
        listViewWidget.add(contentItem2);
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
    private void saveJSON() {
        try {
            String packName = presetIdToResourcePack.get(selectedPreset);
            List<MutableJsonPIDSPreset> packPresets = presets.stream().filter(e -> presetIdToResourcePack.get(e.getId()).equals(packName)).map(e -> e.toMutable()).collect(Collectors.toList());
            for(int i = 0; i < packPresets.size(); i++) {
                MutableJsonPIDSPreset preset = packPresets.get(i);
                if(preset.getId().equals(selectedPreset)) {
                    packPresets.set(i, ourPreset);
                }
            }

            File jsonFile = resourcePackFolder.toPath().resolve(packName).resolve("assets").resolve(Constants.MOD_ID).resolve("joban_custom_resources.json").toFile();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            FileWriter writer = new FileWriter(jsonFile);
            JsonObject rootObject = new JsonObject();
            JsonArray presetArray = new JsonArray();
            for(MutableJsonPIDSPreset preset : packPresets) {
                presetArray.add(preset.toJson());
            }
            rootObject.add("pids_images", presetArray);
            gson.toJson(rootObject, writer);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClose2() {
        saveJSON();
        JCMResourceManager.reloadResources();
        super.onClose2();
    }

    @Override
    public boolean isPauseScreen2() {
        return false;
    }
}