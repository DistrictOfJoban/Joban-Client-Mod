package com.lx862.mtrscripting.mod.impl.mtr;

import com.lx862.jcm.mod.config.JCMClientConfig;
import com.lx862.mtrscripting.core.api.AbstractScriptContext;
import com.lx862.mtrscripting.core.util.ScriptDataStore;

public abstract class MTRScriptContext extends AbstractScriptContext {

    public MTRScriptContext(String name) {
        super(name);
    }

    public boolean debugModeEnabled() {
        return JCMClientConfig.INSTANCE.scripting.scriptDebugMode.value();
    }

    /**
     * The persistent key-value store of this script — one store per script
     * id, shared by every instance of the script and persisted across game
     * restarts (see {@link ScriptDataStore}). JS scripts call
     * {@code ctx.getDataStore()}; C++ scripts call
     * {@code api.context().call("getDataStore")} and use its
     * {@code get/getString/getNumber/getBoolean/set/has/delete/keys/clear}
     * members. Values that must stay per-instance (per PIDS block, per PSD
     * panel, ...) can be keyed with the instance identity the wrapper
     * already exposes (e.g. {@code blockPos()}).
     *
     * @return the script's persistent data store
     */
    public ScriptDataStore getDataStore() {
        return ScriptDataStore.of(getName());
    }
}
