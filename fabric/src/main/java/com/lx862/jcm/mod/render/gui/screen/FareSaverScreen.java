package com.lx862.jcm.mod.render.gui.screen;

import com.lx862.jcm.mod.network.block.FareSaverUpdatePacket;
import com.lx862.jcm.mod.registry.Blocks;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.jcm.mod.render.gui.screen.base.BlockConfigListScreen;
import com.lx862.jcm.mod.render.gui.widget.AlwaysRenderedTextField;
import com.lx862.jcm.mod.render.gui.widget.IntegerTextField;
import com.lx862.jcm.mod.render.gui.widget.ListViewWidget;
import com.lx862.jcm.mod.render.gui.widget.MappedWidget;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.tool.TextCase;

public class FareSaverScreen extends BlockConfigListScreen {
    private long discount;
    private String prefix;

    public FareSaverScreen(BlockPos blockPos, String prefix, int discount) {
        super(blockPos);
        this.discount = discount;
        this.prefix = prefix;
    }

    @Override
    public MutableText getScreenTitle() {
        return Blocks.FARE_SAVER.get().getName();
    }

    @Override
    public void addConfigEntries(ListViewWidget listViewWidget) {
        AlwaysRenderedTextField prefixTextField = new AlwaysRenderedTextField(0, 0, 60, 20, 4, TextCase.DEFAULT, null, "$");
        prefixTextField.setChangedListener2(newValue -> this.prefix = newValue);
        prefixTextField.setText2(this.prefix);

        IntegerTextField discountTextField = new IntegerTextField(0, 0, 60, 20, 0, 1000000, 2);
        discountTextField.onChange(newValue -> this.discount = newValue);
        discountTextField.setValue(this.discount);

        addChild(new ClickableWidget(prefixTextField));
        addChild(new ClickableWidget(discountTextField));
        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "faresaver.prefix"), new MappedWidget(prefixTextField));
        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "faresaver.discount"), new MappedWidget(discountTextField));
    }

    @Override
    public void onSave() {
        Networking.sendPacketToServer(new FareSaverUpdatePacket(blockPos, this.prefix, (int)this.discount));
    }
}
