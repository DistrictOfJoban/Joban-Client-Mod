package com.lx862.jcm.mixin.modded.tsc;

import org.mtr.core.Main;
import org.mtr.core.simulation.Simulator;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Main.class, remap = false)
public interface MainAccessorMixin {
    @Accessor("simulators")
    ObjectImmutableList<Simulator> getSimulators();
}
