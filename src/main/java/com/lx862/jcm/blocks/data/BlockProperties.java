package com.lx862.jcm.blocks.data;

import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;

public class BlockProperties {
    public static final IntProperty HORIZONTAL_PART = IntProperty.of("part", 0, 1);
    public static final IntProperty VERTICAL_PART = IntProperty.of("part", 0, 2);
    public static final BooleanProperty POWERED = BooleanProperty.of("powered");
    public static final BooleanProperty HAS_TOP = BooleanProperty.of("has_top");
    public static final BooleanProperty HAS_BOTTOM = BooleanProperty.of("has_bottom");
}
