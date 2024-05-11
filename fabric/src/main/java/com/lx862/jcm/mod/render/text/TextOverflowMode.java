package com.lx862.jcm.mod.render.text;

/**
 * Define the rendering behavior when a text is beyond the maximum allowed width
 */
public enum TextOverflowMode {
    /**
     * Force the text to fit within the boundary by stretching the text, disregarding the aspect ratio.<br>
     * See {@link TextOverflowMode#SCALE } to scale while retaining aspect ratio
     */
    STRETCH,
    /**
     * Force the text to fit within the boundary by resizing the text's size, keeping the aspect ratio.<br>
     */
    SCALE,
    /**
     * The text would not be fully displayed at once, but will continuously scroll horizontally to display the non-visible portion
     */
    MARQUEE
}
