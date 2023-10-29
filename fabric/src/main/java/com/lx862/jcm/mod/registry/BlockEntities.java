package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.block.entity.*;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.mapping.registry.BlockEntityTypeRegistryObject;

public final class BlockEntities {
    public static final BlockEntityTypeRegistryObject<SubsidyMachineBlockEntity> SUBSIDY_MACHINE = Registry.registerBlockEntity("subsidy_machine", SubsidyMachineBlockEntity::new, Blocks.SUBSIDY_MACHINE);
    public static final BlockEntityTypeRegistryObject<ButterflyLightBlockEntity> BUTTERFLY_LIGHT = Registry.registerBlockEntity("butterfly_light", ButterflyLightBlockEntity::new, Blocks.BUTTERFLY_LIGHT);
    public static final BlockEntityTypeRegistryObject<DepartureTimerBlockEntity> DEPARTURE_TIMER = Registry.registerBlockEntity("departure_timer", DepartureTimerBlockEntity::new, Blocks.DEPARTURE_TIMER);
    public static final BlockEntityTypeRegistryObject<FareSaverBlockEntity> FARE_SAVER = Registry.registerBlockEntity("faresaver_1", FareSaverBlockEntity::new, Blocks.FARE_SAVER);
    public static final BlockEntityTypeRegistryObject<KCRStationNameSignBlockEntity> KCR_STATION_NAME_SIGN = Registry.registerBlockEntity("kcr_name_sign", KCRStationNameSignBlockEntity::new, Blocks.KCR_STATION_NAME_SIGN);
    public static final BlockEntityTypeRegistryObject<PIDSBlockEntity> RV_PIDS = Registry.registerBlockEntity("pids_5", PIDSBlockEntity::new, Blocks.RV_PIDS);
    public static final BlockEntityTypeRegistryObject<StationNameStandingBlockEntity> STATION_NAME_STANDING = Registry.registerBlockEntity("station_name_tall_stand", StationNameStandingBlockEntity::new, Blocks.STATION_NAME_STANDING);

    public static void register() {
        // We just load the class and it will be registered, nothing else
        JCMLogger.debug("Registering block entity...");
    }
}
