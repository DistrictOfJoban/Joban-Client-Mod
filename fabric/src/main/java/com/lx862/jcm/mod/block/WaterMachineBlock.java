package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.Vertical2Block;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.PlayerHelper;
import org.mtr.mod.block.IBlock;

public class WaterMachineBlock extends Vertical2Block {
    public WaterMachineBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return IBlock.getVoxelShapeByDirection(2.5, 0, 2.5, 13.5, 16, 13.5, IBlock.getStatePropertySafe(state, FACING));
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if(player.isHolding(Items.getGlassBottleMapped())) {
            fillBottleForPlayer(player, hand);
            return ActionResult.SUCCESS;
        }

        if(player.isHolding(Items.getBucketMapped())) {
            fillBucketForPlayer(player, hand);
            return ActionResult.SUCCESS;
        }

        return ActionResult.FAIL;
    }

    private static void fillBottleForPlayer(PlayerEntity player, Hand hand) {
        ItemStack newWaterBottle = new ItemStack(ItemConvertible.cast(Items.getPotionMapped()));
        newWaterBottle.getOrCreateTag().putString("Potion", "water");
        offerOrDrop(player, hand, newWaterBottle);
        playSplashSoundToPlayer(player);
    }

    private static void fillBucketForPlayer(PlayerEntity player, Hand hand) {
        ItemStack newWaterBucket = new ItemStack(ItemConvertible.cast(Items.getWaterBucketMapped()));
        offerOrDrop(player, hand, newWaterBucket);
        playSplashSoundToPlayer(player);
    }

    private static void offerOrDrop(PlayerEntity player, Hand hand, ItemStack stack) {
        ItemStack playerHolding = player.getStackInHand(hand);
        playerHolding.decrement(1);

        if(playerHolding.isEmpty()) {
            player.setStackInHand(hand, stack);
        } else {
            PlayerHelper.getPlayerInventory(player).insertStack(stack);
        }
    }

    private static void playSplashSoundToPlayer(PlayerEntity player) {
        player.playSound(SoundEvents.getItemBucketFillMapped(), SoundCategory.BLOCKS, 1F, 1F);
    }
}