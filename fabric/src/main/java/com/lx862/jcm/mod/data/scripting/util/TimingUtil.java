package com.lx862.jcm.mod.data.scripting.util;

import com.lx862.jcm.mod.data.scripting.base.ScriptInstance;
import org.mtr.mod.InitClient;

/* From https://github.com/zbx1425/mtr-nte/blob/master/common/src/main/java/cn/zbx1425/mtrsteamloco/render/scripting/util/TimingUtil.java */

@SuppressWarnings("unused")
public class TimingUtil {

    private static double timeElapsedForScript = 0;
    private static double frameDeltaForScript = 0;

    public static void prepareForScript(ScriptInstance scriptContext) {
        timeElapsedForScript = InitClient.getGameMillis() / 1000.0;
        frameDeltaForScript = timeElapsedForScript - scriptContext.lastExecuteTime;
        scriptContext.lastExecuteTime = timeElapsedForScript;
    }

    public static double elapsed() {
        return timeElapsedForScript;
    }

    public static double delta() {
        return frameDeltaForScript;
    }
}