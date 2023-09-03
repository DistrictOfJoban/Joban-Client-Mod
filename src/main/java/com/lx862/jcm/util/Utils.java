package com.lx862.jcm.util;

import org.mtr.mapping.holder.*;

public class Utils {
    public static void decrementItemFromPlayerHand(PlayerEntity player, Hand hand, int count) {
        ItemStack holding = player.getStackInHand(hand);
        holding.decrement(count);
    }
}
