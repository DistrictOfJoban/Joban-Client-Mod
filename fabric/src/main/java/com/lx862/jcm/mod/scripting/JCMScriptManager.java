package com.lx862.jcm.mod.scripting;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.scripting.pids.TextWrapper;
import com.lx862.jcm.mod.scripting.pids.TextureWrapper;
import com.lx862.jcm.mod.scripting.util.TextUtil;
import com.lx862.mtrscripting.api.ClassRule;
import com.lx862.mtrscripting.api.ScriptingAPI;
import org.mtr.mod.Keys;
import org.mtr.mod.client.MinecraftClientData;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.NativeJavaClass;

public class JCMScriptManager {
    /**
     * Called once when the mod entrypoint is invoked
     */
    public static void initScripting() {
        ScriptingAPI.registerAddonVersion("jcm", Constants.MOD_VERSION);

        ScriptingAPI.addClassRule(ClassRule.parse("com.lx862.jcm.mod.scripting.*"));

        ScriptingAPI.onParseScript((contextName, context, scriptable) -> {
            if (contextName.equals("PIDS")) {
                scriptable.put("Text", scriptable, new NativeJavaClass(scriptable, TextWrapper.class));
                scriptable.put("Texture", scriptable, new NativeJavaClass(scriptable, TextureWrapper.class));
            }
        });
    }
}
