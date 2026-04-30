package com.lx862.jcm.mod;

import com.lx862.jcm.mod.config.JCMServerConfig;
import com.lx862.jcm.mod.registry.JCMRegistry;
import com.lx862.jcm.mod.util.BuildMetadata;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.mod.Keys;

import java.io.IOException;

public class JCM {
    public static final BuildMetadata buildMetadata;

    static {
        try {
            buildMetadata = BuildMetadata.of();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void initialize() {
        try {
            JCMLogger.info("Joban Client Mod v{} @ MTR {}", buildMetadata.fullVersion, Keys.class.getField("MOD_VERSION").get(null));
            if(buildMetadata.getReleaseType() == BuildMetadata.ReleaseType.BETA) {
                JCMLogger.info("This is a beta release, please expect and report any issues/bugs!");
            }
            if(buildMetadata.getReleaseType() == BuildMetadata.ReleaseType.NIGHTLY) {
                JCMLogger.info("This is a development/nightly build and not an official release, instabilities may occur!");
            }
        } catch (Exception e) {
            JCMLogger.warn("Cannot obtain MTR Version, countdown to disaster...");
        }
        JCMServerConfig.init();
        JCMRegistry.register();
    }
}