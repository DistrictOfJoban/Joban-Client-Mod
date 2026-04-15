package com.lx862.mtrscripting.api;

import com.lx862.mtrscripting.util.render.ScriptRenderManager;

public interface ScriptResultCall {
    /**
     * Called when added by {@link ScriptRenderManager}.
     * @throws Exception If the current script call is not ready to be used.
     */
    void validate();
}
