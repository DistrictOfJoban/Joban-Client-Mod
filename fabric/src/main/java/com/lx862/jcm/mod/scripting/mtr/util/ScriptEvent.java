package com.lx862.jcm.mod.scripting.mtr.util;

import com.lx862.mtrscripting.util.StateTracker;

public class ScriptEvent<T> {
    private final StateTracker stateTracker = new StateTracker();

    public void trigger(T object) {
        stateTracker.setState(object);
    }

    public boolean occurred() {
        return stateTracker.stateNowFirst() && stateTracker.stateNow() != null;
    }

    public T detail() {
        return (T)stateTracker.stateNow();
    }

    public void reset() {
        stateTracker.setState(null);
    }
}
