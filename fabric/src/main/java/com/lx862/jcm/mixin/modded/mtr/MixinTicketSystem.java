package com.lx862.jcm.mixin.modded.mtr;

import com.lx862.jcm.mod.block.FareSaverBlock;
import com.lx862.jcm.mod.data.TransactionLog;
import com.lx862.jcm.mod.data.TransactionEntry;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.core.data.Station;
import org.mtr.mapping.holder.PlayerEntity;
import org.mtr.mapping.holder.Text;
import org.mtr.mapping.holder.World;
import org.mtr.mod.data.TicketSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = TicketSystem.class, remap = false)
public abstract class MixinTicketSystem {
    @Shadow
    private static void incrementPlayerScore(World world, PlayerEntity player, String objective, String title, int value) {
    }

    @Inject(method = "onExit", at = @At(value = "INVOKE", target = "Lorg/mtr/mod/data/TicketSystem;incrementPlayerScore(Lorg/mtr/mapping/holder/World;Lorg/mtr/mapping/holder/PlayerEntity;Ljava/lang/String;Ljava/lang/String;I)V"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private static void onExit(World world, Station station, PlayerEntity player, boolean remindIfNoRecord, CallbackInfoReturnable<Boolean> cir, int entryZone1, int entryZone2, int entryZone3, boolean entered, long fare, long finalFare) {
        long finalDeductedAmount = -finalFare;

        if(entered && FareSaverBlock.discountList.containsKey(player.getUuid())) {
            long subsidyAmount = Math.min(finalFare, FareSaverBlock.discountList.get(player.getUuid()));
            incrementPlayerScore(world, player, "mtr_balance", "Balance", (int) subsidyAmount);
            finalDeductedAmount += subsidyAmount;

            if (subsidyAmount < 0 && finalFare > 0) {
                player.sendMessage(Text.cast(TextUtil.translatable(TextCategory.HUD, "faresaver.saved_sarcasm", -subsidyAmount)), false);
            } else if (subsidyAmount > 0) {
                player.sendMessage(Text.cast(TextUtil.translatable(TextCategory.HUD, "faresaver.saved", subsidyAmount)), false);
            }

            FareSaverBlock.discountList.remove(player.getUuid());
        }

        TransactionLog.writeLog(player, new TransactionEntry(station.getName(), finalDeductedAmount, System.currentTimeMillis()));
    }

    @Inject(method = "addBalance", at = @At("TAIL"))
    private static void addBalance(World world, PlayerEntity player, int amount, CallbackInfo ci) {
        if (!world.isClient()) {
            TransactionLog.writeLog(player, new TransactionEntry("Add Value", amount, System.currentTimeMillis()));
        }
    }
}