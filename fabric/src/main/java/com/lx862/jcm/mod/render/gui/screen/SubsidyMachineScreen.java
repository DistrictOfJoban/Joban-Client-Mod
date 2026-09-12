package com.lx862.jcm.mod.render.gui.screen;

import com.lx862.jcm.mod.network.block.SubsidyMachineUpdatePacket;
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

public class SubsidyMachineScreen extends BlockConfigListScreen {
    private long price;
    private long cooldown;

    public SubsidyMachineScreen(BlockPos blockPos, int pricePerUse, int cooldown) {
        super(blockPos);
        this.price = pricePerUse;
        this.cooldown = cooldown;
    }

    @Override
    public MutableText getScreenTitle() {
        return Blocks.SUBSIDY_MACHINE.get().getName();
    }

    @Override
    public void addConfigEntries(ListViewWidget listViewWidget) {
        IntegerTextField priceTextField = new IntegerTextField(0, 0, 60, 20, 0, 50000, 10, TextUtil.translatable(TextCategory.GUI, "subsidy_machine.currency"));
        priceTextField.onChange(newValue -> this.price = newValue);
        priceTextField.setValue(this.price);

        IntegerTextField cooldownTextField = new IntegerTextField(0, 0, 60, 20, 0, 1200, 0);
        cooldownTextField.onChange(newValue -> this.cooldown = newValue);
        cooldownTextField.setValue(this.cooldown);

        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "subsidy_machine.price"), new MappedWidget(priceTextField));
        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "subsidy_machine.cooldown"), new MappedWidget(cooldownTextField));

        addChild(new ClickableWidget(priceTextField));
        addChild(new ClickableWidget(cooldownTextField));
    }

    @Override
    public void onSave() {
        Networking.sendPacketToServer(new SubsidyMachineUpdatePacket(blockPos, (int)price, (int)cooldown));
    }
}
