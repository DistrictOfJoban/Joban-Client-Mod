package com.lx862.jcm.mod.network;

import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mod.Items;

public class PacketValidator {
    /**
     * Whether a player should be able to write new settings to a block.
     * Disallows survival/adventure players without holding a brush.
     * @param serverPlayerEntity The player to check against.
     * @return true if the player can configure the block, false otherwise.
     */
    public static boolean canConfigureBlock(ServerPlayerEntity serverPlayerEntity) {
        if(serverPlayerEntity.isCreative() || serverPlayerEntity.isSpectator()) return true;
        // We ought to search through the entire inventory, but this does not seem to be exposed by MC-Mappings.
        // And the odds of you being in survival/adventure, having a brush, and managed to tug it away from your hand before the GUI opens... is a bit low
        return serverPlayerEntity.isHolding(Items.BRUSH.get());
    }
}
