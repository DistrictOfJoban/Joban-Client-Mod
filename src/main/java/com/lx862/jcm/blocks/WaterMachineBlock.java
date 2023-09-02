package com.lx862.jcm.blocks;

import com.lx862.jcm.blocks.base.VerticalMultiBlock;
import com.lx862.jcm.util.Utils;
import com.lx862.jcm.util.VoxelUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class WaterMachineBlock extends VerticalMultiBlock {
    public WaterMachineBlock(Settings settings) {
        super(settings, 2);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return VoxelUtil.getDirectionalShape16(state.get(FACING),2.5, 0, 2.5, 13.5, 16, 13.5);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {

        if(player.isHolding(Items.GLASS_BOTTLE)) {
            Utils.decrementItemFromPlayerHand(player, hand, 1);
            fillBottleForPlayer(player);
            return ActionResult.SUCCESS;
        }

        if(player.isHolding(Items.BUCKET)) {
            Utils.decrementItemFromPlayerHand(player, hand, 1);
            fillBucketForPlayer(player);
            return ActionResult.SUCCESS;
        }

        return ActionResult.FAIL;
    }

    private static void fillBottleForPlayer(PlayerEntity player) {
        ItemStack newWaterBottle = new ItemStack(Items.POTION);
        PotionUtil.setPotion(newWaterBottle, Potions.WATER);
        player.getInventory().offerOrDrop(newWaterBottle);
        playSplashSoundToPlayer(player);
    }

    private static void fillBucketForPlayer(PlayerEntity player) {
        ItemStack newWaterBucket = new ItemStack(Items.WATER_BUCKET);
        player.getInventory().offerOrDrop(newWaterBucket);
        playSplashSoundToPlayer(player);
    }

    private static void playSplashSoundToPlayer(PlayerEntity player) {
        player.playSound(SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1F, 1F);
    }
}