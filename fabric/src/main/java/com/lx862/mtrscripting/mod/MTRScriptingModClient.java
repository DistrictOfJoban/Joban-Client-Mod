package com.lx862.mtrscripting.mod;

import com.lx862.mtrscripting.core.api.MTRScriptingAPI;
import com.lx862.mtrscripting.core.api.ScriptingLanguage;
import com.lx862.mtrscripting.core.rhinojs.MTRRhinoContextFactory;
import com.lx862.mtrscripting.core.rhinojs.RhinoJSLanguage;
import com.lx862.mtrscripting.mod.impl.mtr.MTRContentScripting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MTRScriptingModClient {
    public static final Logger LOGGER = LogManager.getLogger("MTR Scripting via JCM");

    public static void init() {
        MTRContentScripting.register();

        /* Rhino JS */
        MTRRhinoContextFactory.init();
        MTRScriptingAPI.addScriptingLanguage(ScriptingLanguage.RHINO_JS, new RhinoJSLanguage());
    }
}
