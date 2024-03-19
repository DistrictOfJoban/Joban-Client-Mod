package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.registry.Items;
import org.mtr.mapping.holder.Item;
import org.mtr.mod.block.BlockPSDAPGGlassEndBase;

public class APGGlassEndDRL extends BlockPSDAPGGlassEndBase {
    @Override
    public Item asItem2() {
        return Items.APG_GLASS_END_DRL.get();
    }
}
