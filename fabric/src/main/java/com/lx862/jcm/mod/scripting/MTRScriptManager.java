package com.lx862.jcm.mod.scripting;

import com.lx862.jcm.mod.scripting.util.TextUtil;
import com.lx862.mtrscripting.api.ClassRule;
import com.lx862.mtrscripting.api.ScriptingAPI;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.NativeJavaClass;
import org.mtr.mod.Keys;
import org.mtr.mod.client.MinecraftClientData;

/**
 * A stub for scripting in MTR mod
 */
public class MTRScriptManager {
    /**
     * Called once when the mod entrypoint is invoked
     */
    public static void initScripting() {
        String mtrModVersion = null;
        try {
            mtrModVersion = (String) Keys.class.getField("MOD_VERSION").get(null);
        } catch (ReflectiveOperationException ignored) {
        }
        ScriptingAPI.registerAddonVersion("mtr", mtrModVersion);

        ScriptingAPI.addClassRule(ClassRule.parse("org.mtr.*"));

        ScriptingAPI.onParseScript((contextName, context, scriptable) -> {
            scriptable.put("MTRClientData", scriptable, new NativeJavaClass(scriptable, MinecraftClientData.class));
            scriptable.put("TextUtil", scriptable, new NativeJavaClass(scriptable, TextUtil.class));
        });
    }
}
