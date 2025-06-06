package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.block.entity.*;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.mapping.registry.BlockEntityTypeRegistryObject;

public final class BlockEntities {
    public static final BlockEntityTypeRegistryObject<APGDoorDRLBlockEntity> APG_DOOR_DRL = JCMRegistry.registerBlockEntity("apg_door_1", APGDoorDRLBlockEntity::new, Blocks.APG_DOOR_DRL);
    public static final BlockEntityTypeRegistryObject<AutoIronDoorBlockEntity> AUTO_IRON_DOOR = JCMRegistry.registerBlockEntity("auto_iron_door", AutoIronDoorBlockEntity::new, Blocks.AUTO_IRON_DOOR);
    public static final BlockEntityTypeRegistryObject<APGGlassDRLBlockEntity> APG_GLASS_DRL = JCMRegistry.registerBlockEntity("apg_glass_1", APGGlassDRLBlockEntity::new, Blocks.APG_GLASS_DRL);
    public static final BlockEntityTypeRegistryObject<DepartureTimerBlockEntity> DEPARTURE_TIMER = JCMRegistry.registerBlockEntity("departure_timer", DepartureTimerBlockEntity::new, Blocks.DEPARTURE_TIMER);
    public static final BlockEntityTypeRegistryObject<FareSaverBlockEntity> FARE_SAVER = JCMRegistry.registerBlockEntity("faresaver_1", FareSaverBlockEntity::new, Blocks.FARE_SAVER);
    public static final BlockEntityTypeRegistryObject<SignalBlockInvertedEntityRedAbove> SIGNAL_LIGHT_INVERTED_RED_ABOVE = JCMRegistry.registerBlockEntity("signal_light_inverted_1", SignalBlockInvertedEntityRedAbove::new, Blocks.SIGNAL_LIGHT_INVERTED_RED_ABOVE);
    public static final BlockEntityTypeRegistryObject<SignalBlockInvertedEntityRedBelow> SIGNAL_LIGHT_INVERTED_RED_BELOW = JCMRegistry.registerBlockEntity("signal_light_inverted_2", SignalBlockInvertedEntityRedBelow::new, Blocks.SIGNAL_LIGHT_INVERTED_RED_BOTTOM);
    public static final BlockEntityTypeRegistryObject<StaticSignalLightBlockEntity> SIGNAL_LIGHT_RED_BELOW = JCMRegistry.registerBlockEntity("signal_light_red_1", (blockPos, blockState) -> new StaticSignalLightBlockEntity(StaticSignalLightBlockEntity.SignalType.RED_BELOW, blockPos, blockState), Blocks.STATIC_SIGNAL_LIGHT_RED_BELOW);
    public static final BlockEntityTypeRegistryObject<StaticSignalLightBlockEntity> SIGNAL_LIGHT_RED_TOP = JCMRegistry.registerBlockEntity("signal_light_red_2", (blockPos, blockState) -> new StaticSignalLightBlockEntity(StaticSignalLightBlockEntity.SignalType.RED_TOP, blockPos, blockState), Blocks.STATIC_SIGNAL_LIGHT_RED_TOP);
    public static final BlockEntityTypeRegistryObject<StaticSignalLightBlockEntity> SIGNAL_LIGHT_BLUE = JCMRegistry.registerBlockEntity("signal_light_blue", (blockPos, blockState) -> new StaticSignalLightBlockEntity(StaticSignalLightBlockEntity.SignalType.BLUE, blockPos, blockState), Blocks.STATIC_SIGNAL_LIGHT_BLUE);
    public static final BlockEntityTypeRegistryObject<StaticSignalLightBlockEntity> SIGNAL_LIGHT_GREEN = JCMRegistry.registerBlockEntity("signal_light_green", (blockPos, blockState) -> new StaticSignalLightBlockEntity(StaticSignalLightBlockEntity.SignalType.GREEN, blockPos, blockState), Blocks.STATIC_SIGNAL_LIGHT_GREEN);
    public static final BlockEntityTypeRegistryObject<SubsidyMachineBlockEntity> SUBSIDY_MACHINE = JCMRegistry.registerBlockEntity("subsidy_machine", SubsidyMachineBlockEntity::new, Blocks.SUBSIDY_MACHINE);
    public static final BlockEntityTypeRegistryObject<OperatorButtonBlockEntity> OPERATOR_BUTTON = JCMRegistry.registerBlockEntity("operator_button", OperatorButtonBlockEntity::new, Blocks.OPERATOR_BUTTON);
    public static final BlockEntityTypeRegistryObject<SoundLooperBlockEntity> SOUND_LOOPER = JCMRegistry.registerBlockEntity("sound_looper", SoundLooperBlockEntity::new, Blocks.SOUND_LOOPER);
    public static final BlockEntityTypeRegistryObject<ButterflyLightBlockEntity> BUTTERFLY_LIGHT = JCMRegistry.registerBlockEntity("butterfly_light", ButterflyLightBlockEntity::new, Blocks.BUTTERFLY_LIGHT);
    public static final BlockEntityTypeRegistryObject<KCRStationNameSignBlockEntity> KCR_STATION_NAME_SIGN = JCMRegistry.registerBlockEntity("kcr_name_sign", (blockPos, blockState) -> new KCRStationNameSignBlockEntity(blockPos, blockState, false), Blocks.KCR_STATION_NAME_SIGN);
    public static final BlockEntityTypeRegistryObject<KCRStationNameSignBlockEntity> KCR_STATION_NAME_SIGN_STATION_COLOR = JCMRegistry.registerBlockEntity("kcr_name_sign_station_color", (blockPos, blockState) -> new KCRStationNameSignBlockEntity(blockPos, blockState, true), Blocks.KCR_STATION_NAME_SIGN_STATION_COLOR);
    public static final BlockEntityTypeRegistryObject<LCDPIDSBlockEntity> LCD_PIDS = JCMRegistry.registerBlockEntity("pids_4a", LCDPIDSBlockEntity::new, Blocks.LCD_PIDS);
    public static final BlockEntityTypeRegistryObject<PIDSProjectorBlockEntity> PIDS_PROJECTOR = JCMRegistry.registerBlockEntity("pids_projector", PIDSProjectorBlockEntity::new, Blocks.PIDS_PROJECTOR);
    public static final BlockEntityTypeRegistryObject<PIDS1ABlockEntity> PIDS_1A = JCMRegistry.registerBlockEntity("pids_4", PIDS1ABlockEntity::new, Blocks.PIDS_1A);
    public static final BlockEntityTypeRegistryObject<RVPIDSBlockEntity> RV_PIDS = JCMRegistry.registerBlockEntity("pids_5", RVPIDSBlockEntity::new, Blocks.RV_PIDS);
    public static final BlockEntityTypeRegistryObject<RVPIDSSIL1BlockEntity> RV_PIDS_SIL_1 = JCMRegistry.registerBlockEntity("pids_rv_sil", RVPIDSSIL1BlockEntity::new, Blocks.RV_PIDS_SIL_1);
    public static final BlockEntityTypeRegistryObject<RVPIDSSIL2BlockEntity> RV_PIDS_SIL_2 = JCMRegistry.registerBlockEntity("pids_rv_sil_2", RVPIDSSIL2BlockEntity::new, Blocks.RV_PIDS_SIL_2);

    public static final BlockEntityTypeRegistryObject<StationNameStandingBlockEntity> STATION_NAME_STANDING = JCMRegistry.registerBlockEntity("station_name_tall_stand", StationNameStandingBlockEntity::new, Blocks.STATION_NAME_STANDING);

    public static void register() {
        // We just load the class and it will be registered, nothing else
        JCMLogger.debug("Registering block entity...");
    }
}
