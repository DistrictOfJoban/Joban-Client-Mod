package com.lx862.jcm.mod.render.gui.screen.base;

import com.lx862.jcm.mod.render.GuiHelper;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.core.data.Station;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mod.InitClient;
import org.mtr.mod.data.IGui;

/**
 * GUI Screen for configuring block settings, you should extend this class for your own block config screen
 */
public abstract class BlockConfigScreen extends SavableScreen implements GuiHelper {
    protected final BlockPos blockPos;

    public BlockConfigScreen(BlockPos blockPos) {
        super(false);
        this.blockPos = blockPos;
    }

    protected int getContentWidth() {
        return (int)Math.min((width * 0.75), MAX_CONTENT_WIDTH);
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
}
