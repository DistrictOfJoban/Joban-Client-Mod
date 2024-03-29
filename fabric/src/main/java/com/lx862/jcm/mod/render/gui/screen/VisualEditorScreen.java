package com.lx862.jcm.mod.render.gui.screen;

import com.google.gson.*;
import com.lx862.jcm.mod.render.GuiHelper;
import com.lx862.jcm.mod.render.gui.screen.base.ScreenBase;
import com.lx862.jcm.mod.resources.JCMResourceManager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.resource.ResourcePackManager;
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

    MinecraftClient minecraftClient = MinecraftClient.getInstance();
    VertexConsumerProvider.Immediate vertexConsumers = minecraftClient.getBufferBuilders().getEntityVertexConsumers();

    private Boolean isComp = false;

    private PIDSPresetScreen presetScreen;

    private static MinecraftClient client;
    private static File resourcePackFolder;
    private static List<String> categories;
    private static int currentCategoryIndex;
    private static int currentIdIndex;

    private CheckboxWidgetExtension showWeatherCheckbox;
    private CheckboxWidgetExtension showClockCheckbox;
    private CheckboxWidgetExtension[] hideRowCheckboxes = new CheckboxWidgetExtension[4];
    private TextFieldWidgetExtension idTextField;
    private TextFieldWidgetExtension backgroundTextField;
    private TextFieldWidgetExtension colorTextField;

    private static JsonObject jsonObject;

    private static String selectedTemplateId;

    public VisualEditorScreen(MinecraftClient client, String selectedTemplateId, PIDSPresetScreen presetScreen) {
        super();
        this.client = client;
        this.resourcePackFolder = new File(this.client.runDirectory, "resourcepacks");
        this.categories = new ArrayList<>();
        this.currentCategoryIndex = 0;
        this.selectedTemplateId = selectedTemplateId;
        this.presetScreen = presetScreen;
        initializeCategories();
        loadJSON();
        updateCurrentIdIndex();
    }

    private static void updateCurrentIdIndex() {
        if (selectedTemplateId != null && jsonObject != null && jsonObject.has("pids_images")) {
            JsonArray pidsImages = jsonObject.getAsJsonArray("pids_images");
            for (int i = 0; i < pidsImages.size(); i++) {
                JsonObject currentIdObject = pidsImages.get(i).getAsJsonObject();
                if (currentIdObject.has("id") && selectedTemplateId.equals(currentIdObject.get("id").getAsString())) {
                    currentIdIndex = i;
                    break;
                }
            }
        }
    }

    private void reloadJSON() {
        loadJSON();
        currentIdIndex = 0;
        updateCurrentIdIndex();
        updateFieldsBasedOnCurrentId();
    }

    private void renderBackgroundImage(GraphicsHolder context, JsonObject currentIdObject) {
        JsonElement backgroundElement = jsonObject.getAsJsonArray("pids_images").get(currentIdIndex).getAsJsonObject()
                .get("background");

        String backgroundPath = (backgroundElement != null && !backgroundElement.isJsonNull())
                ? backgroundElement.getAsString()
                : "jsblock:missing";

        if (backgroundPath == null || backgroundPath.trim().isEmpty()) {
            backgroundPath = "jsblock:missing";
        }

        String[] pathComponents = backgroundPath.split(":")[1].split("/");

        String namespace = "jsblock";
        StringBuilder pathBuilder = new StringBuilder();
        for (int i = 0; i < pathComponents.length; i++) {
            pathBuilder.append(pathComponents[i]);
            if (i < pathComponents.length - 1) {
                pathBuilder.append("/");
            }
        }
        String path = pathBuilder.toString().toLowerCase();

        org.mtr.mapping.holder.Identifier backgroundIdentifier = new org.mtr.mapping.holder.Identifier(namespace, path);

        String colorHex = currentIdObject.has("color") ? currentIdObject.get("color").getAsString() : "FFFFFF";
        int textColor = Integer.parseInt(colorHex, 16);

        GuiDrawing guiDrawing = new GuiDrawing(context);

        int minecraftGuiScale = MinecraftClient.getInstance().options.getGuiScale().getValue();
        int screenWidth = this.width;
        int screenHeight = this.height;

        int imageWidth = 1920;
        int imageHeight = 1080;
        double aspectRatio = (double) imageWidth / (double) imageHeight;
        int scaledWidth = (int) (screenHeight * aspectRatio * 0.3);
        int scaledHeight = (int) (scaledWidth / aspectRatio);

        int x = (screenWidth / 2) + ((screenWidth / 2) - scaledWidth) / 2;
        int y = (screenHeight - scaledHeight) / 2;

        GuiHelper.drawTexture(guiDrawing, backgroundIdentifier, x, y, scaledWidth, scaledHeight);
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
                    renderText(context, "Station", "3 min", x, y + i * 8, scaledWidth, scaledHeight, textColor);
                }
            }
        }
    }

    private void renderText(GraphicsHolder context, String leftText, String rightText, int backgroundX, int backgroundY,
                            int scaledWidth, int scaledHeight, int textColor) {

        int leftX = backgroundX;
        int rightX = backgroundX + scaledWidth - GraphicsHolder.getTextWidth(leftText);
        int centerY = backgroundY + (scaledHeight - 9) / 2;

        context.drawText(leftText, leftX, centerY, textColor, true, GraphicsHolder.getDefaultLight());
        context.drawText(rightText, rightX, centerY, textColor, true, GraphicsHolder.getDefaultLight());
    }


    private void renderIcon(GraphicsHolder context, String iconName, int backgroundX, int backgroundY, int scaledWidth,
                            int scaledHeight) {
        GuiDrawing guiDrawing = new GuiDrawing(context);
        org.mtr.mapping.holder.Identifier iconIdentifier = new org.mtr.mapping.holder.Identifier("jsblock", "textures/ve/" + iconName);
        int iconX = backgroundX;
        int iconY = backgroundY;
        GuiHelper.drawTexture(guiDrawing, iconIdentifier, iconX, iconY, scaledWidth, scaledHeight);
    }

    private static void initializeCategories() {
        ResourcePackManager resourcePackManager = client.getResourcePackManager();
        Collection<String> activePackNames = resourcePackManager.getEnabledNames();

        File[] files = resourcePackFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {

                    File jsonFile = new File(file, "assets/jsblock/joban_custom_resources.json");
                    if (jsonFile.exists() && activePackNames.contains("file/" + file.getName())) {
                        categories.add(file.getName());
                    }
                } else if (file.isFile() && file.getName().endsWith(".zip")) {

                    if (isZipContainsFile(file, "assets/jsblock/joban_custom_resources.json")
                            && activePackNames.contains("file/" + file.getName())) {
                        categories.add(file.getName());
                    }
                }
            }
        }

        if (categories.isEmpty()) {
            categories.add("No compatible resource packs are loaded");
        }
    }

    public static void RL() {
        initializeCategories();
        loadJSON();
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

    private static void loadJSON() {
        try {
            String categoryName = categories.get(currentCategoryIndex);
            File jsonFile = new File(resourcePackFolder + File.separator + categoryName + File.separator + "assets" +
                    File.separator + "jsblock" + File.separator + "joban_custom_resources.json");
            if (jsonFile.exists()) {
                Gson gson = new Gson();
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

    private static void saveJSON() {
        try {
            String categoryName = categories.get(currentCategoryIndex);
            File jsonFile = new File(resourcePackFolder + File.separator + categoryName + File.separator + "assets" +
                    File.separator + "jsblock" + File.separator + "joban_custom_resources.json");
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

    @Override
    protected void init2() {
        super.init2();

        if (categories.isEmpty() || categories.get(0).equals("No compatible resource packs are loaded")) {
            this.addDrawableChild(
                    new ButtonWidgetExtension(this.width / 2 - 100, this.height - 30, 200, 20, "Back", button -> {
                        this.client.setScreen(null);
                    }));

            isComp = false;

        } else {

            isComp = true;

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

            this.addDrawableChild(
                    new ButtonWidgetExtension(this.width / 2 - 100, this.height - 30, 200, 20, "Back", button -> {
                        saveJSON();
                        this.client.setScreen(presetScreen);
                        JCMResourceManager.reloadResources();
                    }));

            idTextField = new TextFieldWidgetExtension(20, this.height / 6 + 20, 200, 20, Integer.MAX_VALUE, TextCase.DEFAULT, null, null);
            this.addSelectableChild(idTextField);
            idTextField.setText("");
            idTextField.setText(jsonObject.has("pids_images")
                    ? jsonObject.getAsJsonArray("pids_images").get(currentIdIndex).getAsJsonObject().get("id")
                    .getAsString()
                    : "");

            String backgroundValue = "";
            String colorValue = "";
            boolean showClockValue = false;
            boolean showWeatherValue = false;
            JsonArray hideRowValue = new JsonArray();

            JsonArray pidsImages = jsonObject.getAsJsonArray("pids_images");
            if (pidsImages != null && currentIdIndex >= 0 && currentIdIndex < pidsImages.size()) {
                JsonObject currentIdObject = pidsImages.get(currentIdIndex).getAsJsonObject();

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

            backgroundTextField = new TextFieldWidgetExtension(20, this.height / 6 + 45, 200, 20, Integer.MAX_VALUE, TextCase.DEFAULT, null, null);
            this.addSelectableChild(backgroundTextField);
            backgroundTextField.setText("");
            backgroundTextField.setText(backgroundValue);

            colorTextField = new TextFieldWidgetExtension(20, this.height / 6 + 70, 200, 20, Integer.MAX_VALUE, TextCase.DEFAULT, null, null);
            this.addSelectableChild(colorTextField);
            colorTextField.setText("");
            colorTextField.setText(colorValue);

            showWeatherCheckbox = new CheckboxWidgetExtension(20, this.height / 6 + 95, 200, 20,
                    "Show Weather", true, isChecked -> {
                jsonObject.getAsJsonArray("pids_images").get(currentIdIndex).getAsJsonObject()
                        .addProperty("showWeather", isChecked);
            });
            this.addDrawableChild(showWeatherCheckbox);

            showClockCheckbox = new CheckboxWidgetExtension(20, this.height / 6 + 120, 200, 20,
                    "Show Clock", true, isChecked -> {
                jsonObject.getAsJsonArray("pids_images").get(currentIdIndex).getAsJsonObject().addProperty(
                        "showClock",
                        isChecked);
            });
            this.addDrawableChild(showClockCheckbox);

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
                        if (currentIdIndex >= 0 && currentIdIndex < pidsImages.size()) {
                            JsonObject currentIdObject = pidsImages.get(currentIdIndex).getAsJsonObject();
                            if (currentIdObject.has("hideRow")) {
                                JsonArray hideRowArray = currentIdObject.getAsJsonArray("hideRow");
                                if (index >= 0 && index < hideRowArray.size()) {
                                    hideRowArray.set(index, new JsonPrimitive(isChecked));
                                }
                            }
                        }
                    }
                });
                this.addDrawableChild(hideRowCheckboxes[i]);
            }

            updateFieldsBasedOnCurrentId();
        }

        if (selectedTemplateId != null) {
            idTextField.setText(selectedTemplateId);
        }
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
        super.renderBackground(graphicsHolder);

        if (isComp == true) {
            initWidgets(graphicsHolder, mouseX, mouseY, delta);
        }

        super.render(graphicsHolder, mouseX, mouseY, delta);

        if (isComp == true) {

            String categoryName = categories.get(currentCategoryIndex);
            graphicsHolder.drawCenteredText(categoryName, this.width / 2, 20, 0xFFFFFF);

            if (jsonObject != null && jsonObject.has("pids_images")) {
                JsonArray pidsImages = jsonObject.getAsJsonArray("pids_images");
                if (currentIdIndex >= 0 && currentIdIndex < pidsImages.size()) {
                    JsonObject currentIdObject = pidsImages.get(currentIdIndex).getAsJsonObject();
                    String idName = currentIdObject.get("id").getAsString();
                    graphicsHolder.drawCenteredText("Editing Template: " + idName, this.width / 2, this.height / 6, 0xFFFFFF);
                }
            }

            graphicsHolder.drawCenteredText(String.valueOf("Visual PIDS Editor"), 70, this.height - 20, 0xFFFFFF);

            JsonObject currentIdObject = null;
            if (jsonObject != null && jsonObject.has("pids_images")) {
                JsonArray pidsImages = jsonObject.getAsJsonArray("pids_images");
                if (currentIdIndex >= 0 && currentIdIndex < pidsImages.size()) {
                    currentIdObject = pidsImages.get(currentIdIndex).getAsJsonObject();
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
        }

        if (isComp == false) {
            graphicsHolder.drawCenteredText("No compatible resource packs are loaded", this.width / 2, this.height / 2 - 10, 0xFFFFFF);
            graphicsHolder.drawCenteredText(String.valueOf(this.title), 10, this.height - 20, 0xFFFFFF);
        }
    }

    private void initWidgets(GraphicsHolder context, int mouseX, int mouseY, float delta) {
        idTextField.render(context, mouseX, mouseY, delta);
        backgroundTextField.render(context, mouseX, mouseY, delta);
        colorTextField.render(context, mouseX, mouseY, delta);
    }

    private void updateFieldsBasedOnCurrentId() {
        if (jsonObject != null && jsonObject.has("pids_images")) {
            JsonArray pidsImages = jsonObject.getAsJsonArray("pids_images");
            if (currentIdIndex >= 0 && currentIdIndex < pidsImages.size()) {
                JsonObject currentIdObject = pidsImages.get(currentIdIndex).getAsJsonObject();
                idTextField.setText(getStringOrNull(currentIdObject, "id"));
                backgroundTextField.setText(getStringOrNull(currentIdObject, "background"));
                colorTextField.setText(getStringOrNull(currentIdObject, "color"));
                setCheckboxState(showWeatherCheckbox, getBooleanOrNull(currentIdObject, "showWeather"));
                setCheckboxState(showClockCheckbox, getBooleanOrNull(currentIdObject, "showClock"));
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

    private String getStringOrNull(JsonObject jsonObject, String key) {
        if (jsonObject.has(key) && !jsonObject.get(key).isJsonNull()) {
            return jsonObject.get(key).getAsString();
        }
        return "";
    }

    private Boolean getBooleanOrNull(JsonObject jsonObject, String key) {
        if (jsonObject.has(key) && !jsonObject.get(key).isJsonNull()) {
            return jsonObject.get(key).getAsBoolean();
        }
        return false;
    }

    private void setCheckboxState(CheckboxWidgetExtension checkbox, boolean newState) {
        if (checkbox.isChecked() != newState) {
            checkbox.onPress();
        }
    }

    @Override
    public boolean isPauseScreen2() {
        return false;
    }

    @Override
    public void onClose2() {
        saveJSON();
        this.client.setScreen(presetScreen);
        JCMResourceManager.reloadResources();
    }
}