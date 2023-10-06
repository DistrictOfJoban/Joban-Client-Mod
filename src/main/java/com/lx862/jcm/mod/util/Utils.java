package com.lx862.jcm.mod.util;

import org.mtr.mapping.holder.Hand;
import org.mtr.mapping.holder.ItemStack;
import org.mtr.mapping.holder.Items;
import org.mtr.mapping.holder.PlayerEntity;

public class Utils {
    public static void decrementItemFromPlayerHand(PlayerEntity player, Hand hand, int count) {
        ItemStack holding = player.getStackInHand(hand);
        holding.decrement(count);
    }

    public static boolean playerHoldingBrush(PlayerEntity player) {
        // TODO: Temporary, will replace with Brush after adding MTR as dependencies
        return player.isHolding(Items.getGrassBlockMapped());
    }
}
