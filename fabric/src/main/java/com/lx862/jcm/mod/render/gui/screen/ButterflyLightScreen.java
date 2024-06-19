package com.lx862.jcm.mod.render.gui.screen;

import com.lx862.jcm.mod.render.gui.screen.base.BlockConfigScreen;
import com.lx862.jcm.mod.render.gui.widget.MappedWidget;
import com.lx862.jcm.mod.render.gui.widget.NumericTextField;
import com.lx862.jcm.mod.network.block.ButterflyLightUpdatePacket;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.holder.MutableText;

public class ButterflyLightScreen extends BlockConfigScreen {
    private final NumericTextField startBlinkingSecondsTextField;
    public ButterflyLightScreen(BlockPos blockPos, int startBlinkingSeconds) {
        super(blockPos);
        this.startBlinkingSecondsTextField = new NumericTextField(0, 0, 60, 20, 0, 100000, 10);
        this.startBlinkingSecondsTextField.setValue(startBlinkingSeconds);
    }

    @Override
    public MutableText getScreenTitle() {
        return TextUtil.translatable(TextCategory.BLOCK, "butterfly_light");
    }

    @Override
    public void addConfigEntries() {
        addChild(new ClickableWidget(startBlinkingSecondsTextField));
        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "butterfly_light.countdown"), new MappedWidget(startBlinkingSecondsTextField));
    }

    @Override
    public void onSave() {
        Networking.sendPacketToServer(new ButterflyLightUpdatePacket(blockPos, (int) startBlinkingSecondsTextField.getNumber()));
    }
}
