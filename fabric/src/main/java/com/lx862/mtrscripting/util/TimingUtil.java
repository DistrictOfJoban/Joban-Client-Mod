package com.lx862.mtrscripting.util;

import com.lx862.mtrscripting.core.ScriptInstance;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mod.InitClient;

/* From https://github.com/zbx1425/mtr-nte/blob/master/common/src/main/java/cn/zbx1425/mtrsteamloco/render/scripting/util/TimingUtil.java */

@SuppressWarnings("unused")
public class TimingUtil {
    private static double totalRunningTime = 0;
    private double timeElapsedForScript = 0;
    private double frameDeltaForScript = 0;

    public static void update(double elapsed) {
        totalRunningTime += MinecraftClient.getInstance().isPaused() ? 0 : elapsed / 1000.0;
    }

    public void prepareForScript(ScriptInstance<?> scriptInstance) {
        timeElapsedForScript = totalRunningTime;
        frameDeltaForScript = totalRunningTime - scriptInstance.lastExecuteTime;
        scriptInstance.lastExecuteTime = timeElapsedForScript;
    }

    public static double globalElapsed() {
        return totalRunningTime;
    }

    public double elapsed() {
        return timeElapsedForScript;
    }

    public double delta() {
        return frameDeltaForScript;
    }

    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public static long nanoTime() {
        return System.nanoTime();
    }
}