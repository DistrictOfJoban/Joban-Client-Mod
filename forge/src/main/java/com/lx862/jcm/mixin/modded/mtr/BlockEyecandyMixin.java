package com.lx862.jcm.mixin.modded.mtr;

import com.lx862.jcm.mod.scripting.mtr.MTRScripting;
import com.lx862.jcm.mod.scripting.mtr.eyecandy.EyeCandyScriptContext;
import com.lx862.mtrscripting.core.ScriptInstance;
import com.lx862.mtrscripting.data.UniqueKey;
import org.mtr.mapping.holder.*;
import org.mtr.mod.Items;
import org.mtr.mod.block.BlockEyeCandy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockEyeCandy.class, remap = false)
public class BlockEyecandyMixin {
    @Inject(method = "onUse2", at = @At("HEAD"), cancellable = true)
    public void jcm$onEyecandyBlockUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if(world.isClient()) {
            if(!player.isHolding(Items.BRUSH.get())) {
                BlockEntity blockEntity = world.getBlockEntity(pos);
                if (blockEntity != null && blockEntity.data instanceof BlockEyeCandy.BlockEntity) {
                    ScriptInstance<?> scriptInstance = MTRScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("eyecandy", ((BlockEyeCandy.BlockEntity)blockEntity.data).getModelId(), pos.getX(), pos.getY(), pos.getZ()));
                    if(scriptInstance != null && scriptInstance.getScriptContext() instanceof EyeCandyScriptContext) {
                        ((EyeCandyScriptContext)scriptInstance.getScriptContext()).events().triggerOnBlockUse();
                        cir.setReturnValue(ActionResult.SUCCESS);
                    }
                }
            }
        } else {
            cir.setReturnValue(ActionResult.SUCCESS); // Don't allow block placing by default, we don't know what scripts do with it in the client.
        }
    }
}
