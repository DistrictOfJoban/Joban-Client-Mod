package com.lx862.jcm.mod.render.screen;

import com.lx862.jcm.mod.network.block.RVPIDSUpdatePacket;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.jcm.mod.render.screen.base.BlockConfigScreenBase;
import com.lx862.jcm.mod.render.screen.widget.HorizontalWidgetSet;
import com.lx862.jcm.mod.render.screen.widget.MappedWidget;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.CheckboxWidgetExtension;
import org.mtr.mapping.mapper.TextFieldWidgetExtension;
import org.mtr.mapping.tool.TextCase;

public class RVPIDSScreen extends BlockConfigScreenBase {
    private final TextFieldWidgetExtension[] customMessagesWidgets;
    private final CheckboxWidgetExtension[] rowHiddenWidgets;
    private final CheckboxWidgetExtension hidePlatformNumber;
    private final ButtonWidgetExtension choosePresetButton;
    private String presetId;
    public RVPIDSScreen(BlockPos blockPos, String[] customMessages, boolean[] rowHidden, boolean hidePlatformNumber, String presetId) {
        super(blockPos);
        this.customMessagesWidgets = new TextFieldWidgetExtension[customMessages.length];
        this.rowHiddenWidgets = new CheckboxWidgetExtension[rowHidden.length];

        for(int i = 0; i < customMessages.length; i++) {
            String customMessage = customMessages[i];
            this.customMessagesWidgets[i] = new TextFieldWidgetExtension(0, 0, 120, 20, "", 100, TextCase.DEFAULT, null, "Custom messages...");
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
            this.customMessagesWidgets[i].setWidth2(w - 100);
            this.rowHiddenWidgets[i].setWidth2(95);

            HorizontalWidgetSet widgetSet = new HorizontalWidgetSet();
            widgetSet.addWidget(new MappedWidget(this.customMessagesWidgets[i]));
            widgetSet.addWidget(new MappedWidget(this.rowHiddenWidgets[i]));
            widgetSet.setXYSize(0, 0, 0, 20);

            listViewWidget.add(null, new MappedWidget(widgetSet));
        }

        addChild(new ClickableWidget(hidePlatformNumber));
        addChild(new ClickableWidget(choosePresetButton));

        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "pids.listview.title.hide_platform_number"), new MappedWidget(hidePlatformNumber));
        listViewWidget.add(TextUtil.literal("PIDS Preset (" + presetId + ")"), new MappedWidget(choosePresetButton));
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

        Networking.sendPacketToServer(new RVPIDSUpdatePacket(blockPos, customMessages, rowHidden, hidePlatformNumber.isChecked2(), presetId));
    }
}
