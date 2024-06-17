package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.item.ItemDRLAPG;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.mapping.holder.Item;
import org.mtr.mapping.registry.ItemRegistryObject;

public class Items {
    public static final ItemRegistryObject APG_DOOR_DRL = JCMRegistry.registerItem("apg_door_drl", itemSettings -> new Item(new ItemDRLAPG(ItemDRLAPG.EnumPSDAPGItem.DOOR, ItemDRLAPG.EnumPSDAPGType.APG, itemSettings)), ItemGroups.JCM_MAIN);
    public static final ItemRegistryObject APG_GLASS_DRL = JCMRegistry.registerItem("apg_glass_drl", itemSettings -> new Item(new ItemDRLAPG(ItemDRLAPG.EnumPSDAPGItem.GLASS, ItemDRLAPG.EnumPSDAPGType.APG, itemSettings)), ItemGroups.JCM_MAIN);
    public static final ItemRegistryObject APG_GLASS_END_DRL = JCMRegistry.registerItem("apg_glass_end_drl", itemSettings -> new Item(new ItemDRLAPG(ItemDRLAPG.EnumPSDAPGItem.GLASS_END, ItemDRLAPG.EnumPSDAPGType.APG, itemSettings)), ItemGroups.JCM_MAIN);

    public static void register() {
        // We just load the class and it will be registered, nothing else
        JCMLogger.debug("Registering items...");
    }
}
