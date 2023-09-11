package com.lx862.jcm.data;

import net.minecraft.state.property.Properties;
import org.mtr.mapping.holder.BooleanProperty;
import org.mtr.mapping.holder.DirectionProperty;
import org.mtr.mapping.holder.IntegerProperty;

public class BlockProperties {
    public static final DirectionProperty FACING = new DirectionProperty(Properties.HORIZONTAL_FACING);
    public static final IntegerProperty HORIZONTAL_PART = IntegerProperty.of("part", 0, 1);
    public static final IntegerProperty VERTICAL_PART_2 = IntegerProperty.of("part", 0, 1);
    public static final IntegerProperty VERTICAL_PART_3 = IntegerProperty.of("part", 0, 2);
    public static final BooleanProperty POWERED = BooleanProperty.of("powered");
    public static final BooleanProperty HAS_TOP = BooleanProperty.of("has_top");
    public static final BooleanProperty TOP = BooleanProperty.of("top");
}
