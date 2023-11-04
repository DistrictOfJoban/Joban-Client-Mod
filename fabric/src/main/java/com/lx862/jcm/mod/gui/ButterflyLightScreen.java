package com.lx862.jcm.mod.gui;

import com.lx862.jcm.mod.gui.base.BlockConfigScreenBase;
import com.lx862.jcm.mod.gui.widget.MappedWidget;
import com.lx862.jcm.mod.gui.widget.NumericTextField;
import com.lx862.jcm.mod.network.block.ButterflyLightUpdatePacket;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.holder.MutableText;

public class ButterflyLightScreen extends BlockConfigScreenBase {
    private final NumericTextField secondsToBlinkTextField;
    public ButterflyLightScreen(BlockPos blockPos, int discount) {
        super(blockPos);
        this.secondsToBlinkTextField = new NumericTextField(0, 0, 60, 20, 0, 100000, 10, TextUtil.translatable(TextCategory.GUI, "faresaver.currency"));
        this.secondsToBlinkTextField.setValue(discount);
    }

    @Override
    public MutableText getScreenTitle() {
        return TextUtil.translatable(TextCategory.BLOCK, "butterfly_light");
    }

    @Override
    public void addConfigEntries() {
        addChild(new ClickableWidget(secondsToBlinkTextField));
        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "butterfly_light.countdown"), new MappedWidget(secondsToBlinkTextField));
    }

    @Override
    public void onSave() {
        Networking.sendPacketToServer(new ButterflyLightUpdatePacket(blockPos, (int)secondsToBlinkTextField.getValue()));
    }
}
