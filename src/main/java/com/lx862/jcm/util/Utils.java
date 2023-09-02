package com.lx862.jcm.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class Utils {
    public static void decrementItemFromPlayerHand(PlayerEntity player, Hand hand, int count) {
        ItemStack holding = player.getStackInHand(hand);
        holding.decrement(count);
    }
}
