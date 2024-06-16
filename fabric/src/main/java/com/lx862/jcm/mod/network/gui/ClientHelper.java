package com.lx862.jcm.mod.network.gui;

import com.lx862.jcm.mod.render.gui.screen.*;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.Screen;

public final class ClientHelper {

    public static void openButterflyLightScreen(BlockPos blockPos, int secondsToBlink) {
		MinecraftClient.getInstance().openScreen(new Screen(new ButterflyLightScreen(blockPos, secondsToBlink)));
	}

	public static void openFareSaverGUIScreen(BlockPos blockPos, String prefix, int discount) {
		MinecraftClient.getInstance().openScreen(new Screen(new FareSaverScreen(blockPos, prefix, discount)));
	}

	public static void openPIDSGUIScreen(BlockPos blockPos, String[] customMessages, boolean[] rowHidden, boolean hidePlatformNumber, String presetId) {
		MinecraftClient.getInstance().openScreen(new Screen(new PIDSScreen(blockPos, customMessages, rowHidden, hidePlatformNumber, presetId)));
	}

	public static void openSoundLooperGUIScreen(BlockPos blockPos, BlockPos corner1, BlockPos corner2, String soundId, int soundCategory, float soundVolume, int interval, boolean needRedstone, boolean limitRange) {
		MinecraftClient.getInstance().openScreen(new Screen(new SoundLooperScreen(blockPos, corner1, corner2, soundId, soundCategory, soundVolume, interval, needRedstone, limitRange)));
	}

	public static void openSubsidyMachineGUIScreen(BlockPos blockPos, int pricePerUse,int cooldown) {
		MinecraftClient.getInstance().openScreen(new Screen(new SubsidyMachineScreen(blockPos, pricePerUse, cooldown)));
	}
}
