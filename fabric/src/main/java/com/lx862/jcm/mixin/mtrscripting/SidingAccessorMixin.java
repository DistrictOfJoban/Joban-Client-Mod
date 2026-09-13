package com.lx862.jcm.mixin.mtrscripting;

import org.mtr.core.data.Siding;
import org.mtr.core.data.Vehicle;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArraySet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Siding.class, remap = false)
public interface SidingAccessorMixin {
    @Accessor
    ObjectArraySet<Vehicle> getVehicles();
}
