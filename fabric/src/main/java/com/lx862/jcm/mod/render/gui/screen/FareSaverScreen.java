package com.lx862.jcm.mod.render.gui.screen;

import com.lx862.jcm.mod.network.block.FareSaverUpdatePacket;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.jcm.mod.render.gui.screen.base.BlockConfigScreen;
import com.lx862.jcm.mod.render.gui.widget.IntegerTextField;
import com.lx862.jcm.mod.render.gui.widget.MappedWidget;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.TextFieldWidgetExtension;
import org.mtr.mapping.tool.TextCase;

public class FareSaverScreen extends BlockConfigScreen {
    private final IntegerTextField discountTextField;
    private final TextFieldWidgetExtension prefixTextField;
    public FareSaverScreen(BlockPos blockPos, String prefix, int discount) {
        super(blockPos);
        this.prefixTextField = new TextFieldWidgetExtension(0, 0, 60, 20, 4, TextCase.DEFAULT, null, "$");
        this.prefixTextField.setText2(prefix);
        this.discountTextField = new IntegerTextField(0, 0, 60, 20, 0, 1000000, 2);
        this.discountTextField.setValue(discount);
    }

    @Override
    public MutableText getScreenTitle() {
        return TextUtil.translatable(TextCategory.BLOCK, "faresaver");
    }

    @Override
    public void addConfigEntries() {
        addChild(new ClickableWidget(prefixTextField));
        addChild(new ClickableWidget(discountTextField));
        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "faresaver.prefix"), new MappedWidget(prefixTextField));
        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "faresaver.discount"), new MappedWidget(discountTextField));
    }
    @Override
    public void onSave() {
        Networking.sendPacketToServer(new FareSaverUpdatePacket(blockPos, prefixTextField.getText2(), (int)discountTextField.getNumber()));
    }
}
