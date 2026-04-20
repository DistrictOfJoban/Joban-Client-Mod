package com.lx862.mtrscripting.mod.impl.mtr;

import com.lx862.jcm.mod.config.JCMClientConfig;
import com.lx862.mtrscripting.core.api.AbstractScriptContext;

public abstract class MTRScriptContext extends AbstractScriptContext {

    public MTRScriptContext(String name) {
        super(name);
    }

    public boolean debugModeEnabled() {
        return JCMClientConfig.INSTANCE.scripting.scriptDebugMode.value();
    }
}
