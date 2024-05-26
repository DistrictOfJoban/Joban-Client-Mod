package com.lx862.jcm.mod.data;

import org.mtr.mapping.holder.BooleanProperty;
import org.mtr.mapping.holder.DirectionProperty;
import org.mtr.mapping.holder.EnumProperty;
import org.mtr.mapping.holder.IntegerProperty;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mod.block.IBlock;

/**
 * Stores all block properties JCM uses. Block classes from JCM should reference the block properties in here
 */
public interface BlockProperties {
    public static final DirectionProperty FACING = DirectionHelper.FACING;
    public static final BooleanProperty HORIZONTAL_IS_LEFT = BooleanProperty.of("left");
    public static final EnumProperty<IBlock.DoubleBlockHalf> VERTICAL_2 = IBlock.HALF;
    public static final EnumProperty<IBlock.EnumThird> VERTICAL_PART_3 = IBlock.THIRD;
    public static final IntegerProperty LIGHT_LEVEL = IntegerProperty.of("level", 0, 15);
    public static final BooleanProperty POWERED = BooleanProperty.of("powered");
    public static final BooleanProperty IS_SLAB = BooleanProperty.of("is_slab");
    public static final BooleanProperty TOP = BooleanProperty.of("ceiling");
    public static final BooleanProperty EXIT_ON_LEFT = BooleanProperty.of("exit_on_left");
    public static final BooleanProperty POINT_TO_LEFT = BooleanProperty.of("right");
    public static final IntegerProperty BARRIER_FENCE_TYPE = IntegerProperty.of("type", 0, 10);
    public static final BooleanProperty BARRIER_FLIPPED = BooleanProperty.of("flipped");
}
