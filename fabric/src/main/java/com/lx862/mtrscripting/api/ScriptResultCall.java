package com.lx862.mtrscripting.api;

public interface ScriptResultCall {
    /**
     * Called when added by {@link com.lx862.jcm.mod.scripting.mtr.render.ScriptRenderManager}.
     * @throws Exception If the current script call is not ready to be used.
     */
    void validate();
}
