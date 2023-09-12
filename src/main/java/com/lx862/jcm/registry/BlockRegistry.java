package com.lx862.jcm.registry;

import com.lx862.jcm.blocks.*;
import com.lx862.jcm.util.Logger;
import net.minecraft.block.AbstractBlock;
import org.mtr.mapping.holder.BlockSettings;
import org.mtr.mapping.registry.BlockRegistryObject;

public final class BlockRegistry {
    public static final BlockRegistryObject BUFFER_STOP = Registry.registerBlockItem("buffer_stop", new BufferStopBlock(BlockSettings.create().strength(4.0f).nonOpaque().luminance(state -> 8)), ItemGroupRegistry.JCM_MAIN);
    public static final BlockRegistryObject CEILING_SLANTED = Registry.registerBlockItem("ceiling_slanted", new CeilingSlantedBlock(BlockSettings.create().strength(4.0f).nonOpaque()), ItemGroupRegistry.JCM_MAIN);
    public static final BlockRegistryObject DEPARTURE_POLE = Registry.registerBlockItem("departure_pole", new DeparturePoleBlock(BlockSettings.create().strength(4.0f).nonOpaque()), ItemGroupRegistry.JCM_MAIN);
    public static final BlockRegistryObject HK_EXIT_SIGN_ODD = Registry.registerBlockItem("hk_exit_sign_odd", new HKExitSignOdd(BlockSettings.create().strength(4.0f).nonOpaque().luminance(state -> 15)), ItemGroupRegistry.JCM_MAIN);
    public static final BlockRegistryObject LIGHT_LANTERN = Registry.registerBlockItem("light_lantern", new LightLanternBlock(BlockSettings.create().strength(4.0f).nonOpaque().luminance(state -> 15)), ItemGroupRegistry.JCM_MAIN);
    public static final BlockRegistryObject MTR_STAIRS = Registry.registerBlockItem("mtr_stairs", new MTRStairsBlock(AbstractBlock.Settings.create().strength(4.0f)), ItemGroupRegistry.JCM_MAIN);
    public static final BlockRegistryObject OPERATOR_BUTTON = Registry.registerBlockItem("operator_button", new OperatorButtonBlock(BlockSettings.create().strength(4.0f).nonOpaque().luminance(state -> 5), 40), ItemGroupRegistry.JCM_MAIN);
    public static final BlockRegistryObject SPOT_LAMP = Registry.registerBlockItem("spot_lamp", new SpotLampBlock(BlockSettings.create().strength(4.0f).nonOpaque().luminance(state -> 15)), ItemGroupRegistry.JCM_MAIN);
    public static final BlockRegistryObject SUBSIDY_MACHINE = Registry.registerBlockItem("subsidy_machine", new SubsidyMachineBlock(BlockSettings.create().strength(4.0f).nonOpaque()), ItemGroupRegistry.JCM_MAIN);
    public static final BlockRegistryObject WRL_STATION_CEILING = Registry.registerBlockItem("wrl_station_ceiling", new WRLStationCeilingBlock(BlockSettings.create().strength(4.0f).nonOpaque()), ItemGroupRegistry.JCM_MAIN);
    public static final BlockRegistryObject WRL_STATION_CEILING_POLE = Registry.registerBlockItem("wrl_station_ceiling_pole", new WRLStationCeilingPole(BlockSettings.create().strength(4.0f).nonOpaque()), ItemGroupRegistry.JCM_MAIN);
    public static final BlockRegistryObject HELPLINE_1 = Registry.registerBlockItem("helpline_1", new WallAttachedHelpLineBlock(BlockSettings.create().strength(4.0f).nonOpaque()), ItemGroupRegistry.JCM_MAIN);
    public static final BlockRegistryObject HELPLINE_2 = Registry.registerBlockItem("helpline_2", new WallAttachedHelpLineBlock(BlockSettings.create().strength(4.0f).nonOpaque()), ItemGroupRegistry.JCM_MAIN);
    public static final BlockRegistryObject HELPLINE_STANDING = Registry.registerBlockItem("helpline_standing", new HelpLineStandingBlock(BlockSettings.create().strength(4.0f).nonOpaque().luminance(state -> 15)), ItemGroupRegistry.JCM_MAIN);
    public static final BlockRegistryObject HELPLINE_STANDING_EAL = Registry.registerBlockItem("helpline_standing_eal", new HelpLineStandingEALBlock(BlockSettings.create().strength(4.0f).nonOpaque().luminance(state -> 15)), ItemGroupRegistry.JCM_MAIN);
    public static final BlockRegistryObject MTR_ENQUIRY_MACHINE = Registry.registerBlockItem("mtr_enquiry_machine", new MTREnquiryMachine(BlockSettings.create().strength(4.0f).nonOpaque()), ItemGroupRegistry.JCM_MAIN);
    public static final BlockRegistryObject MTR_ENQUIRY_MACHINE_WALL = Registry.registerBlockItem("mtr_enquiry_machine_wall", new MTREnquiryMachineWall(BlockSettings.create().strength(4.0f).nonOpaque().luminance(state -> 4)), ItemGroupRegistry.JCM_MAIN);
    public static final BlockRegistryObject RV_ENQUIRY_MACHINE = Registry.registerBlockItem("rv_enquiry_machine", new RVEnquiryMachine(BlockSettings.create().strength(4.0f).nonOpaque()), ItemGroupRegistry.JCM_MAIN);
    public static final BlockRegistryObject KCR_ENQUIRY_MACHINE = Registry.registerBlockItem("kcr_enquiry_machine", new KCREnquiryMachineWall(BlockSettings.create().strength(4.0f).nonOpaque().luminance(state -> 4)), ItemGroupRegistry.JCM_MAIN);
    public static final BlockRegistryObject TCL_EMG_STOP_BUTTON = Registry.registerBlockItem("tcl_emg_stop_button", new TCLEmergencyButtonBlock(BlockSettings.create().strength(4.0f).nonOpaque()), ItemGroupRegistry.JCM_MAIN);
    public static final BlockRegistryObject TML_EMG_STOP_BUTTON = Registry.registerBlockItem("tml_emg_stop_button", new TMLEmergencyButtonBlock(BlockSettings.create().strength(4.0f).nonOpaque().luminance(state -> 15)), ItemGroupRegistry.JCM_MAIN);
    public static final BlockRegistryObject SIL_EMG_STOP_BUTTON = Registry.registerBlockItem("sil_emg_stop_button", new SILEmergencyButtonBlock(BlockSettings.create().strength(4.0f).nonOpaque().luminance(state -> 10)), ItemGroupRegistry.JCM_MAIN);
    public static final BlockRegistryObject TRAIN_MODEL_E44 = Registry.registerBlockItem("train_model_e44", new HKTrainModelBlock(BlockSettings.create().strength(4.0f).nonOpaque()), ItemGroupRegistry.JCM_MAIN);
    public static final BlockRegistryObject MTR_TRESPASS_SIGN = Registry.registerBlockItem("mtr_trespass_sign", new MTRTrespassSignageBlock(BlockSettings.create().strength(4.0f).nonOpaque()), ItemGroupRegistry.JCM_MAIN);
    public static final BlockRegistryObject KCR_TRESPASS_SIGN = Registry.registerBlockItem("kcr_trespass_sign", new KCRTrespassSignageBlock(BlockSettings.create().strength(4.0f).nonOpaque()), ItemGroupRegistry.JCM_MAIN);
    public static final BlockRegistryObject LRT_TRESPASS_SIGN = Registry.registerBlockItem("lrt_trespass_sign", new LRTTrespassSignageBlock(BlockSettings.create().strength(4.0f).nonOpaque()), ItemGroupRegistry.JCM_MAIN);
    public static final BlockRegistryObject LRT_INTER_CAR_BARRIER_LEFT = Registry.registerBlockItem("lrt_inter_car_barrier_left", new LRTInterCarBarrierBlock(BlockSettings.create().strength(4.0f).nonOpaque()), ItemGroupRegistry.JCM_MAIN);
    public static final BlockRegistryObject LRT_INTER_CAR_BARRIER_MIDDLE = Registry.registerBlockItem("lrt_inter_car_barrier_middle", new LRTInterCarBarrierBlock(BlockSettings.create().strength(4.0f).nonOpaque()), ItemGroupRegistry.JCM_MAIN);
    public static final BlockRegistryObject LRT_INTER_CAR_BARRIER_RIGHT = Registry.registerBlockItem("lrt_inter_car_barrier_right", new LRTInterCarBarrierBlock(BlockSettings.create().strength(4.0f).nonOpaque()), ItemGroupRegistry.JCM_MAIN);
    public static final BlockRegistryObject WATER_MACHINE = Registry.registerBlockItem("water_machine", new WaterMachineBlock(BlockSettings.create().strength(4.0f).nonOpaque()), ItemGroupRegistry.JCM_MAIN);

    public static void register() {
        // We just load the class and it will be registered, nothing else
        Logger.info("Registering blocks...");
    }
}
