package com.lx862.jcm.mod.util;

import org.mtr.mapping.holder.Items;
import org.mtr.mapping.holder.PlayerEntity;

public class Utils {
    public static boolean playerHoldingBrush(PlayerEntity player) {
        // TODO: Temporary, will replace with Brush after adding MTR as dependencies
        return player.isHolding(Items.getGrassBlockMapped());
    }
}
