package com.lx862.jcm.mod.util;

import com.lx862.jcm.mod.Constants;
import org.slf4j.LoggerFactory;

public class Logger {
    public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Constants.MOD_ID);

    public static void info(String s) {
        LOGGER.info(Constants.LOGGING_PREFIX + s);
    }

    public static void info(String s, Object... o) {
        LOGGER.info(Constants.LOGGING_PREFIX + s, o);
    }

    public static void warn(String s) {
        LOGGER.warn(Constants.LOGGING_PREFIX + s);
    }

    public static void error(String s) {
        LOGGER.error(Constants.LOGGING_PREFIX + s);
    }

    public static void debug(String s) {
        LOGGER.debug(Constants.LOGGING_PREFIX + s);
    }
}
