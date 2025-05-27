package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.block.*;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.mapping.holder.Block;
import org.mtr.mapping.holder.RenderLayer;
import org.mtr.mapping.registry.BlockRegistryObject;
import org.mtr.mod.block.IBlock;

import static com.lx862.jcm.mod.block.SpotLampBlock.LIT;

public final class Blocks {
    public static final BlockRegistryObject APG_DOOR_DRL = JCMRegistry.registerBlock("apg_door_drl", () -> new Block(new APGDoorDRL()));
    public static final BlockRegistryObject APG_GLASS_DRL = JCMRegistry.registerBlock("apg_glass_drl", () -> new Block(new APGGlassDRL()));
    public static final BlockRegistryObject APG_GLASS_END_DRL = JCMRegistry.registerBlock("apg_glass_end_drl", () -> new Block(new APGGlassEndDRL()));
    public static final BlockRegistryObject AUTO_IRON_DOOR = JCMRegistry.registerBlockItem("auto_iron_door", () -> new Block(new AutoIronDoorBlock((blockSettings) -> blockSettings.strength(4.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject BUTTERFLY_LIGHT = JCMRegistry.registerBlockItem("butterfly_light", () -> new Block(new ButterflyLightBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(3.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject BUFFER_STOP = JCMRegistry.registerBlockItem("buffer_stop", () -> new Block(new BufferStopBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false, state -> 8).strength(4.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject CEILING_SLANTED = JCMRegistry.registerBlockItem("ceiling_slanted", () -> new Block(new CeilingSlantedBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(2.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject CIRCLE_WALL_1 = JCMRegistry.registerBlockItem("circle_wall_1", () -> new Block(new CircleWallBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(8.0f))), ItemGroups.MAIN);
    public static final BlockRegistryObject CIRCLE_WALL_2 = JCMRegistry.registerBlockItem("circle_wall_2", () -> new Block(new CircleWallBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(8.0f))), ItemGroups.MAIN);
    public static final BlockRegistryObject CIRCLE_WALL_3 = JCMRegistry.registerBlockItem("circle_wall_3", () -> new Block(new CircleWallBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(8.0f))), ItemGroups.MAIN);
    public static final BlockRegistryObject CIRCLE_WALL_4 = JCMRegistry.registerBlockItem("circle_wall_4", () -> new Block(new CircleWallBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(8.0f))), ItemGroups.MAIN);
    public static final BlockRegistryObject CIRCLE_WALL_5 = JCMRegistry.registerBlockItem("circle_wall_5", () -> new Block(new CircleWallBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(8.0f))), ItemGroups.MAIN);
    public static final BlockRegistryObject CIRCLE_WALL_6 = JCMRegistry.registerBlockItem("circle_wall_6", () -> new Block(new CircleWallBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(8.0f))), ItemGroups.MAIN);
    public static final BlockRegistryObject CIRCLE_WALL_7 = JCMRegistry.registerBlockItem("circle_wall_7", () -> new Block(new CircleWallBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(8.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject DEPARTURE_POLE = JCMRegistry.registerBlockItem("departure_pole", () -> new Block(new DeparturePoleBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(3.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject DEPARTURE_TIMER = JCMRegistry.registerBlockItem("departure_timer", () -> new Block(new DepartureTimerBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false, state -> 4).strength(3.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject EXIT_SIGN_ODD = JCMRegistry.registerBlockItem("exit_sign_odd", () -> new Block(new ExitSignOdd(org.mtr.mod.Blocks.createDefaultBlockSettings(false, state -> 15).strength(1.5f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject EXIT_SIGN_EVEN = JCMRegistry.registerBlockItem("exit_sign_even", () -> new Block(new ExitSignEven(org.mtr.mod.Blocks.createDefaultBlockSettings(false, state -> 15).strength(1.5f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject FIRE_ALARM = JCMRegistry.registerBlockItem("fire_alarm", () -> new Block(new FireAlarmWall(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(1.5f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject FARE_SAVER = JCMRegistry.registerBlockItem("fare_saver", () -> new Block(new FareSaverBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false, state -> 15).strength(4.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject HELPLINE_1 = JCMRegistry.registerBlockItem("helpline_1", () -> new Block(new WallAttachedHelpLineBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(1.5f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject HELPLINE_2 = JCMRegistry.registerBlockItem("helpline_2", () -> new Block(new WallAttachedHelpLineBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(1.5f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject HELPLINE_HKWK = JCMRegistry.registerBlockItem("helpline_hkwk", () -> new Block(new WallAttachedHelpLineBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject HELPLINE_STANDING = JCMRegistry.registerBlockItem("helpline_standing", () -> new Block(new HelpLineStandingBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false, state -> 15).strength(4.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject HELPLINE_STANDING_EAL = JCMRegistry.registerBlockItem("helpline_standing_eal", () -> new Block(new HelpLineStandingEALBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false, state -> 15).strength(4.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject KCR_EMG_STOP_SIGN = JCMRegistry.registerBlockItem("kcr_emg_stop_sign", () -> new Block(new KCREmergencyStopSign(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject KCR_ENQUIRY_MACHINE = JCMRegistry.registerBlockItem("kcr_enquiry_machine", () -> new Block(new KCREnquiryMachineWall(org.mtr.mod.Blocks.createDefaultBlockSettings(false, state -> 4).strength(4.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject KCR_STATION_NAME_SIGN = JCMRegistry.registerBlockItem("kcr_name_sign", () -> new Block(new KCRStationNameSignBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false, state -> 15).strength(4.0f).nonOpaque(), false)), ItemGroups.MAIN);
    public static final BlockRegistryObject KCR_STATION_NAME_SIGN_STATION_COLOR = JCMRegistry.registerBlockItem("kcr_name_sign_station_color", () -> new Block(new KCRStationNameSignBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false, state -> 15).strength(4.0f).nonOpaque(), true)), ItemGroups.MAIN);
    public static final BlockRegistryObject KCR_TRESPASS_SIGN = JCMRegistry.registerBlockItem("kcr_trespass_sign", () -> new Block(new KCRTrespassSignageBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject LCD_PIDS = JCMRegistry.registerBlockItem("lcd_pids", () -> new Block(new LCDPIDSBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false, state -> 8).strength(2.0f).nonOpaque())), ItemGroups.PIDS);
    public static final BlockRegistryObject LIGHT_BLOCK = JCMRegistry.registerBlockItem("light_block", () -> new Block(new LightBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false, state -> IBlock.getStatePropertySafe(state, BlockProperties.LIGHT_LEVEL)).strength(1.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject LIGHT_LANTERN = JCMRegistry.registerBlockItem("light_lantern", () -> new Block(new LightLanternBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false, state -> IBlock.getStatePropertySafe(state, LIT) ? 15 : 0).strength(4.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject LRT_TRESPASS_SIGN = JCMRegistry.registerBlockItem("lrt_trespass_sign", () -> new Block(new LRTTrespassSignageBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject LRT_INTER_CAR_BARRIER_LEFT = JCMRegistry.registerBlockItem("lrt_inter_car_barrier_left", () -> new Block(new LRTInterCarBarrierBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject LRT_INTER_CAR_BARRIER_MIDDLE = JCMRegistry.registerBlockItem("lrt_inter_car_barrier_middle", () -> new Block(new LRTInterCarBarrierBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject LRT_INTER_CAR_BARRIER_RIGHT = JCMRegistry.registerBlockItem("lrt_inter_car_barrier_right", () -> new Block(new LRTInterCarBarrierBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject MTR_ENQUIRY_MACHINE = JCMRegistry.registerBlockItem("mtr_enquiry_machine", () -> new Block(new MTREnquiryMachine(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject MTR_ENQUIRY_MACHINE_WALL = JCMRegistry.registerBlockItem("mtr_enquiry_machine_wall", () -> new Block(new MTREnquiryMachineWall(org.mtr.mod.Blocks.createDefaultBlockSettings(false, state -> 4).strength(4.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject MTR_STAIRS = JCMRegistry.registerBlockItem("mtr_stairs", () -> new Block(new MTRStairsBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(4.0f))), ItemGroups.MAIN);
    public static final BlockRegistryObject MTR_TRESPASS_SIGN = JCMRegistry.registerBlockItem("mtr_trespass_sign", () -> new Block(new MTRTrespassSignageBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject OPERATOR_BUTTON = JCMRegistry.registerBlockItem("operator_button", () -> new Block(new OperatorButtonBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false, state -> 5).strength(1.0f).nonOpaque(), 40)), ItemGroups.MAIN);
    public static final BlockRegistryObject PIDS_PROJECTOR = JCMRegistry.registerBlockItem("pids_projector", () -> new Block(new PIDSProjectorBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(2.0f).nonOpaque())), ItemGroups.PIDS);
    public static final BlockRegistryObject PIDS_1A = JCMRegistry.registerBlockItem("pids_1a", () -> new Block(new PIDS1ABlock()), ItemGroups.PIDS);
    public static final BlockRegistryObject RV_PIDS = JCMRegistry.registerBlockItem("rv_pids", () -> new Block(new RVPIDSBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false, state -> 8).strength(2.0f).nonOpaque())), ItemGroups.PIDS);
    public static final BlockRegistryObject RV_PIDS_SIL_1 = JCMRegistry.registerBlockItem("rv_pids_sil_1", () -> new Block(new RVPIDSSIL1Block(org.mtr.mod.Blocks.createDefaultBlockSettings(false, state -> 8).strength(2.0f).nonOpaque())), ItemGroups.PIDS);
    public static final BlockRegistryObject RV_PIDS_SIL_2 = JCMRegistry.registerBlockItem("rv_pids_sil_2", () -> new Block(new RVPIDSSIL2Block(org.mtr.mod.Blocks.createDefaultBlockSettings(false, state -> 8).strength(2.0f).nonOpaque())), ItemGroups.PIDS);
    public static final BlockRegistryObject RV_PIDS_POLE = JCMRegistry.registerBlockItem("rv_pids_pole", () -> new Block(new RVPIDSPole(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(2.0f).nonOpaque())), ItemGroups.PIDS);
    public static final BlockRegistryObject RV_ENQUIRY_MACHINE = JCMRegistry.registerBlockItem("rv_enquiry_machine", () -> new Block(new RVEnquiryMachine(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject SIL_EMG_STOP_BUTTON = JCMRegistry.registerBlockItem("sil_emg_stop_button", () -> new Block(new SILEmergencyButtonBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false, state -> 10).strength(4.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject SIGNAL_LIGHT_INVERTED_RED_ABOVE = JCMRegistry.registerBlockItem("signal_light_inverted_1", () -> new Block(new InvertedSignalBlockRedAbove(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(1.0f))), ItemGroups.MAIN);
    public static final BlockRegistryObject SIGNAL_LIGHT_INVERTED_RED_BOTTOM = JCMRegistry.registerBlockItem("signal_light_inverted_2", () -> new Block(new InvertedSignalBlockRedBelow(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(1.0f))), ItemGroups.MAIN);
    public static final BlockRegistryObject STATIC_SIGNAL_LIGHT_RED_BELOW = JCMRegistry.registerBlockItem("signal_light_red_1", () -> new Block(new StaticSignalLightBlockRedBelow(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(1.0f))), ItemGroups.MAIN);
    public static final BlockRegistryObject STATIC_SIGNAL_LIGHT_RED_TOP = JCMRegistry.registerBlockItem("signal_light_red_2", () -> new Block(new StaticSignalLightBlockRedTop(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(1.0f))), ItemGroups.MAIN);
    public static final BlockRegistryObject STATIC_SIGNAL_LIGHT_GREEN = JCMRegistry.registerBlockItem("signal_light_green", () -> new Block(new StaticSignalLightBlockGreen(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(1.0f))), ItemGroups.MAIN);
    public static final BlockRegistryObject STATIC_SIGNAL_LIGHT_BLUE = JCMRegistry.registerBlockItem("signal_light_blue", () -> new Block(new StaticSignalLightBlockBlue(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(1.0f))), ItemGroups.MAIN);
    public static final BlockRegistryObject SPOT_LAMP = JCMRegistry.registerBlockItem("spot_lamp", () -> new Block(new SpotLampBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false, state -> IBlock.getStatePropertySafe(state, LIT) ? 15 : 0).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject STATION_NAME_STANDING = JCMRegistry.registerBlock("station_name_standing", () -> new Block(new StationNameStandingBlock()));
    public static final BlockRegistryObject SUBSIDY_MACHINE = JCMRegistry.registerBlockItem("subsidy_machine", () -> new Block(new SubsidyMachineBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject SOUND_LOOPER = JCMRegistry.registerBlockItem("sound_looper", () -> new Block(new SoundLooperBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false))), ItemGroups.MAIN);
    public static final BlockRegistryObject STATION_CEILING_WRL = JCMRegistry.registerBlockItem("station_ceiling_wrl", () -> new Block(new StationCeilingWRL2Block(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject STATION_CEILING_WRL_SINGLE = JCMRegistry.registerBlockItem("station_ceiling_wrl_single", () -> new Block(new StationCeilingWRLBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject STATION_CEILING_WRL_STATION_COLOR = JCMRegistry.registerBlockItem("station_ceiling_wrl_station_color", () -> new Block(new StationCeilingWRL2Block(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject STATION_CEILING_WRL_SINGLE_STATION_COLOR = JCMRegistry.registerBlockItem("station_ceiling_wrl_single_station_color", () -> new Block(new StationCeilingWRLBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject STATION_CEILING_WRL_POLE = JCMRegistry.registerBlockItem("station_ceiling_wrl_pole", () -> new Block(new StationCeilingWRL2Pole(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject STATION_CEILING_WRL_POLE_SINGLE = JCMRegistry.registerBlockItem("station_ceiling_wrl_single_pole", () -> new Block(new StationCeilingWRLPole(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject TCL_EMG_STOP_BUTTON = JCMRegistry.registerBlockItem("tcl_emg_stop_button", () -> new Block(new TCLEmergencyButtonBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject THALES_TICKET_BARRIER_ENTRANCE = JCMRegistry.registerBlockItem("thales_ticket_barrier_entrance", () -> new Block(new ThalesTicketBarrier(true)), ItemGroups.MAIN);
    public static final BlockRegistryObject THALES_TICKET_BARRIER_EXIT = JCMRegistry.registerBlockItem("thales_ticket_barrier_exit", () -> new Block(new ThalesTicketBarrier(false)), ItemGroups.MAIN);
    public static final BlockRegistryObject THALES_TICKET_BARRIER_BARE = JCMRegistry.registerBlockItem("thales_ticket_barrier_bare", () -> new Block(new ThalesTicketBarrierBareBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject TML_EMG_STOP_BUTTON = JCMRegistry.registerBlockItem("tml_emg_stop_button", () -> new Block(new TMLEmergencyButtonBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false, state -> 15).strength(4.0f).nonOpaque())), ItemGroups.MAIN);
    public static final BlockRegistryObject TRAIN_MODEL_E44 = JCMRegistry.registerBlockItem("train_model_e44", () -> new Block(new MTRTrainModelBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(0.5f))), ItemGroups.MAIN);
    public static final BlockRegistryObject WATER_MACHINE = JCMRegistry.registerBlockItem("water_machine", () -> new Block(new WaterMachineBlock(org.mtr.mod.Blocks.createDefaultBlockSettings(false).strength(4.0f).nonOpaque())), ItemGroups.MAIN);

    public static void register() {
        JCMLogger.debug("Registering blocks...");
        // Calling this method cause the static class to be loaded, in turn registering the content.
    }

    public static void registerClient() {
        /* Define Render Layer (Texture transparency etc.) */
        JCMRegistryClient.registerBlockRenderType(RenderLayer.getCutout(),
            APG_DOOR_DRL,
            APG_GLASS_DRL,
            APG_GLASS_END_DRL,
            AUTO_IRON_DOOR,
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
            RV_PIDS_SIL_1,
            RV_PIDS_SIL_2,
            SUBSIDY_MACHINE,
            SPOT_LAMP,
            HELPLINE_1,
            HELPLINE_2,
            HELPLINE_HKWK,
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

        JCMRegistryClient.registerBlockRenderType(RenderLayer.getTranslucent(),
            Blocks.THALES_TICKET_BARRIER_BARE
        );

        /* Station Colored Blocks */
        JCMRegistryClient.registerStationColoredBlock(
            KCR_STATION_NAME_SIGN_STATION_COLOR,
            STATION_NAME_STANDING,
            STATION_CEILING_WRL_STATION_COLOR,
            STATION_CEILING_WRL_SINGLE_STATION_COLOR
        );
    }
}
