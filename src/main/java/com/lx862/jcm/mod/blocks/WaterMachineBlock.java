package com.lx862.jcm.mod.blocks;

import com.lx862.jcm.mod.blocks.base.Vertical2Block;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.Utils;
import com.lx862.jcm.mod.util.VoxelUtil;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import org.mtr.mapping.holder.*;

public class WaterMachineBlock extends Vertical2Block {
    public WaterMachineBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), 2.5, 0, 2.5, 13.5, 16, 13.5);
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (player.isHolding(Items.getGlassBottleMapped())) {
            Utils.decrementItemFromPlayerHand(player, hand, 1);
            fillBottleForPlayer(player);
            return ActionResult.SUCCESS;
        }

        if (player.isHolding(Items.getBucketMapped())) {
            Utils.decrementItemFromPlayerHand(player, hand, 1);
            fillBucketForPlayer(player);
            return ActionResult.SUCCESS;
        }

        return ActionResult.FAIL;
    }

    private static void fillBottleForPlayer(PlayerEntity player) {
        ItemStack newWaterBottle = new ItemStack(new net.minecraft.item.ItemStack(Items.getPotionMapped().data));
        PotionUtil.setPotion(newWaterBottle.data, Potions.WATER);
        player.getInventory().offerOrDrop(newWaterBottle);
        playSplashSoundToPlayer(player);
    }

    private static void fillBucketForPlayer(PlayerEntity player) {
        ItemStack newWaterBucket = new ItemStack(new net.minecraft.item.ItemStack(Items.getWaterBucketMapped().data));
        player.getInventory().offerOrDrop(newWaterBucket);
        playSplashSoundToPlayer(player);
    }

    private static void playSplashSoundToPlayer(PlayerEntity player) {
        player.playSound(new SoundEvent(net.minecraft.sound.SoundEvents.ITEM_BUCKET_FILL), SoundCategory.BLOCKS, 1F, 1F);
    }
}