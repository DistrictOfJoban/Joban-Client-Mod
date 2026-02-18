package com.lx862.jcm.mod.scripting.mtr;

import com.lx862.jcm.mod.JCMClient;
import com.lx862.mtrscripting.core.ScriptContext;

public abstract class MTRScriptContext extends ScriptContext {

    public MTRScriptContext(String name) {
        super(name);
    }

    public boolean debugModeEnabled() {
        return JCMClient.getConfig().debug;
    }
}
