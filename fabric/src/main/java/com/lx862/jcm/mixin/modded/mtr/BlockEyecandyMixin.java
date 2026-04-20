package com.lx862.jcm.mixin.modded.mtr;

import com.lx862.mtrscripting.mod.impl.mtr.MTRContentScripting;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.EyeCandyScriptContext;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.event.BlockUseEvent;
import com.lx862.mtrscripting.core.primitive.ScriptInstance;
import com.lx862.mtrscripting.core.primitive.UniqueKey;
import org.mtr.mapping.holder.*;
import org.mtr.mod.Items;
import org.mtr.mod.block.BlockEyeCandy;
import org.mtr.mod.block.BlockWaterloggable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockEyeCandy.class, remap = false)
public class BlockEyecandyMixin extends BlockWaterloggable {

    public BlockEyecandyMixin(BlockSettings blockSettings) {
        super(blockSettings);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        org.mtr.mapping.holder.BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity != null && blockEntity.data instanceof BlockEyeCandy.BlockEntity) {
            if(blockEntity.getWorld() != null && blockEntity.getWorld().isClient()) {
                /* We should check with ShapeContext, but isHolding is not mapped */
                /* Need replacing in architectury branch */
                boolean notHoldingBrush = MinecraftClient.getInstance().getPlayerMapped() != null && !MinecraftClient.getInstance().getPlayerMapped().isHolding(Items.BRUSH.get());

                if(notHoldingBrush) {
                    ScriptInstance<?> scriptInstance = MTRContentScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("eyecandy", ((BlockEyeCandy.BlockEntity) blockEntity.data).getModelId(), pos.getX(), pos.getY(), pos.getZ()));
                    if (scriptInstance != null && scriptInstance.getContextObject() instanceof EyeCandyScriptContext) {
                        VoxelShape shape = ((EyeCandyScriptContext) scriptInstance.getContextObject()).getOutlineShape();
                        if(shape != null) {
                            return shape;
                        }
                    }
                }
            }
        }
        return VoxelShapes.fullCube();
    }

    @Override
    public VoxelShape getCollisionShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        org.mtr.mapping.holder.BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity != null && blockEntity.data instanceof BlockEyeCandy.BlockEntity) {
            if(blockEntity.getWorld() != null && blockEntity.getWorld().isClient()) {
                /* We should check with ShapeContext, but isHolding is not mapped */
                /* Need replacing in architectury branch */
                boolean notHoldingBrush = MinecraftClient.getInstance().getPlayerMapped() != null && !MinecraftClient.getInstance().getPlayerMapped().isHolding(Items.BRUSH.get());

                if(notHoldingBrush) {
                    ScriptInstance<?> scriptInstance = MTRContentScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("eyecandy", ((BlockEyeCandy.BlockEntity) blockEntity.data).getModelId(), pos.getX(), pos.getY(), pos.getZ()));
                    if (scriptInstance != null && scriptInstance.getContextObject() instanceof EyeCandyScriptContext) {
                        VoxelShape shape = ((EyeCandyScriptContext) scriptInstance.getContextObject()).getCollisionShape();
                        if(shape != null) {
                            return shape;
                        }
                    }
                }
            }
        }
        return VoxelShapes.empty();
    }

    @Inject(method = "onUse2", at = @At("RETURN"), cancellable = true)
    public void jcm$onEyecandyBlockUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        ActionResult returnValue = cir.getReturnValue();
        if(returnValue == ActionResult.SUCCESS) return;

        if(world.isClient()) {
            if(!player.isHolding(Items.BRUSH.get())) {
                org.mtr.mapping.holder.BlockEntity blockEntity = world.getBlockEntity(pos);
                if (blockEntity != null && blockEntity.data instanceof BlockEyeCandy.BlockEntity) {
                    ScriptInstance<?> scriptInstance = MTRContentScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("eyecandy", ((BlockEyeCandy.BlockEntity)blockEntity.data).getModelId(), pos.getX(), pos.getY(), pos.getZ()));
                    if(scriptInstance != null && scriptInstance.getContextObject() instanceof EyeCandyScriptContext) {
                        ((EyeCandyScriptContext)scriptInstance.getContextObject()).events().onBlockUse.trigger(new BlockUseEvent(player, player.getStackInHand(hand)));
                        cir.setReturnValue(ActionResult.SUCCESS);
                    }
                }
            }
        } else {
            cir.setReturnValue(ActionResult.SUCCESS); // Don't allow block placing by default, we don't know what scripts do with it in the client.
        }
    }
}
