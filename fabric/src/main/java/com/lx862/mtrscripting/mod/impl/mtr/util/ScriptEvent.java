package com.lx862.mtrscripting.mod.impl.mtr.util;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.util.StateTrackerJS;

public class ScriptEvent<T> {
    private final StateTrackerJS stateTracker = new StateTrackerJS();

    @ApiInternal
    public ScriptEvent() {
    }

    @ApiInternal
    public void trigger(T object) {
        stateTracker.setState(object);
    }

    public boolean occurred() {
        return stateTracker.stateNowFirst() && stateTracker.stateNow() != null;
    }

    public T detail() {
        return (T)stateTracker.stateNow();
    }

    @ApiInternal
    public void reset() {
        stateTracker.setState(null);
    }
}
