package com.lx862.mtrscripting.core.primitive;

import com.lx862.mtrscripting.core.ScriptManager;
import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.rhinojs.RhinoParsedScript;
import com.lx862.mtrscripting.core.util.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Represent a script which has been parsed/executed and ready for functions to be invoked.
 * For implementation details, please check {@link RhinoParsedScript }
 */
@ApiInternal
public abstract class ParsedScript {
    private static final int SCRIPT_RESET_TIME = 4000;

    protected final String displayName;
    protected final String contextName;
    protected final ScriptManager scriptManager;
    protected final TimingJS timing;
    protected final ExecutorService executorService;
    protected final List<ScriptContent> scriptContents;
    protected final double rateLimit;
    protected Exception capturedScriptException = null;
    protected long lastFailedTime = -1;

    public ParsedScript(ScriptManager scriptManager, String displayName, String contextName, Configuration scriptConfiguration) {
        this.displayName = displayName;
        this.contextName = contextName;
        this.scriptManager = scriptManager;
        this.scriptContents = scriptConfiguration.content();
        this.rateLimit = scriptConfiguration.rateLimit();
        this.timing = new TimingJS();
        this.executorService = scriptManager.getDesignatedScriptExecutor();
    }

    /**
     * @return Whether we are currently in the cooldown period after an errored script execution, and script shouldn't be executed.
     */
    public boolean duringFailCooldown() {
        return lastFailedTime != -1 && System.currentTimeMillis() - lastFailedTime <= SCRIPT_RESET_TIME;
    }

    /** Returns the exception occurred during the last failed script execution. Use in combination with {@link ParsedScript#duringFailCooldown()} to ensure the exception is still relevant. */
    public Exception getCapturedScriptException() {
        return capturedScriptException;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public abstract ParsedScript parse(boolean verboseLogging);
    public abstract void parseScriptContent(String str, String source);

    public abstract Future<?> invokeCreateFunctions(ScriptInstance<?> instance, Runnable finishCallback);
    public abstract void invokeRenderFunctions(ScriptInstance<?> instance, Runnable finishCallback);
    public abstract void invokeDisposeFunctions(ScriptInstance<?> instance, Runnable finishCallback);

    public abstract void exposeClassMethod(String name, Method method);
    public abstract void exposeClass(String name, Class<?> cl);
    public abstract void exposeClassInstance(String name, Object classInstance);

    public static class Configuration {
        private final List<ScriptContent> content;
        private final String scriptingLanguage;
        private final double rateLimit;

        private Configuration(List<ScriptContent> content, String scriptingLanguage, double rateLimit) {
            this.content = content;
            this.scriptingLanguage = scriptingLanguage;
            this.rateLimit = rateLimit;
        }

        public static Configuration create(List<ScriptContent> content, String scriptingLanguage, double rateLimit) {
            return new Configuration(content, scriptingLanguage, rateLimit);
        }

        public String scriptingLanguage() {
            return this.scriptingLanguage;
        }

        public double rateLimit() {
            return this.rateLimit;
        }

        public List<ScriptContent> content() {
            return this.content;
        }
    }
}
