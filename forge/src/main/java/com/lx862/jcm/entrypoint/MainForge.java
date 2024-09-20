package com.lx862.jcm.entrypoint;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.JCM;
import com.lx862.jcm.mod.JCMClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class MainForge {
    public MainForge() {
        JCM.initialize();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> JCMClient::initializeClient);
        MinecraftForge.EVENT_BUS.register(new MigrateMapping());
    }
}
