package com.lx862.jcm.mod.render.fundamental;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.mtr.mapping.holder.MinecraftClient;

/**
 * Keep track of applied OpenGL Scissors, this should be the primary way to apply clipping to the GUI.<br>
 * Use {@link ClipStack#push(int, int, int, int)} to apply a new clip, and {@link ClipStack#pop()} after finish clipping.
 */
public class ClipStack {
    private static final ObjectList<Rectangle> stacks = new ObjectArrayList<>();

    /**
     * Push and apply a new clipping area.<br>
     * GUI scaling are already taken account for, there's no need to multiply the scale by yourself for the parameters.
     */
    public static void push(int x, int y, int width, int height) {
        stacks.add(new Rectangle(x, y, width, height));
        enableClip(x, y, width, height);
    }

    public static void pop() {
        if(stacks.isEmpty()) {
            throw new IllegalStateException("No more clip stack to be popped!");
        }

        if(stacks.size() > 1) {
            stacks.remove(stacks.size() - 1);
            Rectangle remainingLast = stacks.get(stacks.size() - 1);
            enableClip(remainingLast.x, remainingLast.y, remainingLast.width, remainingLast.height);
        } else {
            stacks.remove(0);
            disableClip();
        }
    }

    /**
     * Enable clipping to only render content within boundary
     * @param x Start X of your rectangle
     * @param y Start Y of your rectangle
     * @param width Width of your rectangle
     * @param height Height of your rectangle
     */
    private static void enableClip(int x, int y, int width, int height) {
        double scale = MinecraftClient.getInstance().getWindow().getScaleFactor();
        int windowHeight = MinecraftClient.getInstance().getWindow().getHeight();
        RenderSystem.enableScissor((int)(x * scale), (int)(windowHeight - (y + height) * scale), (int)(width * scale), (int)(height * scale));
    }

    /**
     * Ensure the clip is applied appropriately, yes this is a hack.
     */
    public static void ensureStateCorrect() {
        if(stacks.isEmpty()) {
            disableClip();
        } else {
            Rectangle latestStacks = stacks.get(stacks.size()-1);
            enableClip(latestStacks.x, latestStacks.y, latestStacks.width, latestStacks.height);
        }
    }

    private static void disableClip() {
        RenderSystem.disableScissor();
    }
}
