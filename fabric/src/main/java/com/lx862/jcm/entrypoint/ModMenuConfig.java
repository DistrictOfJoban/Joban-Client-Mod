package com.lx862.jcm.entrypoint;

import com.lx862.jcm.mod.JCMClient;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import org.mtr.mapping.holder.Screen;

public class ModMenuConfig implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return modmenuScreen -> JCMClient.getClientConfigScreen(new Screen(modmenuScreen));
    }
}
