package com.lx862.mtrscripting.util;

/* From https://github.com/zbx1425/mtr-nte/blob/master/common/src/main/java/cn/zbx1425/mtrsteamloco/render/scripting/util/RateLimit.java*/

@SuppressWarnings("unused")
public class RateLimitJS {
    private double lastTime = 0;
    private final double interval;

    public RateLimitJS(double interval) {
        this.interval = interval;
    }

    public boolean shouldUpdate() {
        double now = TimingJS.globalElapsed();
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
