package com.lx862.jcm.mod.scripting.mtr;

import com.lx862.jcm.mod.scripting.mtr.util.TextUtil;
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
    private static final ScriptManager scriptManager = ScriptingAPI.createScriptManager();

    /**
     * Called once when the mod entrypoint is invoked
     */
    public static void register() {
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
        });
    }

    public static ScriptManager getScriptManager() {
        return scriptManager;
    }

    public static void tick() {
        scriptManager.tick();
    }

    public static void reset() {
        scriptManager.reset();
    }
}
