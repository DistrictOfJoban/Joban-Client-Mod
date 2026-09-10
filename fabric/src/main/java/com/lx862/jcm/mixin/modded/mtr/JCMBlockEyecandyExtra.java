package com.lx862.jcm.mixin.modded.mtr;

import java.util.Map;

public interface JCMBlockEyecandyExtra {
    Map<String, String> jsblock$getCustomConfig();

    void jsblock$updateCustomConfig(Map<String, String> newConfig);
}
