package com.lx862.mtrscripting.mod.gui.screen;

import com.lx862.jcm.mod.registry.Networking;
import com.lx862.jcm.mod.render.GuiHelper;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.render.gui.screen.base.SavableScreen;
import com.lx862.jcm.mod.render.gui.widget.*;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import com.lx862.mtrscripting.mod.gui.screen.widget.ConfigIntegerField;
import com.lx862.mtrscripting.mod.gui.screen.widget.ConfigTextField;
import com.lx862.mtrscripting.mod.gui.screen.widget.ValidatableWidget;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.config.EyecandyCustomConfig;
import com.lx862.mtrscripting.mod.network.packet.EyecandyCustomConfigUpdateC2SPacket;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.TextFieldWidgetExtension;
import org.mtr.mapping.tool.TextCase;

import java.util.*;

public class EyecandyCustomConfigScreen extends SavableScreen implements RenderHelper, GuiHelper {
    private final ListViewWidget listViewWidget;

    private final Map<String, String> storedConfigEntries; // Parent
    private final Map<String, String> storedConfigEntriesPending; // Local Draft
    private final EyecandyCustomConfig customConfig;
    private final BlockPos blockPos;
    private final String eyecandyName;
    private final String eyecandyId;
    private final List<ValidatableWidget> validatableWidgets;

    public EyecandyCustomConfigScreen(String eyecandyName, String eyecandyId, BlockPos blockPos, Map<String, String> storedConfigEntries, EyecandyCustomConfig customConfig) {
        super(false);
        this.blockPos = blockPos;
        this.eyecandyId = eyecandyId;
        this.eyecandyName = eyecandyName;
        this.storedConfigEntries = storedConfigEntries;
        this.storedConfigEntriesPending = new HashMap<>(storedConfigEntries);
        this.validatableWidgets = new ArrayList<>();
        this.customConfig = customConfig;
        this.listViewWidget = new ListViewWidget();
    }

    @Override
    protected void init2() {
        super.init2();
        int contentWidth = (int)Math.min((width * 0.75), MAX_CONTENT_WIDTH);
        int listViewHeight = (int)((height - 60) * 0.76);
        int startX = (width - contentWidth) / 2;
        int startY = TEXT_PADDING * 5;

        validatableWidgets.clear();
        listViewWidget.reset();
        addConfigEntries();

        listViewWidget.setXYSize(startX, startY, contentWidth, listViewHeight);
        addChild(new ClickableWidget(listViewWidget));

        addBottomRowEntry(startX, startY + listViewHeight + 6, contentWidth, getHeightMapped() - (startY + listViewHeight));
    }

    @Override
    public MutableText getScreenTitle() {
        return TextUtil.translatable("Eyecandy Custom Config");
    }

    @Override
    public MutableText getScreenSubtitle() {
        return TextUtil.translatable(eyecandyName);
    }

    /* Validate and enable/disable save button as necessary*/
    @Override
    public void tick2() {
        boolean allValidated = true;
        for(ValidatableWidget validatableWidget : validatableWidgets) {
            if(!validatableWidget.validated()) {
                allValidated = false;
            }
        }
        saveButton.setActiveMapped(allValidated);
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta) {
        super.render(graphicsHolder, mouseX, mouseY, tickDelta);

        #if MC_VERSION >= "11903"
        if(saveButton.isHovered()) {
            setTooltip(List.of(TextUtil.translatable(TextCategory.GUI, "eyecandy.listview.widget.not_validated").data.asOrderedText()));
        }
        #endif
    }

    public void addConfigEntries() {
        if(customConfig != null) {
            if(customConfig.description() != null) {
                DescriptionItem descriptionItem = new DescriptionItem(TextUtil.translatable(customConfig.description()), (int)Math.min((width * 0.75), MAX_CONTENT_WIDTH));
                listViewWidget.add(descriptionItem);
            }

            for(EyecandyCustomConfig.Entry entry : customConfig.entries()) {
                addEntry(entry);
            }
        } else {
            listViewWidget.addCategory(TextUtil.translatable("Unknown key"));
            for(String key : storedConfigEntriesPending.keySet()) {
                addUnknownEntry(key);
            }
        }
    }

