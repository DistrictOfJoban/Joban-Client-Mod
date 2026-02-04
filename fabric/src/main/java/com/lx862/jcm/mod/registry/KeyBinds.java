package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.util.JCMLogger;
import org.lwjgl.glfw.GLFW;
import org.mtr.mapping.holder.KeyBinding;

public class KeyBinds {
    public static final KeyBinding SCRIPT_DEBUG_SOURCE_PREV = JCMRegistryClient.REGISTRY_CLIENT.registerKeyBinding("gui.jsblock.keybinds.script_debug_source_previous", GLFW.GLFW_KEY_LEFT_BRACKET, "category.jsblock.keybinding");
    public static final KeyBinding SCRIPT_DEBUG_SOURCE_NEXT = JCMRegistryClient.REGISTRY_CLIENT.registerKeyBinding("gui.jsblock.keybinds.script_debug_source_next", GLFW.GLFW_KEY_RIGHT_BRACKET, "category.jsblock.keybinding");
    public static final KeyBinding SCRIPT_DEBUG_RELOAD = JCMRegistryClient.REGISTRY_CLIENT.registerKeyBinding("gui.jsblock.keybinds.script_debug_reload", GLFW.GLFW_KEY_R, "category.jsblock.keybinding");

    public static void registerClient() {
        JCMLogger.debug("Registering keybinds...");
    }
}
