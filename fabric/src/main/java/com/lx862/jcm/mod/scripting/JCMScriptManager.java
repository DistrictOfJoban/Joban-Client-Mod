package com.lx862.jcm.mod.scripting;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.scripting.pids.TextWrapper;
import com.lx862.jcm.mod.scripting.pids.TextureWrapper;
import com.lx862.jcm.mod.scripting.util.TextUtil;
import com.lx862.mtrscripting.api.ScriptingAPI;
import org.mtr.mod.Keys;
import org.mtr.mod.client.MinecraftClientData;
import vendor.com.lx862.jcm.org.mozilla.javascript.NativeJavaClass;

public class JCMScriptManager {
    public static void registerScripting() {
        String mtrModVersion = null;
        try {
            mtrModVersion = (String) Keys.class.getField("MOD_VERSION").get(null);
        } catch (ReflectiveOperationException ignored) {
        }
        ScriptingAPI.registerAddonVersion("mtr", mtrModVersion);
        ScriptingAPI.registerAddonVersion("jcm", Constants.MOD_VERSION);

        ScriptingAPI.allowClass("com.lx862.jcm.mod.scripting");
        ScriptingAPI.allowClass("org.mtr");
        ScriptingAPI.allowClass("java.awt");
        ScriptingAPI.allowClass("java.lang");
        ScriptingAPI.allowClass("sun.java2d");

        ScriptingAPI.onParseScript((contextName, context, scriptable) -> {
            // On behalf of MTR
            scriptable.put("MTRClientData", scriptable, new NativeJavaClass(scriptable, MinecraftClientData.class));
            scriptable.put("TextUtil", scriptable, new NativeJavaClass(scriptable, TextUtil.class));

            if (contextName.equals("PIDS")) {
                scriptable.put("Text", scriptable, new NativeJavaClass(scriptable, TextWrapper.class));
                scriptable.put("Texture", scriptable, new NativeJavaClass(scriptable, TextureWrapper.class));
            }
        });
    }
}