    private void addEntry(EyecandyCustomConfig.Entry entry) {
        HorizontalWidgetSet widgetSet = new HorizontalWidgetSet();

        String originalValue = this.storedConfigEntries.get(entry.id());
        String currentValue = this.storedConfigEntriesPending.get(entry.id());
        String defaultValue = entry.defaultValue() == null ? "" : entry.defaultValue();

        if(entry.type() == EyecandyCustomConfig.Entry.Type.STRING) {
            ConfigTextField textField = new ConfigTextField(0, 0, 120, 20, 600, TextCase.DEFAULT, null, null, entry.validationCallback());
            textField.onChange((newStr) -> {
                this.storedConfigEntriesPending.put(entry.id(), newStr);
            });
            textField.setOriginalValue(originalValue);
            textField.setText2(currentValue == null ? defaultValue : currentValue);
            widgetSet.addWidget(new MappedWidget(textField));
            addChild(new ClickableWidget(textField));
            validatableWidgets.add(textField);
        } else if(entry.type() == EyecandyCustomConfig.Entry.Type.INTEGER) {
            ConfigIntegerField textField = new ConfigIntegerField(0, 0, 120, 20, entry.validationCallback());
            textField.onChange((newStr) -> {
                this.storedConfigEntriesPending.put(entry.id(), newStr);
            });
            textField.setOriginalValue(parseInt(originalValue, 0));
            textField.setValue(parseInt(originalValue, 0));
            widgetSet.addWidget(new MappedWidget(textField));
            addChild(new ClickableWidget(textField));
            validatableWidgets.add(textField);
        }

        widgetSet.setXYSize(0, 0, 120, 20);

        addChild(new ClickableWidget(widgetSet));
        ContentItem contentItem = new ContentItem(TextUtil.translatable(entry.name()), new MappedWidget(widgetSet), 26);

        listViewWidget.add(contentItem);
    }

    private void addUnknownEntry(String key) {
        HorizontalWidgetSet widgetSet = new HorizontalWidgetSet();

        TextFieldWidgetExtension textFieldWidgetExtension = new TextFieldWidgetExtension(0, 0, 120, 20, 600, TextCase.DEFAULT, null, null);
        textFieldWidgetExtension.setChangedListener2((newStr) -> {
            this.storedConfigEntriesPending.put(key, newStr);
        });
        textFieldWidgetExtension.setText2(this.storedConfigEntriesPending.get(key));

        widgetSet.addWidget(new MappedWidget(textFieldWidgetExtension));
        addChild(new ClickableWidget(textFieldWidgetExtension));

        widgetSet.setXYSize(0, 0, 120, 20);

        addChild(new ClickableWidget(widgetSet));
        ContentItem contentItem = new ContentItem(TextUtil.literal(key), new MappedWidget(widgetSet), 26);

        listViewWidget.add(contentItem);
    }

    @Override
    protected void onSave() {
        this.storedConfigEntries.clear();
        this.storedConfigEntries.putAll(this.storedConfigEntriesPending);

        Map<String, String> entriesToSave = new HashMap<>();
        if(customConfig == null) {
            entriesToSave.putAll(this.storedConfigEntriesPending);
        } else {
            for(EyecandyCustomConfig.Entry key : customConfig.entries()) {
                entriesToSave.put(key.id(), this.storedConfigEntriesPending.getOrDefault(key.id(), key.defaultValue()));
            }
        }

        sendCustomConfigUpdate(eyecandyId, blockPos, entriesToSave);
    }

    private static int parseInt(String str, int fallback) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static void sendCustomConfigUpdate(String modelId, BlockPos blockPos, Map<String, String> entries) {
        Networking.sendPacketToServer(new EyecandyCustomConfigUpdateC2SPacket(modelId, blockPos, entries));
    }
}
