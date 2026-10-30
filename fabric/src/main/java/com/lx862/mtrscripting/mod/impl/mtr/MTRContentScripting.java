package com.lx862.mtrscripting.mod.impl.mtr;

import com.lx862.jcm.mod.config.JCMClientConfig;
import com.lx862.mtrscripting.mod.gui.hud.MTRScriptDebugOverlay;
import com.lx862.mtrscripting.mod.MTRScriptingModClient;
import com.lx862.mtrscripting.mod.impl.mtr.util.ante.ANTEComponentUtilJS;
import com.lx862.mtrscripting.mod.impl.mtr.util.ante.ANTEGlobalRegisterJS;
import com.lx862.mtrscripting.mod.impl.mtr.util.ante.ANTESoundHelperJS;
import com.lx862.mtrscripting.mod.impl.mtr.util.ante.ANTETickableSoundJS;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.config.ante.ConfigResponder;
import com.lx862.mtrscripting.mod.impl.mtr.util.MTRWrapperJS;
import com.lx862.mtrscripting.mod.impl.mtr.util.TextUtilJS;
import com.lx862.mtrscripting.core.ScriptManager;
import com.lx862.mtrscripting.core.api.ClassRule;
import com.lx862.mtrscripting.core.api.MTRScriptingAPI;
import org.mtr.mod.Keys;
import org.mtr.mod.client.MinecraftClientData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MTRContentScripting {
    private static final int EXECUTOR_AMOUNT = 4;
    private static final List<ExecutorService> scriptExecutors = new ArrayList<>();
    private static ScriptManager scriptManager;

    /**
     * Called once when the mod entrypoint is invoked
     */
    public static void register() {
        initExecutors(EXECUTOR_AMOUNT);
        scriptManager = new ScriptManager(MTRScriptingModClient.LOGGER, scriptExecutors);
        scriptManager.getClassShutter().setEnabled(!JCMClientConfig.INSTANCE.scripting.disableScriptRestrictions.value());

        String mtrModVersion = null;
        try {
            mtrModVersion = (String) Keys.class.getField("MOD_VERSION").get(null);
        } catch (ReflectiveOperationException ignored) {
        }

        scriptManager.getClassShutter().allowClass(ClassRule.parse("org.mtr.*"));
        scriptManager.getClassShutter().allowClass(ClassRule.parse("top.mcmtr.core.*"));
        scriptManager.getClassShutter().allowClass(ClassRule.parse("top.mcmtr.mod.*"));
        scriptManager.getClassShutter().allowClass(ClassRule.parse("com.lx862.mtrscripting.mod.impl.mtr.*"));

        scriptManager.parseScriptEvent.register((contextName, parsedScript) -> {
            parsedScript.exposeClass("MTRClientData", MinecraftClientData.class);
            parsedScript.exposeClass("MTR", MTRWrapperJS.class);
            parsedScript.exposeClass("TextUtil", TextUtilJS.class);

            // ANTE Eyecandy Custom Config
            if(contextName.equals("Block")) {
                parsedScript.exposeClass("ConfigResponder", ConfigResponder.class);
                parsedScript.exposeClass("Optional", Optional.class);
            }

            // ANTE Utils
            parsedScript.exposeClass("SoundHelper", ANTESoundHelperJS.class);
            parsedScript.exposeClass("TickableSound", ANTETickableSoundJS.class);
            parsedScript.exposeClass("GlobalRegister", ANTEGlobalRegisterJS.class);
            parsedScript.exposeClass("ComponentUtil", ANTEComponentUtilJS.class);
        });

        MTRScriptingAPI.registerAddonVersion("mtr", mtrModVersion);
        MTRScriptDebugOverlay.registerDebugSource("MTR", scriptManager);
    }

    private static void initExecutors(int threadCount) {
        for(int i = 0; i < threadCount; i++) {
            scriptExecutors.add(Executors.newFixedThreadPool(1, ScriptManager.getNamedThreadFactory("JCM-Scripting-Executor-%d")));
        }
    }

    public static ScriptManager getScriptManager() {
        return scriptManager;
    }

    public static void tick() {
        int clearedInstance = scriptManager.getInstanceManager().tick();
        if(clearedInstance > 0 && JCMClientConfig.INSTANCE.debugMode.value()) {
            MTRScriptingModClient.LOGGER.info("[MTR Scripting via JCM] Removed {} dead MTR script instance", clearedInstance);
        }
    }

    public static List<ExecutorService> getScriptExecutors() {
        return new ArrayList<>(scriptExecutors);
    }

    public static void reset() {
        scriptManager.reset();
    }
}
