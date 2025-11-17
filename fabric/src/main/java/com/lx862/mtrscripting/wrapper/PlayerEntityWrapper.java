package com.lx862.mtrscripting.wrapper;

import com.lx862.jcm.mapping.LoaderImpl;
import org.mtr.mapping.holder.*;

public class PlayerEntityWrapper extends EntityWrapper {
    private final PlayerEntity playerEntity;

    public PlayerEntityWrapper(PlayerEntity player) {
        super(Entity.cast(player));
        this.playerEntity = player;
    }

    public String playerName() {
        return playerEntity.getGameProfile().getName();
    }

    public boolean isHoldingItem(Identifier id) {
        Item itm = LoaderImpl.getItemFromId(id);
        if(itm == null) return false;
        return playerEntity.isHolding(itm);
    }

    public ItemStackWrapper mainHandItem() {
        return new ItemStackWrapper(playerEntity.getMainHandStack());
    }

    public ItemStackWrapper offHandItem() {
        return new ItemStackWrapper(playerEntity.getOffHandStack());
    }

    public ItemStackWrapper activeItem() {
        ItemStackWrapper mainHandItem = mainHandItem();
        if(!mainHandItem.empty()) return mainHandItem;

        return offHandItem();
    }
}
