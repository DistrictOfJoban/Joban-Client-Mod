package com.lx862.jcm.mod.data.pids.scripting.util;

import org.mtr.core.data.Platform;
import org.mtr.mod.client.MinecraftClientData;

public class MTRUtil {
    public static Platform getPlatform(long id) {
        return MinecraftClientData.getInstance().platformIdMap.get(id);
    }
}
