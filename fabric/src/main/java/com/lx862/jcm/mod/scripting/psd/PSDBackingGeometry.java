package com.lx862.jcm.mod.scripting.psd;

/** Coordinate conversions for the two faces of a scripted glass panel. */
final class PSDBackingGeometry {
    private PSDBackingGeometry() {
    }

    /** Converts the low block-axis edge to the platform canvas's left edge. */
    static float runLeft(int column, float low, float high, boolean mirrored) {
        return column + (mirrored ? 1 - high : low);
    }

    /** Converts the high block-axis edge to the platform canvas's right edge. */
    static float runRight(int column, float low, float high, boolean mirrored) {
        return column + (mirrored ? 1 - low : high);
    }

    /** Left edge after the track view's translate(width), rotateY(180) transform. */
    static float viewLeft(float runWidth, float left, float right, boolean trackSide) {
        return trackSide ? runWidth - right : left;
    }

    /** Classifies the actual camera against the pack-derived pane plane. */
    static boolean isTrackSide(double cameraX, double cameraZ, double anchorX, double anchorZ,
                               int normalX, int normalZ, float paneAlong) {
        final double along = (cameraX - anchorX - 0.5) * normalX
                + (cameraZ - anchorZ - 0.5) * normalZ;
        return along >= paneAlong;
    }
}
