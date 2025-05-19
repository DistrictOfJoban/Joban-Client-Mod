package com.lx862.jcm.mod.render.gui.screen;

import com.lx862.jcm.mod.network.block.SubsidyMachineUpdatePacket;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.jcm.mod.render.gui.screen.base.BlockConfigListScreen;
import com.lx862.jcm.mod.render.gui.widget.IntegerTextField;
import com.lx862.jcm.mod.render.gui.widget.MappedWidget;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.holder.MutableText;

public class SubsidyMachineScreen extends BlockConfigListScreen {
    private final IntegerTextField priceTextField;
    private final IntegerTextField cooldownTextField;
    public SubsidyMachineScreen(BlockPos blockPos, int pricePerUse, int cooldown) {
        super(blockPos);
        this.priceTextField = new IntegerTextField(0, 0, 60, 20, 0, 50000, 10, TextUtil.translatable(TextCategory.GUI, "subsidy_machine.currency"));
        this.cooldownTextField = new IntegerTextField(0, 0, 60, 20, 0, 1200, 0);

        this.priceTextField.setValue(pricePerUse);
        this.cooldownTextField.setValue(cooldown);
    }

    @Override
    public MutableText getScreenTitle() {
        return TextUtil.translatable(TextCategory.BLOCK, "subsidy_machine");
    }

    @Override
    public void addConfigEntries() {
        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "subsidy_machine.price"), new MappedWidget(priceTextField));
        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "subsidy_machine.cooldown"), new MappedWidget(cooldownTextField));

        addChild(new ClickableWidget(priceTextField));
        addChild(new ClickableWidget(cooldownTextField));
    }

    @Override
    public void onSave() {
        Networking.sendPacketToServer(new SubsidyMachineUpdatePacket(blockPos, (int)priceTextField.getNumber(), (int)cooldownTextField.getNumber()));
    }
}
