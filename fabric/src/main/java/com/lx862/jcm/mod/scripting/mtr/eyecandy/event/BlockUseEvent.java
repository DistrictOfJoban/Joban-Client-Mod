package com.lx862.jcm.mod.scripting.mtr.eyecandy.event;

import com.lx862.mtrscripting.wrapper.EntityWrapper;
import com.lx862.mtrscripting.wrapper.ItemStackWrapper;
import com.lx862.mtrscripting.wrapper.PlayerEntityWrapper;
import org.mtr.mapping.holder.Entity;
import org.mtr.mapping.holder.ItemStack;
import org.mtr.mapping.holder.PlayerEntity;

public class BlockUseEvent {
    private final PlayerEntityWrapper entity;
    private final ItemStackWrapper itemUsed;

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
