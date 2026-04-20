package com.lx862.mtrscripting.mod.impl.mtr.eyecandy.event;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.integration.ItemStackWrapper;
import com.lx862.mtrscripting.core.integration.PlayerEntityWrapper;
import org.mtr.mapping.holder.ItemStack;
import org.mtr.mapping.holder.PlayerEntity;

public class BlockUseEvent {
    private final PlayerEntityWrapper entity;
    private final ItemStackWrapper itemUsed;

    @ApiInternal
    public BlockUseEvent(PlayerEntity playerEntity, ItemStack itemUsed) {
        this.entity = new PlayerEntityWrapper(playerEntity);
        this.itemUsed = new ItemStackWrapper(itemUsed);
    }

    public PlayerEntityWrapper player() {
        return entity;
    }

    public ItemStackWrapper item() {
        return itemUsed;
    }
}
