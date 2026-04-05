package com.lx862.jcm.mod.scripting.mtr.eyecandy;

import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.mtrscripting.util.ScriptVector3f;
import org.mtr.mapping.holder.*;
import org.mtr.mod.block.BlockEyeCandy;
import org.mtr.mod.block.IBlock;

public class EyecandyBlockEntityWrapper {
    private final BlockEyeCandy.BlockEntity be;

    /* Note: Field access for backward compatibility only, you should use the getter functions. */
    public final String prefabId;
    public final float translateX;
    public final float translateY;
    public final float translateZ;
    public final float rotateX;
    public final float rotateY;
    public final float rotateZ;
    public final boolean fullLight;

    public EyecandyBlockEntityWrapper(BlockEyeCandy.BlockEntity be) {
        this.be = be;
        this.prefabId = be.getModelId();
        this.translateX = be.getTranslateX();
        this.translateY = be.getTranslateY();
        this.translateZ = be.getTranslateZ();
        this.rotateX = be.getRotateX();
        this.rotateY = be.getRotateY();
        this.rotateZ = be.getRotateZ();
        this.fullLight = be.getFullBrightness();
    }

    public String getModelId() {
        return this.prefabId;
    }

    public float getTranslateX() {
        return this.translateX;
    }

    public float getTranslateY() {
        return this.translateY;
    }

    public float getTranslateZ() {
        return this.translateZ;
    }

    public float getRotateX() {
        return this.rotateX;
    }

    public float getRotateY() {
        return this.rotateY;
    }

    public float getRotateZ() {
        return this.rotateZ;
    }

    public boolean getFullBrightness() {
        return this.fullLight;
    }

    public ScriptVector3f pos() {
        ScriptVector3f realPos = new ScriptVector3f(be.getPos2());
        realPos.add(getTranslateX(), getTranslateY(), getTranslateZ());
        return realPos;
    }

    public ScriptVector3f blockPos() {
        return new ScriptVector3f(be.getPos2());
    }

    public Direction facing() {
        return IBlock.getStatePropertySafe(be.getCachedState2(), BlockProperties.FACING);
    }

    public boolean isCrosshairTarget() {
        HitResult hitResult = MinecraftClient.getInstance().getCrosshairTargetMapped();

        if(BlockHitResult.isInstance(hitResult)) {
            return BlockHitResult.cast(hitResult).getBlockPos().equals(be.getPos2());
        }
        return false;
    }

    /**
     * Equivalent to {@link EyecandyBlockEntityWrapper#blockPos()} and {@link ScriptVector3f#rawBlockPos()}
     * For backward compatibility of scripts designed for Nemo's Transit Expansion (NTE)
     */
    @Deprecated
    public BlockPos getWorldPos() {
        return blockPos().rawBlockPos();
    }

    /**
     * Equivalent to {@link EyecandyBlockEntityWrapper#blockPos()}
     * For backward compatibility of scripts designed for Nemo's Transit Expansion (NTE)
     */
    @Deprecated
    public ScriptVector3f getWorldPosVector3f() {
        return blockPos();
    }

    /**
     * Equivalent to {@link EyecandyBlockEntityWrapper#pos()}
     * For backward compatibility of scripts designed for Aphrodite's Nemo's Transit Expansion (ANTE)
     */
    @Deprecated
    public ScriptVector3f getTransformPosVector3f() {
        return pos();
    }

    public int redstoneLevel() {
        World world = be.getWorld2();
        if(world == null) return 0;
        int highestRedstoneLevel = 0;

        for(Direction direction : Direction.values()) {
            highestRedstoneLevel = Math.max(highestRedstoneLevel, world.isEmittingRedstonePower(blockPos().rawBlockPos().offset(direction), direction.getOpposite()) ? 15 : 0);
        }
        return highestRedstoneLevel;
    }

    public World getWorld() {
        return be.getWorld2();
    }

    public boolean removed() {
        return be.isRemoved2();
    }
}
