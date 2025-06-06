package com.lx862.jcm.mod.util;

import com.lx862.jcm.mod.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Logger used by JCM that appends a logging prefix in {@value com.lx862.jcm.mod.Constants#LOGGING_PREFIX }
 */
public final class JCMLogger {
    public static final Logger LOGGER = LogManager.getLogger(Constants.MOD_NAME);

    public static void info(String s, Object... o) {
        LOGGER.info(Constants.LOGGING_PREFIX + s, o);
    }

    public static void warn(String s, Object... o) {
        LOGGER.warn(Constants.LOGGING_PREFIX + s, o);
    }

    public static void error(String s, Object... o) {
        LOGGER.error(Constants.LOGGING_PREFIX + s, o);
    }

    public static void error(String s, Throwable e) {
        LOGGER.error(Constants.LOGGING_PREFIX + s, e);
    }

    public static void fatal(String s, Object... o) {
        LOGGER.fatal(Constants.LOGGING_PREFIX + s, o);
    }

    public static void debug(String s, Object... o) {
        LOGGER.debug(Constants.LOGGING_PREFIX + "[DEBUG] " + s, o);
    }
}
