package com.lx862.jcm.mod.scripting.mtr.eyecandy;

import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.mtrscripting.util.ScriptVector3f;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mod.block.BlockEyeCandy;
import org.mtr.mod.block.IBlock;

public class EyecandyBlockEntityWrapper {
    private final BlockEyeCandy.BlockEntity be;

    public EyecandyBlockEntityWrapper(BlockEyeCandy.BlockEntity be) {
        this.be = be;
    }

    public String getModelId() {
        return be.getModelId();
    }

    public float getTranslateX() {
        return be.getTranslateX();
    }

    public float getTranslateY() {
        return be.getTranslateY();
    }

    public float getTranslateZ() {
        return be.getTranslateZ();
    }

    public float getRotateX() {
        return be.getRotateX();
    }

    public float getRotateY() {
        return be.getRotateY();
    }

    public float getRotateZ() {
        return be.getRotateZ();
    }

    public boolean getFullBrightness() {
        return be.getFullBrightness();
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
