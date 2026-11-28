package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.util.JCMLogger;
import org.lwjgl.glfw.GLFW;
import org.mtr.mapping.holder.KeyBinding;

public class KeyBinds {
    public static final KeyBinding SCRIPT_DEBUG_SOURCE_PREV = JCMRegistryClient.REGISTRY_CLIENT.registerKeyBinding("gui.jsblock.keybinds.script_debug_source_previous", GLFW.GLFW_KEY_LEFT_BRACKET, "category.jsblock.keybinding");
    public static final KeyBinding SCRIPT_DEBUG_SOURCE_NEXT = JCMRegistryClient.REGISTRY_CLIENT.registerKeyBinding("gui.jsblock.keybinds.script_debug_source_next", GLFW.GLFW_KEY_RIGHT_BRACKET, "category.jsblock.keybinding");
    public static final KeyBinding SCRIPT_DEBUG_RELOAD = JCMRegistryClient.REGISTRY_CLIENT.registerKeyBinding("gui.jsblock.keybinds.script_debug_reload", GLFW.GLFW_KEY_R, "category.jsblock.keybinding");
    /* ABI 16 era ecosystem feature: the in-game script browser (Modrinth
       search + one-click install into resourcepacks/). Unbound by default
       so it never collides with a user's existing binds. */
    public static final KeyBinding SCRIPT_BROWSER = JCMRegistryClient.REGISTRY_CLIENT.registerKeyBinding("gui.jsblock.keybinds.script_browser", GLFW.GLFW_KEY_UNKNOWN, "category.jsblock.keybinding");

    public static void registerClient() {
        JCMLogger.debug("Registering keybinds...");
    }
}
