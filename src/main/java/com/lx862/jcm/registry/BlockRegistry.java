package com.lx862.jcm.registry;

import com.lx862.jcm.blocks.*;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.block.Block;
import net.minecraft.client.render.RenderLayer;

public class BlockRegistry {
    public static final Block CEILING_SLANTED = new CeilingSlantedBlock(Block.Settings.create().strength(4.0f).nonOpaque());
    public static final Block HK_EXIT_SIGN_ODD = new HKExitSignOdd(Block.Settings.create().strength(4.0f).nonOpaque().luminance(state -> 15));
    public static final Block LIGHT_LANTERN = new LightLanternBlock(Block.Settings.create().strength(4.0f).nonOpaque().luminance(state -> 15));
    public static final Block MTR_STAIRS = new MTRStairsBlock(Block.Settings.create().strength(4.0f));
    public static final Block OPERATOR_BUTTON = new OperatorButtonBlock(Block.Settings.create().strength(4.0f).nonOpaque().luminance(state -> 5), 40);
    public static final Block SPOT_LAMP = new SpotLampBlock(Block.Settings.create().strength(4.0f).nonOpaque().luminance(state -> 15));
    public static final Block WRL_STATION_CEILING = new WRLStationCeilingBlock(Block.Settings.create().strength(4.0f).nonOpaque());
    public static final Block WRL_STATION_CEILING_POLE = new WRLStationCeilingPole(Block.Settings.create().strength(4.0f).nonOpaque());
    public static final Block TCL_EMG_STOP_BUTTON = new TCLEmergencyButtonBlock(Block.Settings.create().strength(4.0f).nonOpaque());
    public static final Block TML_EMG_STOP_BUTTON = new TMLEmergencyButtonBlock(Block.Settings.create().strength(4.0f).nonOpaque().luminance(state -> 15));
    public static final Block SIL_EMG_STOP_BUTTON = new SILEmergencyButtonBlock(Block.Settings.create().strength(4.0f).nonOpaque().luminance(state -> 10));
    public static final Block TRAIN_MODEL_E44 = new HKTrainModelBlock(Block.Settings.create().strength(4.0f).nonOpaque());
    public static final Block MTR_TRESPASS_SIGN = new MTRTrespassSignageBlock(Block.Settings.create().strength(4.0f).nonOpaque());
    public static final Block KCR_TRESPASS_SIGN = new KCRTrespassSignageBlock(Block.Settings.create().strength(4.0f).nonOpaque());
    public static final Block LRT_TRESPASS_SIGN = new LRTTrespassSignageBlock(Block.Settings.create().strength(4.0f).nonOpaque());
    public static final Block WATER_MACHINE = new WaterMachineBlock(Block.Settings.create().strength(4.0f).nonOpaque());

    public static void register() {
        RegistryHelper.registerBlockItem("ceiling_slanted", CEILING_SLANTED, ItemGroupRegistry.TYPE.MAIN);
        RegistryHelper.registerBlockItem("hk_exit_sign_odd", HK_EXIT_SIGN_ODD, ItemGroupRegistry.TYPE.MAIN);
        RegistryHelper.registerBlockItem("light_lantern", LIGHT_LANTERN, ItemGroupRegistry.TYPE.MAIN);
        RegistryHelper.registerBlockItem("mtr_stairs", MTR_STAIRS, ItemGroupRegistry.TYPE.MAIN);
        RegistryHelper.registerBlockItem("operator_button", OPERATOR_BUTTON, ItemGroupRegistry.TYPE.MAIN);
        RegistryHelper.registerBlockItem("spot_lamp", SPOT_LAMP, ItemGroupRegistry.TYPE.MAIN);
        RegistryHelper.registerBlockItem("wrl_station_ceiling", WRL_STATION_CEILING, ItemGroupRegistry.TYPE.MAIN);
        RegistryHelper.registerBlockItem("wrl_station_ceiling_pole", WRL_STATION_CEILING_POLE, ItemGroupRegistry.TYPE.MAIN);

        RegistryHelper.registerBlockItem("tcl_emg_stop_button", TCL_EMG_STOP_BUTTON, ItemGroupRegistry.TYPE.MAIN);
        RegistryHelper.registerBlockItem("sil_emg_stop_button", SIL_EMG_STOP_BUTTON, ItemGroupRegistry.TYPE.MAIN);
        RegistryHelper.registerBlockItem("tml_emg_stop_button", TML_EMG_STOP_BUTTON, ItemGroupRegistry.TYPE.MAIN);
        RegistryHelper.registerBlockItem("train_model_e44", TRAIN_MODEL_E44, ItemGroupRegistry.TYPE.MAIN);
        RegistryHelper.registerBlockItem("mtr_trespass_sign", MTR_TRESPASS_SIGN, ItemGroupRegistry.TYPE.MAIN);
        RegistryHelper.registerBlockItem("kcr_trespass_sign", KCR_TRESPASS_SIGN, ItemGroupRegistry.TYPE.MAIN);
        RegistryHelper.registerBlockItem("lrt_trespass_sign", LRT_TRESPASS_SIGN, ItemGroupRegistry.TYPE.MAIN);
        RegistryHelper.registerBlockItem("water_machine", WATER_MACHINE, ItemGroupRegistry.TYPE.MAIN);
    }

    public static void registerClient() {
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(),
                SPOT_LAMP,
                SIL_EMG_STOP_BUTTON,
                TML_EMG_STOP_BUTTON,
                MTR_TRESPASS_SIGN,
                WATER_MACHINE
        );
    }
}
