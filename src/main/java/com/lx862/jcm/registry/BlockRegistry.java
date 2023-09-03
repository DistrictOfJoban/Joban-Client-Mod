package com.lx862.jcm.registry;

import com.lx862.jcm.blocks.*;
import com.lx862.jcm.util.Logger;
import net.minecraft.block.AbstractBlock;
import org.mtr.mapping.holder.Block;
import org.mtr.mapping.holder.BlockSettings;
import org.mtr.mapping.registry.BlockRegistryObject;

public final class BlockRegistry {

    static {
        CEILING_SLANTED = Registry.registerBlockItem("ceiling_slanted", new Block(new CeilingSlantedBlock(BlockSettings.create().strength(4.0f).nonOpaque())), ItemGroupRegistry.JCM_MAIN);
        HK_EXIT_SIGN_ODD = Registry.registerBlockItem("hk_exit_sign_odd", new Block(new HKExitSignOdd(BlockSettings.create().strength(4.0f).nonOpaque().luminance(state -> 15))), ItemGroupRegistry.JCM_MAIN);
        LIGHT_LANTERN = Registry.registerBlockItem("light_lantern", new Block(new LightLanternBlock(BlockSettings.create().strength(4.0f).nonOpaque().luminance(state -> 15))), ItemGroupRegistry.JCM_MAIN);
        MTR_STAIRS = Registry.registerBlockItem("mtr_stairs", new Block(new MTRStairsBlock(AbstractBlock.Settings.create().strength(4.0f))), ItemGroupRegistry.JCM_MAIN);
        OPERATOR_BUTTON = Registry.registerBlockItem("operator_button", new Block(new OperatorButtonBlock(BlockSettings.create().strength(4.0f).nonOpaque().luminance(state -> 5), 40)), ItemGroupRegistry.JCM_MAIN);
        SPOT_LAMP = Registry.registerBlockItem("spot_lamp", new Block(new SpotLampBlock(BlockSettings.create().strength(4.0f).nonOpaque().luminance(state -> 15))), ItemGroupRegistry.JCM_MAIN);
        WRL_STATION_CEILING = Registry.registerBlockItem("wrl_station_ceiling", new Block(new WRLStationCeilingBlock(BlockSettings.create().strength(4.0f).nonOpaque())), ItemGroupRegistry.JCM_MAIN);
        WRL_STATION_CEILING_POLE = Registry.registerBlockItem("wrl_station_ceiling_pole", new Block(new WRLStationCeilingPole(BlockSettings.create().strength(4.0f).nonOpaque())), ItemGroupRegistry.JCM_MAIN);
        TCL_EMG_STOP_BUTTON = Registry.registerBlockItem("tcl_emg_stop_button", new Block(new TCLEmergencyButtonBlock(BlockSettings.create().strength(4.0f).nonOpaque())), ItemGroupRegistry.JCM_MAIN);
        TML_EMG_STOP_BUTTON = Registry.registerBlockItem("tml_emg_stop_button", new Block(new TMLEmergencyButtonBlock(BlockSettings.create().strength(4.0f).nonOpaque().luminance(state -> 15))), ItemGroupRegistry.JCM_MAIN);
        SIL_EMG_STOP_BUTTON = Registry.registerBlockItem("sil_emg_stop_button", new Block(new SILEmergencyButtonBlock(BlockSettings.create().strength(4.0f).nonOpaque().luminance(state -> 10))), ItemGroupRegistry.JCM_MAIN);
        TRAIN_MODEL_E44 = Registry.registerBlockItem("train_model_e44", new Block(new HKTrainModelBlock(BlockSettings.create().strength(4.0f).nonOpaque())), ItemGroupRegistry.JCM_MAIN);
        MTR_TRESPASS_SIGN = Registry.registerBlockItem("mtr_trespass_sign", new Block(new MTRTrespassSignageBlock(BlockSettings.create().strength(4.0f).nonOpaque())), ItemGroupRegistry.JCM_MAIN);
        KCR_TRESPASS_SIGN = Registry.registerBlockItem("kcr_trespass_sign", new Block(new KCRTrespassSignageBlock(BlockSettings.create().strength(4.0f).nonOpaque())), ItemGroupRegistry.JCM_MAIN);
        LRT_TRESPASS_SIGN = Registry.registerBlockItem("lrt_trespass_sign", new Block(new LRTTrespassSignageBlock(BlockSettings.create().strength(4.0f).nonOpaque())), ItemGroupRegistry.JCM_MAIN);
        WATER_MACHINE = Registry.registerBlockItem("water_machine", new Block(new WaterMachineBlock(BlockSettings.create().strength(4.0f).nonOpaque())), ItemGroupRegistry.JCM_MAIN);
    }

    public static final BlockRegistryObject CEILING_SLANTED;
    public static final BlockRegistryObject HK_EXIT_SIGN_ODD;
    public static final BlockRegistryObject LIGHT_LANTERN;
    public static final BlockRegistryObject MTR_STAIRS;
    public static final BlockRegistryObject OPERATOR_BUTTON;
    public static final BlockRegistryObject SPOT_LAMP;
    public static final BlockRegistryObject WRL_STATION_CEILING;
    public static final BlockRegistryObject WRL_STATION_CEILING_POLE;
    public static final BlockRegistryObject TCL_EMG_STOP_BUTTON;
    public static final BlockRegistryObject TML_EMG_STOP_BUTTON;
    public static final BlockRegistryObject SIL_EMG_STOP_BUTTON;
    public static final BlockRegistryObject TRAIN_MODEL_E44;
    public static final BlockRegistryObject MTR_TRESPASS_SIGN;
    public static final BlockRegistryObject KCR_TRESPASS_SIGN;
    public static final BlockRegistryObject LRT_TRESPASS_SIGN;
    public static final BlockRegistryObject WATER_MACHINE;

    public static void register() {
        // We just load the class and it will be registered, nothing else
        Logger.info("Registering blocks...");
    }
}
