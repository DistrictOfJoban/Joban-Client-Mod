package com.lx862.jcm.mod.util;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.config.ClientConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JCMLogger {
    public static final Logger LOGGER = LogManager.getLogger(Constants.MOD_ID);

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
        if(ClientConfig.DEBUG_MODE.get()) {
            LOGGER.info(Constants.LOGGING_PREFIX + "[DEBUG] " + s);
        } else {
            LOGGER.debug(Constants.LOGGING_PREFIX + "[DEBUG] " + s);
        }
    }

    public static void debug(String s, Object... o) {
        LOGGER.debug(Constants.LOGGING_PREFIX + s, o);
    }
}
