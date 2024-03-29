package com.lx862.jcm.mod.render.gui.screen;

import com.google.gson.*;
import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.render.GuiHelper;
import com.lx862.jcm.mod.render.gui.screen.base.ScreenBase;
import com.lx862.jcm.mod.resources.JCMResourceManager;

import net.minecraft.resource.ResourcePackManager;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.mapper.*;
import org.mtr.mapping.tool.TextCase;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class VisualEditorScreen extends ScreenBase {
    private final List<String> resourcePacks = new ArrayList<>();
    private final int currentPackIndex = 0;
    private final String selectedPresetId;
    private final File resourcePackFolder;
    private final CheckboxWidgetExtension[] hideRowCheckboxes = new CheckboxWidgetExtension[4];
    private final TextFieldWidgetExtension idTextField;
    private final TextFieldWidgetExtension backgroundTextField;
    private final CheckboxWidgetExtension showWeatherCheckbox;
    private final CheckboxWidgetExtension showClockCheckbox;
    private TextFieldWidgetExtension colorTextField;
    private JsonObject jsonObject;
    private int currentPresetIndex;

    public VisualEditorScreen(String selectedPresetId) {
        super();
        initializeCategories();
        loadJSON();
        updateCurrentIdIndex();

        this.resourcePackFolder = new File(org.mtr.mapping.holder.MinecraftClient.getInstance().getRunDirectoryMapped(), "resourcepacks");
        this.selectedPresetId = selectedPresetId;

        this.backgroundTextField = new TextFieldWidgetExtension(20, 0, 200, 20, Integer.MAX_VALUE, TextCase.DEFAULT, null, null);
        this.idTextField = new TextFieldWidgetExtension(20, this.height / 6 + 20, 200, 20, Integer.MAX_VALUE, TextCase.DEFAULT, null, null);
        this.showWeatherCheckbox = new CheckboxWidgetExtension(20, 0, 200, 20,  "Show Weather", true, isChecked -> {
            jsonObject.getAsJsonArray("pids_images").get(currentPresetIndex).getAsJsonObject().addProperty("showWeather", isChecked);
        });
        this.showClockCheckbox = new CheckboxWidgetExtension(20, this.height / 6 + 120, 200, 20, "Show Clock", true, isChecked -> {
            jsonObject.getAsJsonArray("pids_images").get(currentPresetIndex).getAsJsonObject().addProperty("showClock", isChecked);
        });
    }

    @Override
    protected void init2() {
        super.init2();
        // for full pids gui
        // this.addDrawableChild(ButtonWidget.builder(Text.of("<"), button -> {
        // currentCategoryIndex = (currentCategoryIndex - 1 + categories.size()) %
        // categories.size();
        // reloadJSON();
        // })
        // .dimensions(10, 10, 50, 20)
        // .build());
        // this.addDrawableChild(ButtonWidget.builder(Text.of(">"), button -> {
        // currentCategoryIndex = (currentCategoryIndex + 1) % categories.size();
        // reloadJSON();
        // })
        // .dimensions(this.width - 60, 10, 50, 20)
        // .build());

        addChild(
                new ClickableWidget(
                        new ButtonWidgetExtension(this.width / 2 - 100, this.height - 30, 200, 20, "Back", button -> {
                            onClose2();
                        })
                )
        );

        idTextField.setX2(20);
        idTextField.setY2(this.height / 6 + 20);
        idTextField.setWidth2(200);
        addChild(new ClickableWidget(idTextField));
        idTextField.setText2(jsonObject.getAsJsonArray("pids_images").get(currentPresetIndex).getAsJsonObject().get("id")
                .getAsString());

        String backgroundValue = "";
        String colorValue = "";
        boolean showClockValue = false;
        boolean showWeatherValue = false;
        JsonArray hideRowValue = new JsonArray();

        JsonArray pidsImages = jsonObject.getAsJsonArray("pids_images");
        if (pidsImages != null && currentPresetIndex >= 0 && currentPresetIndex < pidsImages.size()) {
            JsonObject currentIdObject = pidsImages.get(currentPresetIndex).getAsJsonObject();

            JsonElement backgroundElement = currentIdObject.get("background");
            if (backgroundElement != null && !backgroundElement.isJsonNull()) {
                backgroundValue = backgroundElement.getAsString();
            }

            JsonElement colorElement = currentIdObject.get("color");
            if (colorElement != null && !colorElement.isJsonNull()) {
                colorValue = colorElement.getAsString();
            }

            JsonElement showClockElement = currentIdObject.get("showClock");
            if (showClockElement != null && !showClockElement.isJsonNull()) {
                showClockValue = showClockElement.getAsBoolean();
            }

            JsonElement showWeatherElement = currentIdObject.get("showWeather");
            if (showWeatherElement != null && !showWeatherElement.isJsonNull()) {
                showWeatherValue = showWeatherElement.getAsBoolean();
            }

            JsonElement hideRowElement = currentIdObject.get("hideRow");
            if (hideRowElement != null && !hideRowElement.isJsonNull()) {
                hideRowValue = hideRowElement.getAsJsonArray();
            }
        }

        backgroundTextField.setY2(this.height / 6 + 45);
        backgroundTextField.setText2(backgroundValue);
        addChild(new ClickableWidget(backgroundTextField));

        colorTextField = new TextFieldWidgetExtension(20, this.height / 6 + 70, 200, 20, Integer.MAX_VALUE, TextCase.DEFAULT, null, null);
        colorTextField.setText2(colorValue);
        addChild(new ClickableWidget(colorTextField));


        showWeatherCheckbox.setY2(this.height / 6 + 95);
        addChild(new ClickableWidget(showWeatherCheckbox));

        showClockCheckbox.setY2(this.height / 6 + 120);
        addChild(new ClickableWidget(showClockCheckbox));

        // for full pids gui
        // this.addDrawableChild(ButtonWidget.builder(Text.of("<"), button -> {
        // currentIdIndex = (currentIdIndex - 1 +
        // jsonObject.getAsJsonArray("pids_images").size())
        // % jsonObject.getAsJsonArray("pids_images").size();
        // updateFieldsBasedOnCurrentId();
        // })
        // .dimensions(10, this.height / 6 - 5, 20, 20)
        // .build());
        // this.addDrawableChild(
        // ButtonWidget.builder(Text.of(">"), button -> {
        // currentIdIndex = (currentIdIndex + 1) %
        // jsonObject.getAsJsonArray("pids_images").size();
        // updateFieldsBasedOnCurrentId();
        // })
        // .dimensions(this.width - 30, this.height / 6 - 5, 20, 20)
        // .build());

        int totalWidth = hideRowCheckboxes.length * 30;
        int horizontalGap = (this.width - totalWidth) / (hideRowCheckboxes.length + 1);
        int startX = horizontalGap;

        for (int i = 0; i < hideRowCheckboxes.length; i++) {
            int x = startX + i * (30 + horizontalGap) - 15;
            int y = this.height - 60;
            final int index = i;
            hideRowCheckboxes[i] = new CheckboxWidgetExtension(x, y, 20, 20, "Hide Row " + (i + 1), true, isChecked -> {
                if (jsonObject != null && jsonObject.has("pids_images")) {
                    if (currentPresetIndex >= 0 && currentPresetIndex < pidsImages.size()) {
                        JsonObject currentIdObject = pidsImages.get(currentPresetIndex).getAsJsonObject();
                        if (currentIdObject.has("hideRow")) {
                            JsonArray hideRowArray = currentIdObject.getAsJsonArray("hideRow");
                            if (index < hideRowArray.size()) {
                                hideRowArray.set(index, new JsonPrimitive(isChecked));
                            }
                        }
                    }
                }
            });
            addChild(new ClickableWidget(hideRowCheckboxes[i]));
        }
        populateCurrentPresetFields();

        if (selectedPresetId != null) {
            idTextField.setText2(selectedPresetId);
        }
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
        super.renderBackground(graphicsHolder); // TODO: Why is this needed?

//        backgroundTextField.render(graphicsHolder, mouseX, mouseY, delta);
        colorTextField.render(graphicsHolder, mouseX, mouseY, delta);

        String packName = resourcePacks.get(currentPackIndex);
        graphicsHolder.drawCenteredText(packName, this.width / 2, 20, 0xFFFFFF);

        if (jsonObject != null && jsonObject.has("pids_images")) {
            JsonArray pidsImages = jsonObject.getAsJsonArray("pids_images");
            if (currentPresetIndex >= 0 && currentPresetIndex < pidsImages.size()) {
                JsonObject currentIdObject = pidsImages.get(currentPresetIndex).getAsJsonObject();
                String idName = currentIdObject.get("id").getAsString();
                graphicsHolder.drawCenteredText("Editing Preset: " + idName, this.width / 2, this.height / 6, 0xFFFFFF);
            }
        }

        graphicsHolder.drawCenteredText("Visual PIDS Editor", 70, this.height - 20, 0xFFFFFF);

        JsonObject currentIdObject = null;
        if (jsonObject != null && jsonObject.has("pids_images")) {
            JsonArray pidsImages = jsonObject.getAsJsonArray("pids_images");
            if (currentPresetIndex >= 0 && currentPresetIndex < pidsImages.size()) {
                currentIdObject = pidsImages.get(currentPresetIndex).getAsJsonObject();
            }
        }

        renderBackgroundImage(graphicsHolder, currentIdObject);

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

        super.render(graphicsHolder, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen2() {
        return false;
    }

    @Override
    public void onClose2() {
        saveJSON();
        JCMResourceManager.reloadResources();
        super.onClose2();
    }

    private void populateCurrentPresetFields() {
        if (jsonObject != null && jsonObject.has("pids_images")) {
            JsonArray pidsImages = jsonObject.getAsJsonArray("pids_images");
            if (currentPresetIndex >= 0 && currentPresetIndex < pidsImages.size()) {
                JsonObject currentIdObject = pidsImages.get(currentPresetIndex).getAsJsonObject();
                idTextField.setText2(getStringOrNull(currentIdObject, "id"));
                backgroundTextField.setText2(getStringOrNull(currentIdObject, "background"));
                colorTextField.setText2(getStringOrNull(currentIdObject, "color"));
                setCheckboxState(showWeatherCheckbox, getBoolean(currentIdObject, "showWeather"));
                setCheckboxState(showClockCheckbox, getBoolean(currentIdObject, "showClock"));
                if (currentIdObject.has("hideRow")) {
                    JsonArray hideRowArray = currentIdObject.getAsJsonArray("hideRow");
                    for (int i = 0; i < hideRowCheckboxes.length; i++) {
                        Boolean hideRow = null;
                        if (i < hideRowArray.size()) {
                            hideRow = hideRowArray.get(i).getAsBoolean();
                        }
                        setCheckboxState(hideRowCheckboxes[i], hideRow != null ? hideRow : false);
                    }
                } else {
                    for (CheckboxWidgetExtension checkbox : hideRowCheckboxes) {
                        setCheckboxState(checkbox, false);
                    }
                }
            }
        }
    }

    private void updateCurrentIdIndex() {
        if (selectedPresetId != null && jsonObject != null && jsonObject.has("pids_images")) {
            JsonArray pidsImages = jsonObject.getAsJsonArray("pids_images");
            for (int i = 0; i < pidsImages.size(); i++) {
                JsonObject currentIdObject = pidsImages.get(i).getAsJsonObject();
                if (selectedPresetId.equals(currentIdObject.get("id").getAsString())) {
                    currentPresetIndex = i;
                    break;
                }
            }
        }
    }

    private void renderBackgroundImage(GraphicsHolder context, JsonObject currentIdObject) {
        JsonObject preset = jsonObject.getAsJsonArray("pids_images").get(currentPresetIndex).getAsJsonObject();
        Identifier backgroundId = preset.has("background") ? new Identifier(preset.get("background").getAsString()) : new Identifier(Constants.MOD_ID, "missing");
        String textColorHex = currentIdObject.has("color") ? currentIdObject.get("color").getAsString() : "000000";
        int textColor = Integer.parseInt(textColorHex, 16);

        GuiDrawing guiDrawing = new GuiDrawing(context);
        double minecraftGuiScale = MinecraftClient.getInstance().getWindow().getScaleFactor();
        int screenWidth = this.width;
        int screenHeight = this.height;

        int imageWidth = 1920;
        int imageHeight = 1080;
        double aspectRatio = (double) imageWidth / (double) imageHeight;
        int scaledWidth = (int) (screenHeight * aspectRatio * 0.3);
        int scaledHeight = (int) (scaledWidth / aspectRatio);

        int x = (screenWidth / 2) + ((screenWidth / 2) - scaledWidth) / 2;
        int y = (screenHeight - scaledHeight) / 2;

        GuiHelper.drawTexture(guiDrawing, backgroundId, x, y, scaledWidth, scaledHeight);
        String previewText = "Preview";
        int textWidth = this.textRenderer.getWidth(previewText);
        int textX = x + (scaledWidth - textWidth) / 2;
        int textY = y - 20;

        context.drawCenteredText(previewText, textX, textY, 0xFFFFFF);

        if (currentIdObject.has("showWeather") && currentIdObject.get("showWeather").getAsBoolean()) {
            renderIcon(context, "weather.png", x, y, scaledWidth, scaledHeight);
        }

        if (currentIdObject.has("showClock") && currentIdObject.get("showClock").getAsBoolean()) {
            renderIcon(context, "clock.png", x, y, scaledWidth, scaledHeight);
        }

        JsonElement hideRowElement = currentIdObject.get("hideRow");
        if (hideRowElement != null && !hideRowElement.isJsonNull()) {
            JsonArray hideRowArray = hideRowElement.getAsJsonArray();

            for (int i = 0; i < hideRowArray.size(); i++) {
                boolean hideRow = hideRowArray.get(i).getAsBoolean();
                if (!hideRow) {
                    renderText(context, "Station", "3 min", x, y + (i * 8), scaledWidth, scaledHeight, textColor);
                }
            }
        }
    }

    private void renderText(GraphicsHolder context, String leftText, String rightText, int backgroundX, int backgroundY, int scaledWidth, int scaledHeight, int textColor) {
        int leftX = backgroundX;
        int rightX = backgroundX + scaledWidth - GraphicsHolder.getTextWidth(leftText);
        int centerY = backgroundY + (scaledHeight - 9) / 2;

        context.drawText(leftText, leftX, centerY, textColor, true, GraphicsHolder.getDefaultLight());
        context.drawText(rightText, rightX, centerY, textColor, true, GraphicsHolder.getDefaultLight());
    }

    private void renderIcon(GraphicsHolder context, String iconName, int backgroundX, int backgroundY, int scaledWidth, int scaledHeight) {
        GuiDrawing guiDrawing = new GuiDrawing(context);
        Identifier iconIdentifier = new Identifier(Constants.MOD_ID, "textures/ve/" + iconName);
        int iconX = backgroundX;
        int iconY = backgroundY;
        GuiHelper.drawTexture(guiDrawing, iconIdentifier, iconX, iconY, scaledWidth, scaledHeight);
    }

    private void initializeCategories() {
        ResourcePackManager resourcePackManager = MinecraftClient.getInstance().getResourcePackManager();
        Collection<String> activePackNames = resourcePackManager.getEnabledNames();

        File[] files = resourcePackFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    File jsonFile = new File(file, "assets/jsblock/joban_custom_resources.json");
                    if (jsonFile.exists() && activePackNames.contains("file/" + file.getName())) {
                        resourcePacks.add(file.getName());
                    }
                } else if (file.isFile() && file.getName().endsWith(".zip")) {

                    if (isZipContainsFile(file, "assets/jsblock/joban_custom_resources.json")
                            && activePackNames.contains("file/" + file.getName())) {
                        resourcePacks.add(file.getName());
                    }
                }
            }
        }

        if (resourcePacks.isEmpty()) {
            resourcePacks.add("No compatible resource packs are loaded");
        }
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

    private String getStringOrNull(JsonObject jsonObject, String key) {
        if (jsonObject.has(key) && !jsonObject.get(key).isJsonNull()) {
            return jsonObject.get(key).getAsString();
        }
        return "";
    }

    private boolean getBoolean(JsonObject jsonObject, String key) {
        if (jsonObject.has(key) && !jsonObject.get(key).isJsonNull()) {
            return jsonObject.get(key).getAsBoolean();
        }
        return false;
    }

    private void setCheckboxState(CheckboxWidgetExtension checkbox, boolean newState) {
        if (checkbox.isChecked2() != newState) {
            checkbox.setChecked(!checkbox.isChecked2());
        }
    }

    private void loadJSON() {
        try {
            String rpName = resourcePacks.get(currentPackIndex);
            File jsonFile = resourcePackFolder.toPath().resolve(rpName).resolve("assets").resolve(Constants.MOD_ID).resolve("joban_custom_resources.json").toFile();
            if (jsonFile.exists()) {
                JsonParser parser = new JsonParser();
                JsonElement jsonElement = parser.parse(new FileReader(jsonFile));
                if (jsonElement.isJsonObject()) {
                    jsonObject = jsonElement.getAsJsonObject();

                    if (!jsonObject.has("pids_images")) {
                        jsonObject.add("pids_images", new JsonArray());
                    }
                }
            } else {
                Gson gson = new Gson();
                String defaultJsonString = "{\n" +
                        "  \"pids_images\": [\n" +
                        "    {\n" +
                        "      \"id\": \" \",\n" +
                        "      \"showWeather\": false,\n" +
                        "      \"showClock\": false,\n" +
                        "      \"hideRow\": [\n" +
                        "        false,\n" +
                        "        false,\n" +
                        "        false,\n" +
                        "        false\n" +
                        "      ],\n" +
                        "      \"customTextPushArrival\": true,\n" +
                        "      \"background\": \" \",\n" +
                        "      \"color\": \" \"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";
                jsonObject = gson.fromJson(defaultJsonString, JsonObject.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveJSON() {
        try {
            String categoryName = resourcePacks.get(currentPackIndex);
            File jsonFile = resourcePackFolder.toPath().resolve(categoryName).resolve("assets").resolve(Constants.MOD_ID).resolve("joban_custom_resources.json").toFile();
            if (!jsonFile.exists()) {
                jsonFile.createNewFile();
            }
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            FileWriter writer = new FileWriter(jsonFile);
            gson.toJson(jsonObject, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}