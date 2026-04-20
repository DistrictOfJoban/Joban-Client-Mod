package com.lx862.mtrscripting.core.api;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.util.render.ScriptRenderManager;

public interface ScriptResultCall {
    /**
     * Called when added by {@link ScriptRenderManager}.
     * @throws RuntimeException If the current script call is not ready to be used.
     */
    @ApiInternal
    void validate();
}
