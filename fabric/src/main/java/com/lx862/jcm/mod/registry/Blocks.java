package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.block.*;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.mapping.mapper.BlockHelper;
import org.mtr.mapping.registry.BlockRegistryObject;

public final class Blocks {
    public static final BlockRegistryObject BUTTERFLY_LIGHT = Registry.registerBlockItem("butterfly_light", new ButterflyLightBlock(BlockHelper.createBlockSettings(false, state -> 8).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject BUFFER_STOP = Registry.registerBlockItem("buffer_stop", new BufferStopBlock(BlockHelper.createBlockSettings(false, state -> 8).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject CIRCLE_WALL_1 = Registry.registerBlockItem("circle_wall_1", new CircleWallBlock(BlockHelper.createBlockSettings(false).strength(4.0f)), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject CIRCLE_WALL_2 = Registry.registerBlockItem("circle_wall_2", new CircleWallBlock(BlockHelper.createBlockSettings(false).strength(4.0f)), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject CIRCLE_WALL_3 = Registry.registerBlockItem("circle_wall_3", new CircleWallBlock(BlockHelper.createBlockSettings(false).strength(4.0f)), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject CIRCLE_WALL_4 = Registry.registerBlockItem("circle_wall_4", new CircleWallBlock(BlockHelper.createBlockSettings(false).strength(4.0f)), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject CIRCLE_WALL_5 = Registry.registerBlockItem("circle_wall_5", new CircleWallBlock(BlockHelper.createBlockSettings(false).strength(4.0f)), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject CIRCLE_WALL_6 = Registry.registerBlockItem("circle_wall_6", new CircleWallBlock(BlockHelper.createBlockSettings(false).strength(4.0f)), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject CIRCLE_WALL_7 = Registry.registerBlockItem("circle_wall_7", new CircleWallBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject CEILING_SLANTED = Registry.registerBlockItem("ceiling_slanted", new CeilingSlantedBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject DEPARTURE_POLE = Registry.registerBlockItem("departure_pole", new DeparturePoleBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject DEPARTURE_TIMER = Registry.registerBlockItem("departure_timer", new DepartureTimerBlock(BlockHelper.createBlockSettings(false, state -> 4).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject EXIT_SIGN_ODD = Registry.registerBlockItem("exit_sign_odd", new ExitSignOdd(BlockHelper.createBlockSettings(false, state -> 15).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject EXIT_SIGN_EVEN = Registry.registerBlockItem("exit_sign_even", new ExitSignEven(BlockHelper.createBlockSettings(false, state -> 15).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject FIRE_ALARM = Registry.registerBlockItem("fire_alarm", new FireAlarmWall(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject FARE_SAVER = Registry.registerBlockItem("faresaver", new FareSaverBlock(BlockHelper.createBlockSettings(false, state -> 15).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject KCR_STATION_NAME_SIGN = Registry.registerBlockItem("kcr_name_sign", new KCRStationNameSignBlock(BlockHelper.createBlockSettings(false, state -> 15).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject LIGHT_LANTERN = Registry.registerBlockItem("light_lantern", new LightLanternBlock(BlockHelper.createBlockSettings(false, state -> 15).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject MTR_STAIRS = Registry.registerBlockItem("mtr_stairs", new MTRStairsBlock(BlockHelper.createBlockSettings(false).strength(4.0f).data), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject OPERATOR_BUTTON = Registry.registerBlockItem("operator_button", new OperatorButtonBlock(BlockHelper.createBlockSettings(false, state -> 5).nonOpaque(), 40), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject SPOT_LAMP = Registry.registerBlockItem("spot_lamp", new SpotLampBlock(BlockHelper.createBlockSettings(false, state -> 15).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject SUBSIDY_MACHINE = Registry.registerBlockItem("subsidy_machine", new SubsidyMachineBlock(BlockHelper.createBlockSettings(false).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject STATION_NAME_STANDING = Registry.registerBlockItem("station_name_standing", new StationNameStandingBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject THALES_TICKET_BARRIER_BARE = Registry.registerBlockItem("thales_ticket_barrier_bare", new ThalesTicketBarrierBareBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject WRL_STATION_CEILING = Registry.registerBlockItem("wrl_station_ceiling", new WRLStationCeilingBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque()), ItemGroups.JCM_CEILING);
    public static final BlockRegistryObject WRL_STATION_CEILING_POLE = Registry.registerBlockItem("wrl_station_ceiling_pole", new WRLStationCeilingPole(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque()), ItemGroups.JCM_CEILING);
    public static final BlockRegistryObject HELPLINE_1 = Registry.registerBlockItem("helpline_1", new WallAttachedHelpLineBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject HELPLINE_2 = Registry.registerBlockItem("helpline_2", new WallAttachedHelpLineBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject HELPLINE_STANDING = Registry.registerBlockItem("helpline_standing", new HelpLineStandingBlock(BlockHelper.createBlockSettings(false, state -> 15).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject HELPLINE_STANDING_EAL = Registry.registerBlockItem("helpline_standing_eal", new HelpLineStandingEALBlock(BlockHelper.createBlockSettings(false, state -> 15).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject MTR_ENQUIRY_MACHINE = Registry.registerBlockItem("mtr_enquiry_machine", new MTREnquiryMachine(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject MTR_ENQUIRY_MACHINE_WALL = Registry.registerBlockItem("mtr_enquiry_machine_wall", new MTREnquiryMachineWall(BlockHelper.createBlockSettings(false, state -> 4).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject LCD_PIDS = Registry.registerBlockItem("pids_lcd", new LCDPIDSBlock(BlockHelper.createBlockSettings(false, state -> 8).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject RV_PIDS = Registry.registerBlockItem("pids_rv", new RVPIDSBlock(BlockHelper.createBlockSettings(false, state -> 8).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject RV_PIDS_POLE = Registry.registerBlockItem("rv_pids_pole", new RVPIDSPole(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject RV_ENQUIRY_MACHINE = Registry.registerBlockItem("rv_enquiry_machine", new RVEnquiryMachine(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject KCR_EMG_STOP_SIGN = Registry.registerBlockItem("kcr_emg_stop_sign", new KCREmergencyStopSign(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject KCR_ENQUIRY_MACHINE = Registry.registerBlockItem("kcr_enquiry_machine", new KCREnquiryMachineWall(BlockHelper.createBlockSettings(false, state -> 4).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject TCL_EMG_STOP_BUTTON = Registry.registerBlockItem("tcl_emg_stop_button", new TCLEmergencyButtonBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject TML_EMG_STOP_BUTTON = Registry.registerBlockItem("tml_emg_stop_button", new TMLEmergencyButtonBlock(BlockHelper.createBlockSettings(false, state -> 15).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject SIL_EMG_STOP_BUTTON = Registry.registerBlockItem("sil_emg_stop_button", new SILEmergencyButtonBlock(BlockHelper.createBlockSettings(false, state -> 10).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject TRAIN_MODEL_E44 = Registry.registerBlockItem("train_model_e44", new MTRTrainModelBlock(BlockHelper.createBlockSettings(false).strength(4.0f)), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject MTR_TRESPASS_SIGN = Registry.registerBlockItem("mtr_trespass_sign", new MTRTrespassSignageBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject KCR_TRESPASS_SIGN = Registry.registerBlockItem("kcr_trespass_sign", new KCRTrespassSignageBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject LRT_TRESPASS_SIGN = Registry.registerBlockItem("lrt_trespass_sign", new LRTTrespassSignageBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject LRT_INTER_CAR_BARRIER_LEFT = Registry.registerBlockItem("lrt_inter_car_barrier_left", new LRTInterCarBarrierBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject LRT_INTER_CAR_BARRIER_MIDDLE = Registry.registerBlockItem("lrt_inter_car_barrier_middle", new LRTInterCarBarrierBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject LRT_INTER_CAR_BARRIER_RIGHT = Registry.registerBlockItem("lrt_inter_car_barrier_right", new LRTInterCarBarrierBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject WATER_MACHINE = Registry.registerBlockItem("water_machine", new WaterMachineBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque()), ItemGroups.JCM_MAIN);

    public static void register() {
        // We just load the class and it will be registered, nothing else
        JCMLogger.debug("Registering blocks...");
    }
}
