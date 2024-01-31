package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.block.entity.*;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.mapping.registry.BlockEntityTypeRegistryObject;

public final class BlockEntities {
    public static final BlockEntityTypeRegistryObject<SubsidyMachineBlockEntity> SUBSIDY_MACHINE = RegistryHelper.registerBlockEntity("subsidy_machine", SubsidyMachineBlockEntity::new, Blocks.SUBSIDY_MACHINE);
    public static final BlockEntityTypeRegistryObject<ButterflyLightBlockEntity> BUTTERFLY_LIGHT = RegistryHelper.registerBlockEntity("butterfly_light", ButterflyLightBlockEntity::new, Blocks.BUTTERFLY_LIGHT);
    public static final BlockEntityTypeRegistryObject<DepartureTimerBlockEntity> DEPARTURE_TIMER = RegistryHelper.registerBlockEntity("departure_timer", DepartureTimerBlockEntity::new, Blocks.DEPARTURE_TIMER);
    public static final BlockEntityTypeRegistryObject<FareSaverBlockEntity> FARE_SAVER = RegistryHelper.registerBlockEntity("faresaver_1", FareSaverBlockEntity::new, Blocks.FARE_SAVER);
    public static final BlockEntityTypeRegistryObject<KCRStationNameSignBlockEntity> KCR_STATION_NAME_SIGN = RegistryHelper.registerBlockEntity("kcr_name_sign", (blockPos, blockState) -> new KCRStationNameSignBlockEntity(blockPos, blockState, false), Blocks.KCR_STATION_NAME_SIGN);
    public static final BlockEntityTypeRegistryObject<KCRStationNameSignBlockEntity> KCR_STATION_NAME_SIGN_STATION_COLOR = RegistryHelper.registerBlockEntity("kcr_name_sign_station_color", (blockPos, blockState) -> new KCRStationNameSignBlockEntity(blockPos, blockState, true), Blocks.KCR_STATION_NAME_SIGN_STATION_COLOR);
    public static final BlockEntityTypeRegistryObject<LCDPIDSBlockEntity> LCD_PIDS = RegistryHelper.registerBlockEntity("pids_4", LCDPIDSBlockEntity::new, Blocks.LCD_PIDS);
    public static final BlockEntityTypeRegistryObject<RVPIDSBlockEntity> RV_PIDS = RegistryHelper.registerBlockEntity("pids_5", RVPIDSBlockEntity::new, Blocks.RV_PIDS);
    public static final BlockEntityTypeRegistryObject<StationNameStandingBlockEntity> STATION_NAME_STANDING = RegistryHelper.registerBlockEntity("station_name_tall_stand", StationNameStandingBlockEntity::new, Blocks.STATION_NAME_STANDING);

    public static void register() {
        // We just load the class and it will be registered, nothing else
        JCMLogger.debug("Registering block entity...");
    }
}
