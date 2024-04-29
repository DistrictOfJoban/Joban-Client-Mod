package com.lx862.jcm.mod.render.gui.screen;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.data.pids.PIDSManager;
import com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase;
import com.lx862.jcm.mod.render.GuiHelper;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.render.gui.screen.base.TitledScreen;
import com.lx862.jcm.mod.render.gui.widget.ContentItem;
import com.lx862.jcm.mod.render.gui.widget.ListViewWidget;
import com.lx862.jcm.mod.render.gui.widget.MappedWidget;
import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.TextFieldWidgetExtension;
import org.mtr.mapping.tool.TextCase;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ExportScreen extends TitledScreen implements RenderHelper, GuiHelper {
    private final File resourcePackFolder;
    private final ListViewWidget listViewWidget;
    private final PIDSPresetBase oldPreset;
    private final PIDSPresetBase newPreset;
    private final String associatedRP;
    public ExportScreen(PIDSPresetBase oldPreset, PIDSPresetBase newPreset) {
        super(false);
        this.resourcePackFolder = new File(MinecraftClient.getInstance().getRunDirectoryMapped(), "resourcepacks");
        this.listViewWidget = new ListViewWidget();
        this.newPreset = newPreset;
        this.oldPreset = oldPreset;
        this.associatedRP = getAssociatedResourcePack(this.oldPreset.getId());
    }

    @Override
    protected void init2() {
        super.init2();
        int contentWidth = (int)Math.min((width * 0.75), MAX_CONTENT_WIDTH);
        int listViewHeight = (int)(6 * 25.4);
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
        return TextUtil.translatable(TextCategory.GUI, "pids_save.subtitle", newPreset.getId());
    }

    public void addConfigEntries() {
        listViewWidget.addCategory(TextUtil.translatable(TextCategory.GUI, "pids_preset.listview.widget.new2"));
        addResourcePackListing(PIDSManager.getPreset(newPreset.getId()));
    }

    private void addResourcePackListing(PIDSPresetBase preset) {
        if(associatedRP == null) return;

        TextFieldWidgetExtension textField1 = new TextFieldWidgetExtension(0, 0, 160, 20, "", 100, TextCase.DEFAULT, null, TextUtil.translatable(TextCategory.GUI, "pids_preset.listview.widget.field.tooltip.rpname").getString());
        TextFieldWidgetExtension textField2 = new TextFieldWidgetExtension(0, 0, 160, 20, "", 100, TextCase.DEFAULT, null, TextUtil.translatable(TextCategory.GUI, "pids_preset.listview.widget.field.tooltip.rpdesc").getString());
        TextFieldWidgetExtension textField3 = new TextFieldWidgetExtension(0, 0, 160, 20, "", 100, TextCase.DEFAULT, null, TextUtil.translatable(TextCategory.GUI, "pids_preset.listview.widget.field.tooltip.rpid").getString());

        textField3.setText2(this.oldPreset.getId());

        ButtonWidgetExtension exportBtn = new ButtonWidgetExtension(0, 0, 60, 20, TextUtil.translatable(TextCategory.GUI, "pids_preset.listview.widget.export2"), (btn) -> {
            export(textField1, textField2, textField3);
            onClose2();
        });

        ButtonWidgetExtension backBtn = new ButtonWidgetExtension(0, 0, 60, 20, TextUtil.translatable(TextCategory.GUI, "pids_preset.listview.widget.back"), (btn) -> {
            onClose2();
        });

        addChild(new ClickableWidget(textField1));
        addChild(new ClickableWidget(textField2));
        addChild(new ClickableWidget(textField3));
        addChild(new ClickableWidget(exportBtn));
        addChild(new ClickableWidget(backBtn));

        ContentItem contentItem = new ContentItem(TextUtil.translatable(TextCategory.GUI,"pids_preset.listview.widget.field.rpname"), new MappedWidget(textField1), 26);
        ContentItem contentItem1 = new ContentItem(TextUtil.translatable(TextCategory.GUI,"pids_preset.listview.widget.field.rpdesc"), new MappedWidget(textField2), 26);
        ContentItem contentItem2 = new ContentItem(TextUtil.translatable(TextCategory.GUI,"pids_preset.listview.widget.field.rpid"), new MappedWidget(textField3), 26);
        ContentItem contentItem3 = new ContentItem(TextUtil.translatable(TextCategory.GUI,"pids_preset.listview.widget.title.export"), new MappedWidget(exportBtn), 26);
        ContentItem contentItem4 = new ContentItem(TextUtil.translatable(TextCategory.GUI,"pids_preset.listview.widget.title.back"), new MappedWidget(backBtn), 26);

        listViewWidget.add(contentItem);
        listViewWidget.add(contentItem1);
        listViewWidget.add(contentItem2);
        listViewWidget.add(contentItem3);
        listViewWidget.add(contentItem4);
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

    public void export(TextFieldWidgetExtension textField1, TextFieldWidgetExtension textField2, TextFieldWidgetExtension textField3) {
        String rpName = textField1.getText2();
        String rpDesc = textField2.getText2();
        String rpID = textField3.getText2();
        int packFormat = 26;

        File rpFolder = new File(resourcePackFolder, rpName);
        if (!rpFolder.exists()) {
            rpFolder.mkdirs();
        }

        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", packFormat);
        pack.addProperty("description", rpDesc);

        JsonObject packMeta = new JsonObject();
        packMeta.add("pack", pack);

        File packMcmeta = new File(rpFolder, "pack.mcmeta");
        try {
            FileWriter writer = new FileWriter(packMcmeta);
            new GsonBuilder().setPrettyPrinting().create().toJson(packMeta, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        File assetsFolder = new File(rpFolder, "assets");
        File modFolder = new File(assetsFolder, Constants.MOD_ID);
        File customResourcesJson = new File(modFolder, "joban_custom_resources.json");
        if (!modFolder.exists()) {
            modFolder.mkdirs();
        }

        JsonArray presetsArray = new JsonArray();
        JsonObject presetObject = newPreset.toJson();
        presetObject.addProperty("id", textField3.getText2());
        presetObject.addProperty("name", textField3.getText2());
        presetsArray.add(presetObject);
        JsonObject rootObject = new JsonObject();
        rootObject.add("pids_images", presetsArray);
        try {
            FileWriter writer = new FileWriter(customResourcesJson);
            new GsonBuilder().setPrettyPrinting().create().toJson(rootObject, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        copyResources(associatedRP, rpFolder);
    }

    private void copyResources(String oldRPName, File newRpFolder) {
        File oldRPFolder = new File(resourcePackFolder, oldRPName);
        File oldAssetsFolder = new File(oldRPFolder, "assets");
        File oldModFolder = new File(oldAssetsFolder, Constants.MOD_ID);

        String backgroundPath = oldPreset.getBackground().getNamespace() + ":" + oldPreset.getBackground().getPath();
        String[] folders = backgroundPath.split(":");

        File destFolder = new File(newRpFolder, "assets");
        for (int i = 0; i < folders.length - 1; i++) {
            destFolder = new File(destFolder, folders[i]);
        }

        if (folders.length > 0) {
            copyResources(oldModFolder, destFolder, folders[folders.length - 1]);
        } else {
            JCMLogger.error("Invalid background path: " + backgroundPath);
        }
    }

    private void copyResources(File sourceFolder, File destFolder, String fileName) {
        File sourceFile = new File(sourceFolder, fileName);
        File destFile = new File(destFolder, fileName);
        if (!destFile.getParentFile().exists()) {
            destFile.getParentFile().mkdirs();
        }
        try {
            Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            JCMLogger.error("Failed to copy resource: " + sourceFile + " to " + destFile, e);
        }
    }
}