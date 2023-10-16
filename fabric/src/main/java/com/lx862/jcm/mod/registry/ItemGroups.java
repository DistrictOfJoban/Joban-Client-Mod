package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.Constants;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.ItemConvertible;
import org.mtr.mapping.holder.ItemStack;
import org.mtr.mapping.registry.CreativeModeTabHolder;
import org.mtr.mapping.registry.Registry;

public class ItemGroups {
    public static final CreativeModeTabHolder JCM_MAIN = Registry.createCreativeModeTabHolder(
            new Identifier(Constants.MOD_ID, "main"),
            () -> new ItemStack(new ItemConvertible(Blocks.MTR_STAIRS.get().data))
    );

    public static final CreativeModeTabHolder JCM_CEILING = Registry.createCreativeModeTabHolder(
            new Identifier(Constants.MOD_ID, "ceiling"),
            () -> new ItemStack(new ItemConvertible(Blocks.WRL_STATION_CEILING.get().data))
    );
}
