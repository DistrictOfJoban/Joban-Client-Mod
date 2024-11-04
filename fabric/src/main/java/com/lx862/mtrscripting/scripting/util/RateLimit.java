package com.lx862.mtrscripting.scripting.util;

/* From https://github.com/zbx1425/mtr-nte/blob/master/common/src/main/java/cn/zbx1425/mtrsteamloco/render/scripting/util/RateLimit.java*/

public class RateLimit {

    private double lastTime = 0;
    private final double interval;

    public RateLimit(double interval) {
        this.interval = interval;
    }

    public boolean shouldUpdate() {
        double now = TimingUtil.elapsed();
        if (now - lastTime > interval) {
            lastTime = now;
            return true;
        }
        return false;
    }

    public void resetCoolDown() {
        lastTime = 0;
    }
}
