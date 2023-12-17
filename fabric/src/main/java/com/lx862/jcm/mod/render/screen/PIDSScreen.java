package com.lx862.jcm.mod.render.screen;

import com.lx862.jcm.mod.data.pids.PIDSManager;
import com.lx862.jcm.mod.network.block.PIDSUpdatePacket;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.jcm.mod.render.screen.base.BlockConfigScreenBase;
import com.lx862.jcm.mod.render.screen.widget.HorizontalWidgetSet;
import com.lx862.jcm.mod.render.screen.widget.ListEntry;
import com.lx862.jcm.mod.render.screen.widget.MappedWidget;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.CheckboxWidgetExtension;
import org.mtr.mapping.mapper.TextFieldWidgetExtension;
import org.mtr.mapping.tool.TextCase;

public class PIDSScreen extends BlockConfigScreenBase {
    private final TextFieldWidgetExtension[] customMessagesWidgets;
    private final CheckboxWidgetExtension[] rowHiddenWidgets;
    private final CheckboxWidgetExtension hidePlatformNumber;
    private final ButtonWidgetExtension choosePresetButton;
    private String presetId;
    public PIDSScreen(BlockPos blockPos, String[] customMessages, boolean[] rowHidden, boolean hidePlatformNumber, String presetId) {
        super(blockPos);
        this.customMessagesWidgets = new TextFieldWidgetExtension[customMessages.length];
        this.rowHiddenWidgets = new CheckboxWidgetExtension[rowHidden.length];

        for(int i = 0; i < customMessages.length; i++) {
            String customMessage = customMessages[i];
            this.customMessagesWidgets[i] = new TextFieldWidgetExtension(0, 0, 120, 20, "", 100, TextCase.DEFAULT, null, TextUtil.translatable(TextCategory.GUI, "pids.listview.widget.custom_message").getString());
            this.customMessagesWidgets[i].setText2(customMessage);
        }

        for(int i = 0; i < rowHidden.length; i++) {
            boolean rowIsHidden = rowHidden[i];
            this.rowHiddenWidgets[i] = new CheckboxWidgetExtension(0, 0, 120, 20, true, (cb) -> {});
            this.rowHiddenWidgets[i].setChecked(rowIsHidden);
            this.rowHiddenWidgets[i].setMessage2(Text.cast(TextUtil.translatable(TextCategory.GUI, "pids.listview.widget.row_hidden")));
        }

        this.hidePlatformNumber = new CheckboxWidgetExtension(0, 0, 20, 20, true, (cb) -> {});
        this.hidePlatformNumber.setChecked(hidePlatformNumber);

        this.choosePresetButton = new ButtonWidgetExtension(0, 0, 60, 20, TextUtil.translatable(TextCategory.GUI, "pids.listview.widget.choose_preset"), (btn) -> {
            MinecraftClient.getInstance().openScreen(
                new Screen(new PIDSPresetScreen(this.presetId, (str) -> {
                    this.presetId = str;
                }).withPreviousScreen(new Screen(this)))
            );
        });

        this.presetId = presetId;
    }

    @Override
    public MutableText getScreenTitle() {
        return TextUtil.translatable(TextCategory.BLOCK, "pids_rv");
    }

    @Override
    public void addConfigEntries() {
        for(int i = 0; i < this.customMessagesWidgets.length; i++) {
            addChild(new ClickableWidget(this.customMessagesWidgets[i]));
            addChild(new ClickableWidget(this.rowHiddenWidgets[i]));

            int w = listViewWidget.getWidth2();
            this.rowHiddenWidgets[i].setWidth2(95);
            this.customMessagesWidgets[i].setWidth2(w - this.rowHiddenWidgets[i].getWidth2() - 10);

            HorizontalWidgetSet widgetSet = new HorizontalWidgetSet();
            widgetSet.addWidget(new MappedWidget(this.customMessagesWidgets[i]));
            widgetSet.addWidget(new MappedWidget(this.rowHiddenWidgets[i]));
            widgetSet.setXYSize(0, 0, 0, 20);

            listViewWidget.add(null, new MappedWidget(widgetSet));
        }

        addChild(new ClickableWidget(hidePlatformNumber));
        addChild(new ClickableWidget(choosePresetButton));

        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "pids.listview.title.hide_platform_number"), new MappedWidget(hidePlatformNumber));

        ListEntry presetEntry = new ListEntry(TextUtil.translatable(TextCategory.GUI, "pids.listview.title.pids_preset"), new MappedWidget(choosePresetButton), false, 26);
        if(PIDSManager.getPreset(presetId) != null) {
            presetEntry.setIconCallback((guiDrawing, startX, startY, width, height) -> {
                PIDSPresetScreen.drawPIDSPreview(PIDSManager.getPreset(presetId), guiDrawing, startX, startY, width, height, true);
            });
        }
        listViewWidget.add(presetEntry);
    }

    @Override
    public void onSave() {
        String[] customMessages = new String[customMessagesWidgets.length];
        boolean[] rowHidden = new boolean[rowHiddenWidgets.length];

        for(int i = 0; i < customMessagesWidgets.length; i++) {
            customMessages[i] = this.customMessagesWidgets[i].getText2();
        }

        for(int i = 0; i < rowHiddenWidgets.length; i++) {
            rowHidden[i] = this.rowHiddenWidgets[i].isChecked2();
        }

        Networking.sendPacketToServer(new PIDSUpdatePacket(blockPos, customMessages, rowHidden, hidePlatformNumber.isChecked2(), presetId));
    }
}
