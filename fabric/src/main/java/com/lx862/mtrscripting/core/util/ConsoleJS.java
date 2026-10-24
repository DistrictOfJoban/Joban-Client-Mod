package com.lx862.mtrscripting.core.util;

import com.lx862.jcm.mod.config.JCMClientConfig;
import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.Context;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.NativeJSON;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.NativeObject;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.RhinoException;
import com.lx862.mtrscripting.mod.MTRScriptingModClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Provide the `console` object for scripts
 */
public class ConsoleJS {
    private static final Throwable DUMMY_EXCEPTION = new Throwable();
    private final Map<String, Integer> counters = new HashMap<>();
    private final Map<String, Long> timers = new HashMap<>();

    @ApiInternal
    public ConsoleJS() {
    }

    public void time() {
        time("default");
    }

    public void time(String label) {
        if(timers.containsKey(label)) {
            warn("Timer \"" + label + "\" already exists.");
        } else {
            timers.put(label, TimingJS.nanoTime());
        }
    }

    public void timeLog() {
        timeLog("default");
    }

    public void timeLog(String label) {
        long timeNow = TimingJS.nanoTime();
        if(timers.containsKey(label)) {
            log(label + ": " + ((timeNow - timers.get(label)) / 1000 / 1000) + "ms");
        } else {
            warn("Timer \"" + label + "\" doesn't exist.");
        }
    }

    public void timeEnd() {
        timeEnd("default");
    }

    public void timeEnd(String label) {
        long timeNow = TimingJS.nanoTime();
        if(timers.containsKey(label)) {
            log(label + ": " + ((timeNow - timers.get(label)) / 1000 / 1000) + "ms - timer ended");
            timers.remove(label);
        } else {
            warn("Timer \"" + label + "\" doesn't exist.");
        }
    }

    public void count() {
        count("default");
    }

    public void count(String label) {
        int newVal = counters.getOrDefault(label, 0)+1;
        counters.put(label, newVal);
        log(label + ": " + newVal);
    }

    public void countReset() {
        countReset("default");
    }

    public void countReset(String label) {
        if(counters.containsKey(label)) {
            counters.remove(label);
            log(label + ": 0");
        } else {
            warn("Counter " + label + " doesn't exist.");
        }
    }

    public static void log(Object... str) {
        MTRScriptingModClient.LOGGER.info("{} {}", buildLogPrefix(), buildArgument(str));
    }

    public static void warn(Object... str) {
        MTRScriptingModClient.LOGGER.warn("{} {}", buildLogPrefix(), buildArgument(str));
    }

    public static void error(Object... str) {
        MTRScriptingModClient.LOGGER.error("{} {}", buildLogPrefix(), buildArgument(str));
    }

    public static void debug(Object... str) {
        if(JCMClientConfig.INSTANCE.scripting.scriptDebugMode.value()) {
            MTRScriptingModClient.LOGGER.error("{} {}", buildLogPrefix(), buildArgument(str));
        }
    }

    @ApiInternal
    private static String buildArgument(Object... objs) {
        StringBuilder sb = new StringBuilder();
        if(objs == null) {
            sb.append("null");
        } else {
            for(Object obj : objs) {
                if(obj instanceof NativeObject) {
                    sb.append(NativeJSON.stringify(Context.getCurrentContext(), (NativeObject)obj, obj, null, " "));
                } else {
                    sb.append(obj);
                }
            }
        }
        return sb.toString();
    }

    @ApiInternal
    private static String buildLogPrefix() {
        StringBuilder sb = new StringBuilder("[MTR Scripting via JCM]");

        if(JCMClientConfig.INSTANCE.scripting.showLogSource.value()) {
            String source;
            int lineNo;

            try {
                throw Context.throwAsScriptRuntimeEx(DUMMY_EXCEPTION);
            } catch (RhinoException ex) {
                source = ex.sourceName();
                lineNo = ex.lineNumber();
            }

            if(source != null) { // Not executing script, likely invoked by JCM itself, no source to append.
                sb.append(" (").append(source).append("#").append(lineNo).append(")");
            }
        }
        return sb.toString();
    }
}
