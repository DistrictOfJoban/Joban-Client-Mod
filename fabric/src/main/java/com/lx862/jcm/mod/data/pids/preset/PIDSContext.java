package com.lx862.jcm.mod.data.pids.preset;

import org.mtr.core.operation.ArrivalsResponse;
import org.mtr.mapping.holder.World;

public class PIDSContext {
    public final World world;
    public final ArrivalsResponse arrivals;

    public PIDSContext(World world, ArrivalsResponse arrivals) {
        this.world = world;
        this.arrivals = arrivals;
    }
}
