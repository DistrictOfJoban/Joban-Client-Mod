package com.lx862.jcm.mod.scripting.jcm;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.JCMClient;
import com.lx862.jcm.mod.scripting.jcm.pids.TextWrapper;
import com.lx862.jcm.mod.scripting.jcm.pids.TextureWrapper;
import com.lx862.jcm.mod.scripting.mtr.util.TextUtil;
import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.mtrscripting.ScriptManager;
import com.lx862.mtrscripting.api.ClassRule;
import com.lx862.mtrscripting.api.ScriptingAPI;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.NativeJavaClass;
import org.mtr.mod.client.MinecraftClientData;

public class JCMScripting {
    private static final ScriptManager scriptManager = new ScriptManager();
    /**
     * Called once when the mod entrypoint is invoked
     */
    public static void register() {
        ScriptingAPI.registerAddonVersion("jcm", Constants.MOD_VERSION);

        scriptManager.getClassShutter().allowClass(ClassRule.parse("org.mtr.*"));
        scriptManager.getClassShutter().allowClass(ClassRule.parse("com.lx862.jcm.mod.scripting.jcm.*"));
        scriptManager.getClassShutter().allowClass(ClassRule.parse("com.lx862.jcm.mod.scripting.mtr.util.*"));

        scriptManager.onParseScript((contextName, context, scriptable) -> {
            scriptable.put("MTRClientData", scriptable, new NativeJavaClass(scriptable, MinecraftClientData.class));
            scriptable.put("TextUtil", scriptable, new NativeJavaClass(scriptable, TextUtil.class));

            if (contextName.equals("PIDS")) {
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
        if(clearedInstance > 0 && JCMClient.getConfig().debug) {
            JCMLogger.info("Removed {} dead PIDS script instance", clearedInstance);
        }
    }

    public static void reset() {
        scriptManager.reset();
    }
}
