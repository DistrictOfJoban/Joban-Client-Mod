package com.lx862.mtrscripting.scripting.util;

/* From https://github.com/zbx1425/mtr-nte/blob/master/common/src/main/java/cn/zbx1425/mtrsteamloco/render/scripting/util/StateTracker.java */

@SuppressWarnings("unused")
public class StateTracker {

    private String lastState;
    private String currentState;
    private double currentStateTime;
    private boolean firstTimeCurrentState;

    public void setState(String value) {
        if (value != null && !value.equals(currentState)) {
            lastState = currentState;
            currentState = value;
            currentStateTime = TimingUtil.elapsed();
            firstTimeCurrentState = true;
        } else if (value != null) {
            firstTimeCurrentState = false;
        }
    }

    public String stateNow() {
        return currentState;
    }

    public String stateLast() {
        return lastState;
    }

    public double stateNowDuration() {
        return TimingUtil.elapsed() - currentStateTime;
    }

    public boolean stateNowFirst() {
        return firstTimeCurrentState;
    }
}
