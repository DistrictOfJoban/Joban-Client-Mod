package com.lx862.jcm.mod.scripting;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.config.JCMClientConfig;
import com.lx862.mtrscripting.mod.gui.MTRScriptDebugOverlay;
import com.lx862.jcm.mod.scripting.pids.TextWrapper;
import com.lx862.jcm.mod.scripting.pids.TextureWrapper;
import com.lx862.mtrscripting.mod.impl.mtr.MTRContentScripting;
import com.lx862.mtrscripting.mod.impl.mtr.util.TextUtil;
import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.mtrscripting.core.ScriptManager;
import com.lx862.mtrscripting.core.api.ClassRule;
import com.lx862.mtrscripting.core.api.MTRScriptingAPI;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.NativeJavaClass;
import org.mtr.mod.client.MinecraftClientData;

public class JCMScripting {
    private static ScriptManager scriptManager;

    /**
     * Called once when the mod entrypoint is invoked
     */
    public static void register() {
        MTRScriptingAPI.registerAddonVersion("jcm", Constants.MOD_VERSION);
        MTRScriptDebugOverlay.registerDebugSource("JCM", scriptManager);

        scriptManager = new ScriptManager(JCMLogger.LOGGER, MTRContentScripting.getScriptExecutors());
        scriptManager.getClassShutter().setEnabled(!JCMClientConfig.INSTANCE.scripting.disableScriptRestrictions.value());
        scriptManager.getClassShutter().allowClass(ClassRule.parse("org.mtr.*"));
        scriptManager.getClassShutter().allowClass(ClassRule.parse("com.lx862.jcm.mod.scripting.pids.*"));
        scriptManager.getClassShutter().allowClass(ClassRule.parse("com.lx862.mtrscripting.mod.impl.mtr.*"));

        scriptManager.parseScriptEvent.register((contextName, context, scriptable) -> {
            scriptable.put("MTRClientData", scriptable, new NativeJavaClass(scriptable, MinecraftClientData.class));
            scriptable.put("TextUtil", scriptable, new NativeJavaClass(scriptable, TextUtil.class));

            if (contextName.equals("pids")) {
                scriptable.put("Text", scriptable, new NativeJavaClass(scriptable, TextWrapper.class));
                scriptable.put("Texture", scriptable, new NativeJavaClass(scriptable, TextureWrapper.class));
            }
        });
    }

    public static ScriptManager getScriptManager() {
        return scriptManager;
    }

    public static void tick() {
        int clearedInstance = scriptManager.getInstanceManager().clearDeadInstance();
        if(clearedInstance > 0 && JCMClientConfig.INSTANCE.debugMode.value()) {
            JCMLogger.info("Removed {} dead JCM script instance", clearedInstance);
        }
    }

    public static void reset() {
        scriptManager.reset();
    }
}
