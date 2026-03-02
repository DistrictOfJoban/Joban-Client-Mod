package com.lx862.mtrscripting.util;

import com.lx862.jcm.mod.JCMClient;
import com.lx862.mtrscripting.ScriptManager;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.Context;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.RhinoException;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2LongArrayMap;

/**
 * Provide the `console` object for scripts
 */
public class ConsoleJS {
    private static final Throwable DUMMY_EXCEPTION = new Throwable();
    private final Object2IntArrayMap<String> counters = new Object2IntArrayMap<>();
    private final Object2LongArrayMap<String> timers = new Object2LongArrayMap<>();

    public void time() {
        time("default");
    }

    public void time(String label) {
        if(timers.containsKey(label)) {
            warn("Timer \"" + label + "\" already exists.");
        } else {
            timers.put(label, TimingUtil.nanoTime());
        }
    }

    public void timeLog() {
        timeLog("default");
    }

    public void timeLog(String label) {
        long timeNow = TimingUtil.nanoTime();
        if(timers.containsKey(label)) {
            log(label + ": " + ((timeNow - timers.getLong(label)) / 1000 / 1000) + "ms");
        } else {
            warn("Timer \"" + label + "\" doesn't exist.");
        }
    }

    public void timeEnd() {
        timeEnd("default");
    }

    public void timeEnd(String label) {
        long timeNow = TimingUtil.nanoTime();
        if(timers.containsKey(label)) {
            log(label + ": " + ((timeNow - timers.getLong(label)) / 1000 / 1000) + "ms - timer ended");
            timers.removeLong(label);
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
            counters.removeInt(label);
            log(label + ": 0");
        } else {
            warn("Counter " + label + " doesn't exist.");
        }
    }

    public static void log(String... str) {
        String s = String.join(" ", str);
        ScriptManager.LOGGER.info("{} {}", buildLogPrefix(), s);
    }

    public static void warn(String... str) {
        String s = String.join(" ", str);
        ScriptManager.LOGGER.warn("{} {}", buildLogPrefix(), s);
    }

    public static void error(String... str) {
        String s = String.join(" ", str);
        ScriptManager.LOGGER.error("{} {}", buildLogPrefix(), s);
    }

    public static void debug(String... str) {
        if(JCMClient.getConfig().debug) {
            String s = String.join(" ", str);
            ScriptManager.LOGGER.error("{} {}", buildLogPrefix(), s);
        }
    }

    private static String buildLogPrefix() {
        StringBuilder sb = new StringBuilder("[JCM Scripting]");

        if(JCMClient.getConfig().showScriptLogSource) {
            String source = "";
            int lineNo = 0;

            try {
                throw Context.throwAsScriptRuntimeEx(DUMMY_EXCEPTION);
            } catch (Exception ex) {
                if(ex instanceof RhinoException e) {
                    source = e.sourceName();
                    lineNo = e.lineNumber();
                }
            }

            sb.append(" (").append(source).append("#").append(lineNo).append(")");
        }
        return sb.toString();
    }
}
