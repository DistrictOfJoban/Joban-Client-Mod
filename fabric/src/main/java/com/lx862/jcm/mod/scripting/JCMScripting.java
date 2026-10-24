package com.lx862.jcm.mod.scripting;

import com.lx862.jcm.mod.JCM;
import com.lx862.jcm.mod.config.JCMClientConfig;
import com.lx862.jcm.mod.scripting.pids.RectangleWrapper;
import com.lx862.mtrscripting.mod.gui.hud.MTRScriptDebugOverlay;
import com.lx862.jcm.mod.scripting.pids.TextWrapper;
import com.lx862.jcm.mod.scripting.pids.TextureWrapper;
import com.lx862.mtrscripting.mod.impl.mtr.MTRContentScripting;
import com.lx862.mtrscripting.mod.impl.mtr.util.MTRWrapper;
import com.lx862.mtrscripting.mod.impl.mtr.util.TextUtilJS;
import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.mtrscripting.core.ScriptManager;
import com.lx862.mtrscripting.core.api.ClassRule;
import com.lx862.mtrscripting.core.api.MTRScriptingAPI;
import org.mtr.mod.client.MinecraftClientData;

public class JCMScripting {
    private static ScriptManager scriptManager;

    /**
     * Called once when the mod entrypoint is invoked
     */
    public static void register() {
        scriptManager = new ScriptManager(JCMLogger.LOGGER, MTRContentScripting.getScriptExecutors());
        scriptManager.getClassShutter().setEnabled(!JCMClientConfig.INSTANCE.scripting.disableScriptRestrictions.value());
        scriptManager.getClassShutter().allowClass(ClassRule.parse("org.mtr.*"));
        scriptManager.getClassShutter().allowClass(ClassRule.parse("com.lx862.jcm.mod.scripting.pids.*"));
        scriptManager.getClassShutter().allowClass(ClassRule.parse("com.lx862.mtrscripting.mod.impl.mtr.*"));

        scriptManager.parseScriptEvent.register((contextName, parsedScript) -> {
            parsedScript.exposeClass("MTRClientData", MinecraftClientData.class);
            parsedScript.exposeClass("MTR", MTRWrapper.class);
            parsedScript.exposeClass("TextUtil", TextUtilJS.class);

            if (contextName.equals("pids")) {
                parsedScript.exposeClass("Text", TextWrapper.class);
                parsedScript.exposeClass("Texture", TextureWrapper.class);
                parsedScript.exposeClass("Rectangle", RectangleWrapper.class);
            }
        });

        MTRScriptingAPI.registerAddonVersion("jcm", JCM.buildMetadata.version);
        MTRScriptDebugOverlay.registerDebugSource("JCM", scriptManager);
    }

    public static ScriptManager getScriptManager() {
        return scriptManager;
    }

    public static void tick() {
        int clearedInstance = scriptManager.getInstanceManager().tick();
        if(clearedInstance > 0 && JCMClientConfig.INSTANCE.debugMode.value()) {
            JCMLogger.info("Removed {} dead JCM script instance", clearedInstance);
        }
    }

    public static void reset() {
        scriptManager.reset();
    }
}
