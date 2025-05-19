package com.lx862.jcm.mod.render.gui.screen.base;

import com.lx862.jcm.mod.render.GuiHelper;
import com.lx862.jcm.mod.render.gui.widget.WidgetSet;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.core.data.Station;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mod.InitClient;
import org.mtr.mod.data.IGui;

/**
 * GUI Screen for configuring block settings, you should extend this class for your own block config screen
 */
public abstract class BlockConfigScreen extends TitledScreen implements GuiHelper {
    protected final BlockPos blockPos;
    protected final WidgetSet bottomEntryWidget;
    private final ButtonWidgetExtension saveButton;
    private final ButtonWidgetExtension discardButton;
    private boolean discardConfig = false;

    public BlockConfigScreen(BlockPos blockPos) {
        super(false);
        this.blockPos = blockPos;

        this.saveButton = new ButtonWidgetExtension(0, 0, 0, 20, TextUtil.translatable(TextCategory.GUI, "block_config.save"), (btn) -> {
            onClose2();
        });

        this.discardButton = new ButtonWidgetExtension(0, 0, 0, 20, TextUtil.translatable(TextCategory.GUI, "block_config.discard"), (btn) -> {
            discardConfig = true;
            onClose2();
        });

        this.bottomEntryWidget = new WidgetSet(20);
    }

    protected int getContentWidth() {
        return (int)Math.min((width * 0.75), MAX_CONTENT_WIDTH);
    }

    protected void addBottomRowEntry(int x, int y, int width, int height) {
        bottomEntryWidget.reset();
        addChild(new ClickableWidget(saveButton));
        addChild(new ClickableWidget(discardButton));

        bottomEntryWidget.addWidget(saveButton);
        bottomEntryWidget.addWidget(discardButton);
        bottomEntryWidget.setXYSize(x, y, width, height);
        addChild(new ClickableWidget(bottomEntryWidget));
    }

    @Override
    public MutableText getScreenSubtitle() {
        Station atStation = InitClient.findStation(blockPos);

        if(atStation != null) {
            return TextUtil.translatable(TextCategory.GUI,
                    "block_config.subtitle_with_station",
                    blockPos.getX(), blockPos.getY(), blockPos.getZ(),
                    IGui.formatStationName(atStation.getName())
            );
        } else {
            return TextUtil.translatable(TextCategory.GUI,
                    "block_config.subtitle",
                    blockPos.getX(), blockPos.getY(), blockPos.getZ()
            );
        }
    }

    @Override
    public void onClose2() {
        // Save config by default, unless explicitly requested not to
        if(!discardConfig) {
            onSave();
        }
        super.onClose2();
    }

    protected abstract void onSave();

    @Override
    public boolean isPauseScreen2() {
        return false;
    }
}
