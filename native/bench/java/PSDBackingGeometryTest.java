package com.lx862.jcm.mod.scripting.psd;

/** Runs against the production coordinate helper without a Minecraft client. */
public final class PSDBackingGeometryTest {
    private static void close(float expected, float actual, String label) {
        if (Math.abs(expected - actual) > 0.00001F) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    public static void main(String[] args) {
        // Minecraft horizontal facings, with clockwise walking direction.
        final int[][] normals = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        for (int[] normal : normals) {
            final int nx = normal[0], nz = normal[1];
            final int walkX = -nz, walkZ = nx;
            final boolean mirrored = (nx == 0 ? walkX : walkZ) < 0;
            for (int column = 0; column < 4; column++) {
                // Asymmetric edges detect accidental double mirroring and
                // confusing a right-edge position with a padding width.
                float left = PSDBackingGeometry.runLeft(column, 0.125F, 0.875F, mirrored);
                float right = PSDBackingGeometry.runRight(column, 0.125F, 0.875F, mirrored);
                close(0.75F, right - left, "span width");
                left = PSDBackingGeometry.runLeft(column, 0.0625F, 0.75F, mirrored);
                right = PSDBackingGeometry.runRight(column, 0.0625F, 0.75F, mirrored);
                close(column + (mirrored ? 0.25F : 0.0625F), left, "run left");
                close(column + (mirrored ? 0.9375F : 0.75F), right, "run right");
                final float backLeft = PSDBackingGeometry.viewLeft(4, left, right, true);
                // Matrix translate(4,0,0), rotateY(180) maps x -> 4-x;
                // the same physical rectangle must remain covered.
                close(right, 4 - backLeft, "track right world edge");
                close(left, 4 - (backLeft + right - left), "track left world edge");
                close(left, PSDBackingGeometry.viewLeft(4, left, right, false), "platform edge");
                close(column, PSDBackingGeometry.runLeft(column, 0, 1, mirrored), "full pane left");
                close(column + 1, PSDBackingGeometry.runRight(column, 0, 1, mirrored), "full pane right");
            }

            // A resource-pack pane need not be at the vanilla +0.25 depth.
            // Cover negative depths, a camera inside the block and far views.
            for (float plane : new float[]{-0.375F, 0.125F, 0.25F, 0.4375F}) {
                for (double offset : new double[]{-8, -0.01, 0, 0.01, 8}) {
                    double cameraX = 100.5 + (plane + offset) * nx;
                    double cameraZ = -39.5 + (plane + offset) * nz;
                    boolean actual = PSDBackingGeometry.isTrackSide(cameraX, cameraZ, 100, -40, nx, nz, plane);
                    if (actual != (offset >= 0)) throw new AssertionError("camera side at plane " + plane + ", offset " + offset);
                }
            }
        }
        System.out.println("PSD backing geometry: four facings, edge coverage, reversed track winding and pack-dependent camera planes passed");
    }
}
