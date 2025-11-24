package com.lx862.mtrscripting.util;

/* From https://github.com/zbx1425/mtr-nte/blob/master/common/src/main/java/cn/zbx1425/mtrsteamloco/render/scripting/util/StateTracker.java */

import java.util.Objects;

@SuppressWarnings("unused")
public class StateTracker {
    private Object lastState;
    private Object currentState;
    private double currentStateTime;
    private boolean firstTimeCurrentState;

    public void setState(Object newValue) {
        if (!Objects.equals(newValue, currentState)) {
            lastState = currentState;
            currentState = newValue;
            currentStateTime = TimingUtil.globalElapsed();
            firstTimeCurrentState = true;
        } else {
            firstTimeCurrentState = false;
        }
    }

    public Object stateNow() {
        return currentState;
    }

    public Object stateLast() {
        return lastState;
    }

    public double stateNowDuration() {
        return TimingUtil.globalElapsed() - currentStateTime;
    }

    public boolean stateNowFirst() {
        return firstTimeCurrentState;
    }

    public boolean changedTo(Object state) {
        return stateNowFirst() && Objects.equals(currentState, state);
    }

    public boolean changedFromTo(Object oldState, Object curState) {
        return stateNowFirst() && Objects.equals(oldState, lastState) && Objects.equals(curState, currentState);
    }
}
