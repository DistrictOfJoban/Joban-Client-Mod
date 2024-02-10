package com.lx862.jcm.mod.data;

import net.minecraft.state.property.Properties;
import org.mtr.mapping.holder.BooleanProperty;
import org.mtr.mapping.holder.DirectionProperty;
import org.mtr.mapping.holder.IntegerProperty;

/**
 * Stores all block properties JCM uses. Block classes from JCM should reference the block properties in here
 */
public class BlockProperties {
    public static final DirectionProperty FACING = new DirectionProperty(Properties.HORIZONTAL_FACING);
    public static final BooleanProperty HORIZONTAL_IS_LEFT = BooleanProperty.of("left");
    public static final IntegerProperty VERTICAL_PART_2 = IntegerProperty.of("part", 0, 1);
    public static final IntegerProperty VERTICAL_PART_3 = IntegerProperty.of("part", 0, 2);
    public static final BooleanProperty POWERED = BooleanProperty.of("powered");
    public static final BooleanProperty IS_SLAB = BooleanProperty.of("is_slab");
    public static final BooleanProperty TOP = BooleanProperty.of("top");
    public static final BooleanProperty EXIT_ON_LEFT = BooleanProperty.of("exit_on_left");
    public static final BooleanProperty LIT = BooleanProperty.of("lit");
    public static final BooleanProperty POINT_TO_LEFT = BooleanProperty.of("right");
    public static final IntegerProperty BARRIER_FENCE_TYPE = IntegerProperty.of("type", 0, 10);
    public static final BooleanProperty BARRIER_FLIPPED = BooleanProperty.of("flipped");
}
