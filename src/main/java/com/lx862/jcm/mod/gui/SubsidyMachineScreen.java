package com.lx862.jcm.mod.gui;

import com.lx862.jcm.mod.gui.widget.NumericTextField;
import com.lx862.jcm.mod.network.block.SubsidyMachineUpdatePacket;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.registry.RegistryClient;

// TODO: Finish this thing
public class SubsidyMachineScreen extends BlockConfigurationScreenBase {
    private final NumericTextField priceTextField;
    private final NumericTextField cooldownTextField;
    public SubsidyMachineScreen(BlockPos blockPos, int pricePerUse, int cooldown) {
        super(blockPos);
        this.priceTextField = new NumericTextField(0, 0, 60, 20, 0, 50000, 10, "$");
        this.cooldownTextField = new NumericTextField(0, 0, 60, 20, 0, 1200, 0, null);

        this.priceTextField.setValue(pricePerUse);
        this.cooldownTextField.setValue(cooldown);
    }

    @Override
    public MutableText getScreenTitle() {
        return TextUtil.translatable(TextUtil.TextCategory.BLOCK, "subsidy_machine");
    }

    @Override
    public void addConfigEntries() {
        addDrawableChild2(priceTextField);
        addDrawableChild2(cooldownTextField);

        listViewWidget.add(TextUtil.translatable(TextUtil.TextCategory.GUI, "subsidy_machine.price"), priceTextField);
        listViewWidget.add(TextUtil.translatable(TextUtil.TextCategory.GUI, "subsidy_machine.cooldown"), cooldownTextField);
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
        super.render(graphicsHolder, mouseX, mouseY, delta);
    }

    @Override
    public void onSave() {
        RegistryClient.sendPacketToServer(new SubsidyMachineUpdatePacket(blockPos, priceTextField.getValue(), cooldownTextField.getValue()));
    }
}
