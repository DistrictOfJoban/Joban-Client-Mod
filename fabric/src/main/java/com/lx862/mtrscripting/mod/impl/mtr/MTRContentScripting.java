package com.lx862.mtrscripting.mod.impl.mtr;

import com.lx862.jcm.mod.config.JCMClientConfig;
import com.lx862.mtrscripting.mod.gui.MTRScriptDebugOverlay;
import com.lx862.mtrscripting.mod.MTRScriptingMod;
import com.lx862.mtrscripting.mod.impl.mtr.util.TextUtil;
import com.lx862.mtrscripting.core.ScriptManager;
import com.lx862.mtrscripting.core.api.ClassRule;
import com.lx862.mtrscripting.core.api.MTRScriptingAPI;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.NativeJavaClass;
import org.mtr.mod.Keys;
import org.mtr.mod.client.MinecraftClientData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A stub for scripting in MTR mod
 */
public class MTRContentScripting {
    private static final int EXECUTOR_AMOUNT = 4;
    private static final List<ExecutorService> scriptExecutors = new ArrayList<>();

    static {
        for(int i = 0; i < EXECUTOR_AMOUNT; i++) {
            scriptExecutors.add(Executors.newFixedThreadPool(1));
        }
    }

    private static ScriptManager scriptManager;

    /**
     * Called once when the mod entrypoint is invoked
     */
    public static void register() {
        scriptManager = new ScriptManager(MTRScriptingMod.LOGGER, scriptExecutors);
        scriptManager.getClassShutter().setEnabled(!JCMClientConfig.INSTANCE.scripting.disableScriptRestrictions.value());

        String mtrModVersion = null;
        try {
            mtrModVersion = (String) Keys.class.getField("MOD_VERSION").get(null);
        } catch (ReflectiveOperationException ignored) {
        }

        scriptManager.getClassShutter().allowClass(ClassRule.parse("org.mtr.*"));
        scriptManager.getClassShutter().allowClass(ClassRule.parse("com.lx862.mtrscripting.mod.impl.mtr.*"));

        scriptManager.parseScriptEvent.register((contextName, context, scriptable) -> {
            scriptable.put("MTRClientData", scriptable, new NativeJavaClass(scriptable, MinecraftClientData.class));
            scriptable.put("TextUtil", scriptable, new NativeJavaClass(scriptable, TextUtil.class));
        });

        MTRScriptingAPI.registerAddonVersion("mtr", mtrModVersion);
        MTRScriptDebugOverlay.registerDebugSource("MTR", scriptManager);
    }

    public static ScriptManager getScriptManager() {
        return scriptManager;
    }

    public static void tick() {
        int clearedInstance = scriptManager.getInstanceManager().clearDeadInstance();
        if(clearedInstance > 0 && JCMClientConfig.INSTANCE.debugMode.value()) {
            MTRScriptingMod.LOGGER.info("[MTR Scripting via JCM] Removed {} dead MTR script instance", clearedInstance);
        }
    }

    public static List<ExecutorService> getScriptExecutors() {
        return new ArrayList<>(scriptExecutors);
    }

    public static void reset() {
        scriptManager.reset();
    }
}
