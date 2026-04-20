package com.lx862.mtrscripting.mod.impl.mtr.eyecandy.event;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.mod.impl.mtr.util.ScriptEvent;

public class EyecandyEvents {
    public final ScriptEvent<BlockUseEvent> onBlockUse = new ScriptEvent<>();

    @ApiInternal
    public EyecandyEvents() {
    }

    public void handled() {
        synchronized (onBlockUse) {
            onBlockUse.reset();
        }
    }
}
