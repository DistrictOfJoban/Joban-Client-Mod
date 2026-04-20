package com.lx862.mtrscripting.core.util;

import com.lx862.jcm.mod.config.JCMClientConfig;
import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.Context;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.RhinoException;
import com.lx862.mtrscripting.mod.MTRScriptingMod;

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

    public static void log(String... str) {
        String s = String.join(" ", str);
        MTRScriptingMod.LOGGER.info("{} {}", buildLogPrefix(), s);
    }

    public static void warn(String... str) {
        String s = String.join(" ", str);
        MTRScriptingMod.LOGGER.warn("{} {}", buildLogPrefix(), s);
    }

    public static void error(String... str) {
        String s = String.join(" ", str);
        MTRScriptingMod.LOGGER.error("{} {}", buildLogPrefix(), s);
    }

    public static void debug(String... str) {
        if(JCMClientConfig.INSTANCE.scripting.scriptDebugMode.value()) {
            String s = String.join(" ", str);
            MTRScriptingMod.LOGGER.error("{} {}", buildLogPrefix(), s);
        }
    }

    private static String buildLogPrefix() {
        StringBuilder sb = new StringBuilder("[JCM Scripting]");

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
