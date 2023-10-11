package com.lx862.jcm.mod.gui;

import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.Text;
import org.mtr.mapping.mapper.GraphicsHolder;

// TODO: Finish this thing
public class SubsidyMachineScreen extends BasicScreenBase {
    private final BlockPos blockPos;
    public SubsidyMachineScreen(BlockPos blockPos) {
        super(false);
        this.blockPos = blockPos;
    }

    @Override
    public MutableText getScreenTitle() {
        return TextUtil.getTranslatable(TextUtil.TextCategory.BLOCK, "subsidy_machine");
    }

    @Override
    public MutableText getScreenSubtitle() {
        String posText = String.format("%d %d %d", blockPos.getX(), blockPos.getY(), blockPos.getZ());
        String stationText = "Near 中環 Central";
        return Text.literal("At: " + posText + " | " + stationText);
    }

    @Override
    protected void init2() {
        super.init2();
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta) {
        super.render(graphicsHolder, mouseX, mouseY, delta);
    }
}
