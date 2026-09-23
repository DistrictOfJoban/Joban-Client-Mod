package com.lx862.jcm.mod.gui.screen;

import com.lx862.amberui.gui.widget.*;
import com.lx862.jcm.mod.block.entity.PIDSProjectorBlockEntity;
import com.lx862.jcm.mod.network.block.PIDSProjectorUpdatePacket;
import com.lx862.jcm.mod.registry.Blocks;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.*;

public class PIDSProjectorScreen extends PIDSScreen {

    private final DoubleTextField xField;
    private final DoubleTextField yField;
    private final DoubleTextField zField;
    private final DoubleTextField scaleField;
    private final DoubleTextField rotateXField;
    private final DoubleTextField rotateYField;
    private final DoubleTextField rotateZField;

    private final double originalX, originalY, originalZ, originalScale;
    private final double originalRotX, originalRotY, originalRotZ;

    public PIDSProjectorScreen(BlockPos blockPos, String[] customMessages, boolean[] rowHidden, boolean hidePlatformNumber, String presetId, double x, double y, double z, double rotateX, double rotateY, double rotateZ, double scale) {
        super(blockPos, customMessages, rowHidden, hidePlatformNumber, presetId);

        this.xField = new DoubleTextField(0, 0, 40, 20, -1000, 1000, 0);
        this.yField = new DoubleTextField(0, 0, 40, 20, -1000, 1000, 0);
        this.zField = new DoubleTextField(0, 0, 40, 20, -1000, 1000, 0);
        this.rotateXField = new DoubleTextField(0, 0, 40, 20, -359, 360, 0);
        this.rotateYField = new DoubleTextField(0, 0, 40, 20, -359, 360, 0);
        this.rotateZField = new DoubleTextField(0, 0, 40, 20, -359, 360, 0);
        this.scaleField = new DoubleTextField(0, 0, 40, 20, 0, 30, 0);

        this.xField.setChangedListener2(newVal -> sendClientPreview(false));
        this.yField.setChangedListener2(newVal -> sendClientPreview(false));
        this.zField.setChangedListener2(newVal -> sendClientPreview(false));
        this.rotateXField.setChangedListener2(newVal -> sendClientPreview(false));
        this.rotateYField.setChangedListener2(newVal -> sendClientPreview(false));
        this.rotateZField.setChangedListener2(newVal -> sendClientPreview(false));
        this.scaleField.setChangedListener2(newVal -> sendClientPreview(false));

        xField.setText2(String.valueOf(x));
        yField.setText2(String.valueOf(y));
        zField.setText2(String.valueOf(z));
        rotateXField.setText2(String.valueOf(rotateX));
        rotateYField.setText2(String.valueOf(rotateY));
        rotateZField.setText2(String.valueOf(rotateZ));
        scaleField.setText2(String.valueOf(scale));

        this.originalX = x;
        this.originalY = y;
        this.originalZ = z;

        this.originalRotX = rotateX;
        this.originalRotY = rotateY;
        this.originalRotZ = rotateZ;
        this.originalScale = scale;
    }

    @Override
    public MutableText getScreenTitle() {
        return Blocks.PIDS_PROJECTOR.get().getName();
    }

    @Override
    public void addConfigEntries(ListViewWidget listViewWidget) {
        WidgetSet positionFields = new WidgetSet(20, 0);
        WidgetSet rotationFields = new WidgetSet(20, 0);

        addChild(new ClickableWidget(xField));
        addChild(new ClickableWidget(yField));
        addChild(new ClickableWidget(zField));
        addChild(new ClickableWidget(rotateXField));
        addChild(new ClickableWidget(rotateYField));
        addChild(new ClickableWidget(rotateZField));
        addChild(new ClickableWidget(scaleField));
        addChild(new ClickableWidget(positionFields));
        addChild(new ClickableWidget(rotationFields));

        positionFields.addWidget(new MappedWidget(xField));
        positionFields.addWidget(new MappedWidget(yField));
        positionFields.addWidget(new MappedWidget(zField));

        rotationFields.addWidget(new MappedWidget(rotateXField));
        rotationFields.addWidget(new MappedWidget(rotateYField));
        rotationFields.addWidget(new MappedWidget(rotateZField));

        rotationFields.setXYSize(0, 0, 140, 20);
        positionFields.setXYSize(0, 0, 140, 20);
        listViewWidget.add(new CategoryItem(TextUtil.translatable(TextCategory.GUI, "pids.listview.category.projection")));
        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "pids.listview.widget.position"), new MappedWidget(positionFields));
        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "pids.listview.widget.rotate"), new MappedWidget(rotationFields));
        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "pids.listview.widget.scale"), new MappedWidget(scaleField));
        listViewWidget.add(new CategoryItem(TextUtil.translatable(TextCategory.GUI, "pids.listview.category.pids")));
        super.addConfigEntries(listViewWidget);
    }

    private void sendClientPreview(boolean discard) {
        ClientWorld clientWorld = MinecraftClient.getInstance().getWorldMapped();
        if(clientWorld != null) {
            BlockEntity clientBE = clientWorld.getBlockEntity(blockPos);
            if(clientBE != null && clientBE.data instanceof PIDSProjectorBlockEntity) {
                PIDSProjectorBlockEntity blockEntity = (PIDSProjectorBlockEntity)clientBE.data;
                double x = discard ? originalX : xField.getNumber();
                double y = discard ? originalY : yField.getNumber();
                double z = discard ? originalZ : zField.getNumber();
                double rotX = discard ? originalRotX : rotateXField.getNumber();
                double rotY = discard ? originalRotY : rotateYField.getNumber();
                double rotZ = discard ? originalRotZ : rotateZField.getNumber();
                double scale = discard ? originalScale : scaleField.getNumber();
                blockEntity.setData(blockEntity.getCustomMessages(), blockEntity.getPlatformIds(), blockEntity.getRowHidden(), blockEntity.platformNumberHidden(), blockEntity.getPresetId(), x, y, z, rotX, rotY, rotZ, scale);
            }
        }
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

        Networking.sendPacketToServer(new PIDSProjectorUpdatePacket(blockPos, filteredPlatforms, customMessages, rowHidden, hidePlatformNumber.isChecked2(), presetId, xField.getNumber(), yField.getNumber(), zField.getNumber(), rotateXField.getNumber(), rotateYField.getNumber(), rotateZField.getNumber(), scaleField.getNumber()));
    }

    @Override
    public void onDiscard() {
        sendClientPreview(true);
    }
}
