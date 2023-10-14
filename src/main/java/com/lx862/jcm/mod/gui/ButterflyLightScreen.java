package com.lx862.jcm.mod.gui;

import com.lx862.jcm.mod.gui.base.BlockConfigurationScreenBase;
import com.lx862.jcm.mod.gui.widget.NumericTextField;
import com.lx862.jcm.mod.network.block.ButterflyLightUpdatePacket;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.GraphicsHolder;

public class ButterflyLightScreen extends BlockConfigurationScreenBase {
    private final NumericTextField secondsToBlinkTextField;
    public ButterflyLightScreen(BlockPos blockPos, int discount) {
        super(blockPos);
        this.secondsToBlinkTextField = new NumericTextField(0, 0, 60, 20, 0, 100000, 10, TextUtil.translatable(TextUtil.TextCategory.GUI, "faresaver.currency"));
        this.secondsToBlinkTextField.setValue(discount);
    }

    @Override
    public MutableText getScreenTitle() {
        return TextUtil.translatable(TextUtil.TextCategory.BLOCK, "butterfly_light");
    }

    @Override
    public void addConfigEntries() {
        addDrawableChild2(secondsToBlinkTextField);
        listViewWidget.add(TextUtil.translatable(TextUtil.TextCategory.GUI, "butterfly_light.countdown"), secondsToBlinkTextField);
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta) {
        super.render(graphicsHolder, mouseX, mouseY, tickDelta);
    }

    @Override
    public void onSave() {
        Networking.sendPacketToServer(new ButterflyLightUpdatePacket(blockPos, secondsToBlinkTextField.getValue()));
    }
}
