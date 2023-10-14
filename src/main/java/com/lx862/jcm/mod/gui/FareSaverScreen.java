package com.lx862.jcm.mod.gui;

import com.lx862.jcm.mod.gui.base.BlockConfigurationScreenBase;
import com.lx862.jcm.mod.gui.widget.NumericTextField;
import com.lx862.jcm.mod.network.block.FareSaverUpdatePacket;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.GraphicsHolder;

public class FareSaverScreen extends BlockConfigurationScreenBase {
    private final NumericTextField discountTextField;
    public FareSaverScreen(BlockPos blockPos, int discount) {
        super(blockPos);
        this.discountTextField = new NumericTextField(0, 0, 60, 20, 0, 100000, 2, TextUtil.translatable(TextUtil.TextCategory.GUI, "faresaver.currency"));
        this.discountTextField.setValue(discount);
    }

    @Override
    public MutableText getScreenTitle() {
        return TextUtil.translatable(TextUtil.TextCategory.BLOCK, "faresaver");
    }

    @Override
    public void addConfigEntries() {
        addDrawableChild2(discountTextField);
        listViewWidget.add(TextUtil.translatable(TextUtil.TextCategory.GUI, "faresaver.discount"), discountTextField);
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta) {
        super.render(graphicsHolder, mouseX, mouseY, tickDelta);
    }

    @Override
    public void onSave() {
        Networking.sendPacketToServer(new FareSaverUpdatePacket(blockPos, discountTextField.getValue()));
    }
}
