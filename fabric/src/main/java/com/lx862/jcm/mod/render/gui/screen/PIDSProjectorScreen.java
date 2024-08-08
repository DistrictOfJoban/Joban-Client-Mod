package com.lx862.jcm.mod.render.gui.screen;

import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.data.pids.PIDSManager;
import com.lx862.jcm.mod.network.block.PIDSProjectorUpdatePacket;
import com.lx862.jcm.mod.network.block.PIDSUpdatePacket;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.jcm.mod.render.gui.screen.base.BlockConfigScreen;
import com.lx862.jcm.mod.render.gui.widget.ContentItem;
import com.lx862.jcm.mod.render.gui.widget.HorizontalWidgetSet;
import com.lx862.jcm.mod.render.gui.widget.MappedWidget;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.core.data.Platform;
import org.mtr.core.data.Station;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectList;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.CheckboxWidgetExtension;
import org.mtr.mapping.mapper.TextFieldWidgetExtension;
import org.mtr.mapping.tool.TextCase;
import org.mtr.mod.InitClient;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.screen.DashboardListItem;
import org.mtr.mod.screen.DashboardListSelectorScreen;

import static org.mtr.mod.screen.PIDSConfigScreen.getPlatformsForList;

public class PIDSProjectorScreen extends PIDSScreen {

    private final TextFieldWidgetExtension xField;
    private final TextFieldWidgetExtension yField;
    private final TextFieldWidgetExtension zField;
    private final TextFieldWidgetExtension scaleField;

    public PIDSProjectorScreen(BlockPos blockPos, String[] customMessages, boolean[] rowHidden, boolean hidePlatformNumber, String presetId, float x1, float y1, float z1, float scale) {
        super(blockPos, customMessages, rowHidden, hidePlatformNumber, presetId);

        this.xField = new TextFieldWidgetExtension(0, 0, 60, 20, "", 100, TextCase.DEFAULT, null, TextUtil.translatable(TextCategory.GUI, "pids.listview.widget.x").getString());
        this.yField = new TextFieldWidgetExtension(0, 0, 60, 20, "", 100, TextCase.DEFAULT, null, TextUtil.translatable(TextCategory.GUI, "pids.listview.widget.y").getString());
        this.zField = new TextFieldWidgetExtension(0, 0, 60, 20, "", 100, TextCase.DEFAULT, null, TextUtil.translatable(TextCategory.GUI, "pids.listview.widget.y").getString());
        this.scaleField = new TextFieldWidgetExtension(0, 0, 60, 20, "", 100, TextCase.DEFAULT, null, TextUtil.translatable(TextCategory.GUI, "pids.listview.widget.scale").getString());

        xField.setText2(String.valueOf(x1));
        yField.setText2(String.valueOf(y1));
        zField.setText2(String.valueOf(z1));
        scaleField.setText2(String.valueOf(scale));
    }

    @Override
    public MutableText getScreenTitle() {
        return TextUtil.translatable(TextCategory.BLOCK, "pids_projector");
    }

    @Override
    public void addConfigEntries() {
        super.addConfigEntries();

        addChild(new ClickableWidget(xField));
        addChild(new ClickableWidget(yField));
        addChild(new ClickableWidget(zField));
        addChild(new ClickableWidget(scaleField));

        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "pids.listview.widget.x"), new MappedWidget(xField));
        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "pids.listview.widget.y"), new MappedWidget(yField));
        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "pids.listview.widget.z"), new MappedWidget(zField));
        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "pids.listview.widget.scale"), new MappedWidget(scaleField));
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

        Networking.sendPacketToServer(new PIDSProjectorUpdatePacket(blockPos, filteredPlatforms, customMessages, rowHidden, hidePlatformNumber.isChecked2(), presetId, Float.parseFloat(xField.getText2()), Float.parseFloat(yField.getText2()), Float.parseFloat(zField.getText2()), Float.parseFloat(scaleField.getText2())));
    }
}
