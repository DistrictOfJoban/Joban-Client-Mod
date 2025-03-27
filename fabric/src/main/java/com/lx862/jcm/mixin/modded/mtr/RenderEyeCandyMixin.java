package com.lx862.jcm.mixin.modded.mtr;

import com.lx862.jcm.mod.JCMClient;
import com.lx862.jcm.mod.resources.MTRContentResourceManager;
import com.lx862.jcm.mod.scripting.eyecandy.EyeCandyScriptContext;
import com.lx862.jcm.mod.scripting.eyecandy.EyeCandyScriptInstance;
import com.lx862.mtrscripting.api.ScriptResultCall;
import com.lx862.mtrscripting.core.ParsedScript;
import com.lx862.mtrscripting.data.UniqueKey;
import com.lx862.mtrscripting.core.ScriptInstance;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.block.BlockEyeCandy;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.render.RenderEyeCandy;
import org.mtr.mod.render.StoredMatrixTransformations;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(value = RenderEyeCandy.class, remap = false)
public class RenderEyeCandyMixin {
    @Inject(method = "render(Lorg/mtr/mod/block/BlockEyeCandy$BlockEntity;FLorg/mtr/mapping/mapper/GraphicsHolder;II)V", at = @At("HEAD"), cancellable = true)
    public void renderScript(BlockEyeCandy.BlockEntity blockEntity, float tickDelta, GraphicsHolder graphicsHolder, int light, int overlay, CallbackInfo ci) {
        World world = blockEntity.getWorld2();
        if(world == null) return;

        ParsedScript script = MTRContentResourceManager.getEyecandyScript(blockEntity.getModelId());
        if(script == null) return;

        ScriptInstance<BlockEyeCandy.BlockEntity> scriptInstance = JCMClient.scriptManager.getInstanceManager().getInstance(new UniqueKey("mtr", "eyecandy", blockEntity.getModelId(), blockEntity.getPos2()), () -> new EyeCandyScriptInstance(new EyeCandyScriptContext(blockEntity), blockEntity, script));
        if(!(scriptInstance instanceof EyeCandyScriptInstance)) return;

        EyeCandyScriptInstance eyeCandyScriptInstance = (EyeCandyScriptInstance) scriptInstance;
        EyeCandyScriptContext eyeCandyScriptContext = (EyeCandyScriptContext)scriptInstance.getScriptContext();
        scriptInstance.setWrapperObject(blockEntity);

        scriptInstance.getScript().invokeRenderFunction(scriptInstance, () -> {
            eyeCandyScriptInstance.setDrawCalls(eyeCandyScriptContext.getDrawCalls());
            eyeCandyScriptInstance.setSoundCalls(eyeCandyScriptContext.getSoundCalls());
            eyeCandyScriptContext.reset();
        });

        BlockPos blockPos = blockEntity.getPos2();
        Direction facing = IBlock.getStatePropertySafe(blockEntity.getWorld2(), blockPos, BlockEyeCandy.FACING);

        StoredMatrixTransformations blockStoredMatrixTransformations = new StoredMatrixTransformations(0.5 + (double)blockEntity.getPos2().getX(), (double)blockEntity.getPos2().getY(), 0.5 + (double)blockEntity.getPos2().getZ());
        StoredMatrixTransformations storedMatrixTransformations = blockStoredMatrixTransformations.copy();

        storedMatrixTransformations.add((graphicsHolderNew) -> {
            graphicsHolderNew.translate(blockEntity.getTranslateX(), blockEntity.getTranslateY(), blockEntity.getTranslateZ());
            graphicsHolderNew.rotateYDegrees(180.0F - facing.asRotation());
            graphicsHolderNew.rotateXDegrees(blockEntity.getRotateX() + 180.0F);
            graphicsHolderNew.rotateYDegrees(blockEntity.getRotateY());
            graphicsHolderNew.rotateZDegrees(blockEntity.getRotateZ());
        });

        for(ScriptResultCall drawCall : new ArrayList<>(eyeCandyScriptInstance.drawCalls)) {
            if(drawCall == null) continue; // TODO: They should never be null
            drawCall.run(world, graphicsHolder, storedMatrixTransformations, facing, light);
        }
        for(ScriptResultCall soundCall : new ArrayList<>(eyeCandyScriptInstance.soundCalls)) {
            if(soundCall == null) continue; // TODO: They should never be null
            soundCall.run(world, graphicsHolder, blockStoredMatrixTransformations, facing, light);
        }
        ci.cancel();
    }
}
