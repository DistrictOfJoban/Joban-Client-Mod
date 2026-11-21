package com.lx862.jcm.mixin.mtrscripting;

import org.mtr.mod.block.BlockPSDAPGBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes MTR's protected {@link BlockPSDAPGBase#isAPG()} — the official
 * "this wall block belongs to a HALF-height gate" semantic (the same method
 * MTR's own {@code getOutlineShape2} uses to give APG blocks their short
 * upper half). Addons building on MTR's PSD classes override it — every
 * Tianjin Metro APG block does — so invoking it classifies MTR, JCM DRL and
 * third-party gates alike. See {@code PSDScriptingRules}.
 */
@Mixin(value = BlockPSDAPGBase.class, remap = false)
public interface BlockPSDAPGBaseAccessor {

    @Invoker("isAPG")
    boolean jcm$invokeIsAPG();
}
