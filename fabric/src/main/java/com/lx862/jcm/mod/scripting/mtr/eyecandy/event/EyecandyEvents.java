package com.lx862.jcm.mod.scripting.mtr.eyecandy.event;

import com.lx862.jcm.mod.scripting.mtr.util.ScriptEvent;

public class EyecandyEvents {
    public final ScriptEvent<BlockUseEvent> onBlockUse = new ScriptEvent<>();

    public void handled() {
        synchronized (onBlockUse) {
            onBlockUse.reset();
        }
    }
}
