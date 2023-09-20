package com.lx862.jcm.registry;

import com.lx862.jcm.blocks.entity.SubsidyMachineBlockEntity;
import com.lx862.jcm.util.Logger;
import org.mtr.mapping.registry.BlockEntityTypeRegistryObject;

public final class BlockEntities {
    public static final BlockEntityTypeRegistryObject<SubsidyMachineBlockEntity> SUBSIDY_MACHINE = Registry.registerBlockEntity("subsidy_machine", SubsidyMachineBlockEntity::new, Blocks.SUBSIDY_MACHINE);
    public static void register() {
        // We just load the class and it will be registered, nothing else
        Logger.info("Registering block entity...");
    }
}
