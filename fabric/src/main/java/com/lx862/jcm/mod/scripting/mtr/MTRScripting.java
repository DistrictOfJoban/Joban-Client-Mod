package com.lx862.jcm.mod.scripting.mtr;

import com.lx862.jcm.mod.JCMClient;
import com.lx862.jcm.mod.scripting.mtr.util.ModelManager;
import com.lx862.jcm.mod.scripting.mtr.util.TextUtil;
import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.mtrscripting.ScriptManager;
import com.lx862.mtrscripting.api.ClassRule;
import com.lx862.mtrscripting.api.ScriptingAPI;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.NativeJavaClass;
import org.mtr.mod.Keys;
import org.mtr.mod.client.MinecraftClientData;

/**
 * A stub for scripting in MTR mod
 */
public class MTRScripting {
    private static ScriptManager scriptManager;

    /**
     * Called once when the mod entrypoint is invoked
     */
    public static void register() {
        if(scriptManager == null) scriptManager = new ScriptManager();
        scriptManager.getClassShutter().setEnabled(!JCMClient.getConfig().disableScriptingRestriction);

        String mtrModVersion = null;
        try {
            mtrModVersion = (String) Keys.class.getField("MOD_VERSION").get(null);
        } catch (ReflectiveOperationException ignored) {
        }
        ScriptingAPI.registerAddonVersion("mtr", mtrModVersion);

        scriptManager.getClassShutter().allowClass(ClassRule.parse("org.mtr.*"));
        scriptManager.getClassShutter().allowClass(ClassRule.parse("com.lx862.jcm.mod.scripting.mtr.*"));

        scriptManager.onParseScript((contextName, context, scriptable) -> {
            scriptable.put("MTRClientData", scriptable, new NativeJavaClass(scriptable, MinecraftClientData.class));
            scriptable.put("TextUtil", scriptable, new NativeJavaClass(scriptable, TextUtil.class));
            scriptable.put("ModelManager", scriptable, new NativeJavaClass(scriptable, ModelManager.class));
        });
    }

    public static ScriptManager getScriptManager() {
        return scriptManager;
    }

    public static void tick() {
        int clearedInstance = scriptManager.getInstanceManager().clearDeadInstance();
        if(clearedInstance > 0 && JCMClient.getConfig().debug) {
            JCMLogger.info("Removed {} dead MTR script instance", clearedInstance);
        }
    }

    public static void reset() {
        scriptManager.reset();
    }
}
