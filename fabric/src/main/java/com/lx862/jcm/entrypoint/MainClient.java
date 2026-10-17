package com.lx862.jcm.entrypoint;

import com.lx862.jcm.mod.JCMClient;
import net.fabricmc.api.ClientModInitializer;

public class MainClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        JCMClient.initialize();
        /* Fabric-only: publishes the render-pass frustum used to cull native
           vehicle scripts (see FabricVehicleCulling / NativeCullFrustum). */
        FabricVehicleCulling.init();
    }
}