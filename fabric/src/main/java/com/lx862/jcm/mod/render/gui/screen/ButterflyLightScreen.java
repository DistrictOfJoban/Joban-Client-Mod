package com.lx862.jcm.mod.render.gui.screen;

import com.lx862.jcm.mod.network.block.ButterflyLightUpdatePacket;
import com.lx862.jcm.mod.registry.Blocks;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.jcm.mod.render.gui.screen.base.BlockConfigListScreen;
import com.lx862.jcm.mod.render.gui.widget.IntegerTextField;
import com.lx862.jcm.mod.render.gui.widget.ListViewWidget;
import com.lx862.jcm.mod.render.gui.widget.MappedWidget;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.holder.MutableText;

public class ButterflyLightScreen extends BlockConfigListScreen {
    private long value;

    public ButterflyLightScreen(BlockPos blockPos, int startBlinkingSeconds) {
        super(blockPos);
        this.value = startBlinkingSeconds;
    }

    @Override
    public MutableText getScreenTitle() {
        return Blocks.BUTTERFLY_LIGHT.get().getName();
    }

    @Override
    public void addConfigEntries(ListViewWidget listViewWidget) {
        IntegerTextField startBlinkingSecTextField = new IntegerTextField(0, 0, 60, 20, 0, 100000, 10);
        startBlinkingSecTextField.setValue(value);
        startBlinkingSecTextField.onChange(newValue -> {
            this.value = newValue;
        });

        addChild(new ClickableWidget(startBlinkingSecTextField));
        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "butterfly_light.countdown"), new MappedWidget(startBlinkingSecTextField));
    }

    @Override
    public void onSave() {
        Networking.sendPacketToServer(new ButterflyLightUpdatePacket(blockPos, (int) this.value));
    }
}
