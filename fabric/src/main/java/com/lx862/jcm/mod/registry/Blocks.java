package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.block.*;
import com.lx862.jcm.mod.block.APGGlassEndDRL;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.mapping.holder.Block;
import org.mtr.mapping.holder.RenderLayer;
import org.mtr.mapping.mapper.BlockHelper;
import org.mtr.mapping.registry.BlockRegistryObject;

public final class Blocks {
    public static final BlockRegistryObject APG_DOOR_DRL = RegistryHelper.registerBlock("apg_door_drl", () -> new Block(new APGDoorDRL()));
    public static final BlockRegistryObject APG_GLASS_DRL = RegistryHelper.registerBlock("apg_glass_drl", () -> new Block(new APGGlassDRL()));
    public static final BlockRegistryObject APG_GLASS_END_DRL = RegistryHelper.registerBlock("apg_glass_end_drl", () -> new Block(new APGGlassEndDRL()));
    public static final BlockRegistryObject AUTO_IRON_DOOR_BLOCK = RegistryHelper.registerBlockItem("auto_iron_door", () -> new Block(new AutoIronDoorBlock((blockSettings) -> blockSettings.strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject BUTTERFLY_LIGHT = RegistryHelper.registerBlockItem("butterfly_light", () -> new Block(new ButterflyLightBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject BUFFER_STOP = RegistryHelper.registerBlockItem("buffer_stop", () -> new Block(new BufferStopBlock(BlockHelper.createBlockSettings(false, state -> 8).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject CIRCLE_WALL_1 = RegistryHelper.registerBlockItem("circle_wall_1", () -> new Block(new CircleWallBlock(BlockHelper.createBlockSettings(false).strength(4.0f))), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject CIRCLE_WALL_2 = RegistryHelper.registerBlockItem("circle_wall_2", () -> new Block(new CircleWallBlock(BlockHelper.createBlockSettings(false).strength(4.0f))), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject CIRCLE_WALL_3 = RegistryHelper.registerBlockItem("circle_wall_3", () -> new Block(new CircleWallBlock(BlockHelper.createBlockSettings(false).strength(4.0f))), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject CIRCLE_WALL_4 = RegistryHelper.registerBlockItem("circle_wall_4", () -> new Block(new CircleWallBlock(BlockHelper.createBlockSettings(false).strength(4.0f))), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject CIRCLE_WALL_5 = RegistryHelper.registerBlockItem("circle_wall_5", () -> new Block(new CircleWallBlock(BlockHelper.createBlockSettings(false).strength(4.0f))), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject CIRCLE_WALL_6 = RegistryHelper.registerBlockItem("circle_wall_6", () -> new Block(new CircleWallBlock(BlockHelper.createBlockSettings(false).strength(4.0f))), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject CIRCLE_WALL_7 = RegistryHelper.registerBlockItem("circle_wall_7", () -> new Block(new CircleWallBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject CEILING_SLANTED = RegistryHelper.registerBlockItem("ceiling_slanted", () -> new Block(new CeilingSlantedBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject DEPARTURE_POLE = RegistryHelper.registerBlockItem("departure_pole", () -> new Block(new DeparturePoleBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject DEPARTURE_TIMER = RegistryHelper.registerBlockItem("departure_timer", () -> new Block(new DepartureTimerBlock(BlockHelper.createBlockSettings(false, state -> 4).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject EXIT_SIGN_ODD = RegistryHelper.registerBlockItem("exit_sign_odd", () -> new Block(new ExitSignOdd(BlockHelper.createBlockSettings(false, state -> 15).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject EXIT_SIGN_EVEN = RegistryHelper.registerBlockItem("exit_sign_even", () -> new Block(new ExitSignEven(BlockHelper.createBlockSettings(false, state -> 15).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject FIRE_ALARM = RegistryHelper.registerBlockItem("fire_alarm", () -> new Block(new FireAlarmWall(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject FARE_SAVER = RegistryHelper.registerBlockItem("faresaver", () -> new Block(new FareSaverBlock(BlockHelper.createBlockSettings(false, state -> 15).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject KCR_STATION_NAME_SIGN = RegistryHelper.registerBlockItem("kcr_name_sign", () -> new Block(new KCRStationNameSignBlock(BlockHelper.createBlockSettings(false, state -> 15).strength(4.0f).nonOpaque(), false)), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject KCR_STATION_NAME_SIGN_STATION_COLOR = RegistryHelper.registerBlockItem("kcr_name_sign_station_color", () -> new Block(new KCRStationNameSignBlock(BlockHelper.createBlockSettings(false, state -> 15).strength(4.0f).nonOpaque(), true)), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject LIGHT_BLOCK = RegistryHelper.registerBlockItem("light_block", () -> new Block(new LightBlock(BlockHelper.createBlockSettings(false, state -> BlockUtil.getProperty(state, BlockProperties.LIGHT_LEVEL)).strength(1.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject LIGHT_LANTERN = RegistryHelper.registerBlockItem("light_lantern", () -> new Block(new LightLanternBlock(BlockHelper.createBlockSettings(false, state -> 15).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject MTR_STAIRS = RegistryHelper.registerBlockItem("mtr_stairs", () -> new Block(new MTRStairsBlock(BlockHelper.createBlockSettings(false).strength(4.0f))), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject OPERATOR_BUTTON = RegistryHelper.registerBlockItem("operator_button", () -> new Block(new OperatorButtonBlock(BlockHelper.createBlockSettings(false, state -> 5).nonOpaque(), 40)), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject SPOT_LAMP = RegistryHelper.registerBlockItem("spot_lamp", () -> new Block(new SpotLampBlock(BlockHelper.createBlockSettings(false, state -> 15).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject SUBSIDY_MACHINE = RegistryHelper.registerBlockItem("subsidy_machine", () -> new Block(new SubsidyMachineBlock(BlockHelper.createBlockSettings(false).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject SOUND_LOOPER = RegistryHelper.registerBlockItem("sound_looper", () -> new Block(new SoundLooperBlock(BlockHelper.createBlockSettings(false))), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject STATION_NAME_STANDING = RegistryHelper.registerBlockItem("station_name_standing", () -> new Block(new StationNameStandingBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject STATION_CEILING_WRL = RegistryHelper.registerBlockItem("station_ceiling_wrl", () -> new Block(new StationCeilingWRL2Block(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.JCM_CEILING);
     public static final BlockRegistryObject STATION_CEILING_WRL_SINGLE = RegistryHelper.registerBlockItem("station_ceiling_wrl_single", () -> new Block(new StationCeilingWRLBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.JCM_CEILING);
    public static final BlockRegistryObject STATION_CEILING_WRL_STATION_COLOR = RegistryHelper.registerBlockItem("station_ceiling_wrl_station_color", () -> new Block(new StationCeilingWRL2Block(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.JCM_CEILING);
    public static final BlockRegistryObject STATION_CEILING_WRL_SINGLE_STATION_COLOR = RegistryHelper.registerBlockItem("station_ceiling_wrl_single_station_color", () -> new Block(new StationCeilingWRLBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.JCM_CEILING);
    public static final BlockRegistryObject STATION_CEILING_WRL_POLE = RegistryHelper.registerBlockItem("station_ceiling_wrl_pole", () -> new Block(new StationCeilingWRL2Pole(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.JCM_CEILING);
     public static final BlockRegistryObject STATION_CEILING_WRL_POLE_SINGLE = RegistryHelper.registerBlockItem("station_ceiling_wrl_single_pole", () -> new Block(new StationCeilingWRLPole(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.JCM_CEILING);
    public static final BlockRegistryObject THALES_TICKET_BARRIER_ENTRANCE = RegistryHelper.registerBlockItem("thales_ticket_barrier_entrance", () -> new Block(new ThalesTicketBarrier(true)), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject THALES_TICKET_BARRIER_EXIT = RegistryHelper.registerBlockItem("thales_ticket_barrier_exit", () -> new Block(new ThalesTicketBarrier(false)), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject THALES_TICKET_BARRIER_BARE = RegistryHelper.registerBlockItem("thales_ticket_barrier_bare", () -> new Block(new ThalesTicketBarrierBareBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject HELPLINE_1 = RegistryHelper.registerBlockItem("helpline_1", () -> new Block(new WallAttachedHelpLineBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject HELPLINE_2 = RegistryHelper.registerBlockItem("helpline_2", () -> new Block(new WallAttachedHelpLineBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject HELPLINE_STANDING = RegistryHelper.registerBlockItem("helpline_standing", () -> new Block(new HelpLineStandingBlock(BlockHelper.createBlockSettings(false, state -> 15).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject HELPLINE_STANDING_EAL = RegistryHelper.registerBlockItem("helpline_standing_eal", () -> new Block(new HelpLineStandingEALBlock(BlockHelper.createBlockSettings(false, state -> 15).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject KCR_EMG_STOP_SIGN = RegistryHelper.registerBlockItem("kcr_emg_stop_sign", () -> new Block(new KCREmergencyStopSign(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject KCR_ENQUIRY_MACHINE = RegistryHelper.registerBlockItem("kcr_enquiry_machine", () -> new Block(new KCREnquiryMachineWall(BlockHelper.createBlockSettings(false, state -> 4).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject KCR_TRESPASS_SIGN = RegistryHelper.registerBlockItem("kcr_trespass_sign", () -> new Block(new KCRTrespassSignageBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject MTR_ENQUIRY_MACHINE = RegistryHelper.registerBlockItem("mtr_enquiry_machine", () -> new Block(new MTREnquiryMachine(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject MTR_ENQUIRY_MACHINE_WALL = RegistryHelper.registerBlockItem("mtr_enquiry_machine_wall", () -> new Block(new MTREnquiryMachineWall(BlockHelper.createBlockSettings(false, state -> 4).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject PIDS_1A = RegistryHelper.registerBlockItem("pids_1a", () -> new Block(new PIDS1ABlock()), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject LCD_PIDS = RegistryHelper.registerBlockItem("pids_lcd", () -> new Block(new LCDPIDSBlock(BlockHelper.createBlockSettings(false, state -> 8).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject RV_PIDS = RegistryHelper.registerBlockItem("pids_rv", () -> new Block(new RVPIDSBlock(BlockHelper.createBlockSettings(false, state -> 8).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject RV_PIDS_POLE = RegistryHelper.registerBlockItem("rv_pids_pole", () -> new Block(new RVPIDSPole(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject RV_ENQUIRY_MACHINE = RegistryHelper.registerBlockItem("rv_enquiry_machine", () -> new Block(new RVEnquiryMachine(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject TCL_EMG_STOP_BUTTON = RegistryHelper.registerBlockItem("tcl_emg_stop_button", () -> new Block(new TCLEmergencyButtonBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject TML_EMG_STOP_BUTTON = RegistryHelper.registerBlockItem("tml_emg_stop_button", () -> new Block(new TMLEmergencyButtonBlock(BlockHelper.createBlockSettings(false, state -> 15).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject SIL_EMG_STOP_BUTTON = RegistryHelper.registerBlockItem("sil_emg_stop_button", () -> new Block(new SILEmergencyButtonBlock(BlockHelper.createBlockSettings(false, state -> 10).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject TRAIN_MODEL_E44 = RegistryHelper.registerBlockItem("train_model_e44", () -> new Block(new MTRTrainModelBlock(BlockHelper.createBlockSettings(false).strength(4.0f))), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject SIGNAL_LIGHT_INVERTED_RED_ABOVE = RegistryHelper.registerBlockItem("signal_light_inverted_1", () -> new Block(new InvertedSignalBlockRedAbove(BlockHelper.createBlockSettings(false).strength(4.0f))), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject SIGNAL_LIGHT_INVERTED_RED_BOTTOM = RegistryHelper.registerBlockItem("signal_light_inverted_2", () -> new Block(new InvertedSignalBlockRedBelow(BlockHelper.createBlockSettings(false).strength(4.0f))), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject STATIC_SIGNAL_LIGHT_RED_BELOW = RegistryHelper.registerBlockItem("signal_light_red_1", () -> new Block(new StaticSignalLightBlockRedBelow(BlockHelper.createBlockSettings(false).strength(4.0f))), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject STATIC_SIGNAL_LIGHT_RED_TOP = RegistryHelper.registerBlockItem("signal_light_red_2", () -> new Block(new StaticSignalLightBlockRedTop(BlockHelper.createBlockSettings(false).strength(4.0f))), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject STATIC_SIGNAL_LIGHT_GREEN = RegistryHelper.registerBlockItem("signal_light_green", () -> new Block(new StaticSignalLightBlockGreen(BlockHelper.createBlockSettings(false).strength(4.0f))), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject STATIC_SIGNAL_LIGHT_BLUE = RegistryHelper.registerBlockItem("signal_light_blue", () -> new Block(new StaticSignalLightBlockBlue(BlockHelper.createBlockSettings(false).strength(4.0f))), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject MTR_TRESPASS_SIGN = RegistryHelper.registerBlockItem("mtr_trespass_sign", () -> new Block(new MTRTrespassSignageBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject LRT_TRESPASS_SIGN = RegistryHelper.registerBlockItem("lrt_trespass_sign", () -> new Block(new LRTTrespassSignageBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject LRT_INTER_CAR_BARRIER_LEFT = RegistryHelper.registerBlockItem("lrt_inter_car_barrier_left", () -> new Block(new LRTInterCarBarrierBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject LRT_INTER_CAR_BARRIER_MIDDLE = RegistryHelper.registerBlockItem("lrt_inter_car_barrier_middle", () -> new Block(new LRTInterCarBarrierBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject LRT_INTER_CAR_BARRIER_RIGHT = RegistryHelper.registerBlockItem("lrt_inter_car_barrier_right", () -> new Block(new LRTInterCarBarrierBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);
    public static final BlockRegistryObject WATER_MACHINE = RegistryHelper.registerBlockItem("water_machine", () -> new Block(new WaterMachineBlock(BlockHelper.createBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.JCM_MAIN);

    public static void register() {
        JCMLogger.debug("Registering blocks...");
        // Calling this method should cause the static class to be loaded, in turn registering the content.
    }

    public static void registerClient() {
        /* Define Render Layer (Texture transparency etc.) */
        RegistryHelperClient.registerBlockRenderType(RenderLayer.getCutout(),
                APG_DOOR_DRL,
                APG_GLASS_DRL,
                APG_GLASS_END_DRL,
                AUTO_IRON_DOOR_BLOCK,
                BUFFER_STOP,
                BUTTERFLY_LIGHT,
                CIRCLE_WALL_1,
                CIRCLE_WALL_2,
                CIRCLE_WALL_3,
                CIRCLE_WALL_4,
                CIRCLE_WALL_5,
                CIRCLE_WALL_6,
                FIRE_ALARM,
                FARE_SAVER,
                KCR_STATION_NAME_SIGN,
                KCR_STATION_NAME_SIGN_STATION_COLOR,
                KCR_EMG_STOP_SIGN,
                MTR_ENQUIRY_MACHINE,
                RV_ENQUIRY_MACHINE,
                SUBSIDY_MACHINE,
                SPOT_LAMP,
                HELPLINE_2,
                HELPLINE_1,
                HELPLINE_STANDING,
                HELPLINE_STANDING_EAL,
                SIL_EMG_STOP_BUTTON,
                STATION_NAME_STANDING,
                TML_EMG_STOP_BUTTON,
                THALES_TICKET_BARRIER_ENTRANCE,
                THALES_TICKET_BARRIER_EXIT,
                MTR_TRESPASS_SIGN,
                WATER_MACHINE
        );

        RegistryHelperClient.registerBlockRenderType(RenderLayer.getTranslucent(),
                Blocks.THALES_TICKET_BARRIER_BARE
        );

        /* Station Colored Blocks */
        RegistryHelperClient.registerStationColoredBlock(
                KCR_STATION_NAME_SIGN_STATION_COLOR,
                STATION_NAME_STANDING,
                STATION_CEILING_WRL_STATION_COLOR,
                STATION_CEILING_WRL_SINGLE_STATION_COLOR
        );
    }
}
