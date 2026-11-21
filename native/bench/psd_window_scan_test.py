"""Extract-and-run regression for PSDWindowBounds' occlusion scan (yanyang.13).

MTR's second glass style (psd_glass_2, 128px textures) paints a half-panel
design: a transparent window above a solid LED panel, with a left stile. Its
full-height pane quad is majority-opaque (70 of 128 sprite rows) yet carries a
real ~45% window. The "mostly-frame quad" whole-extent shortcut must stay
restricted to geometrically SMALL quads - otherwise it reads that majority as
"the whole quad is frame", erases the window, and the renderer falls back to
the FULL face (the script canvas then spans the LED panel and the stiles: the
2026-11-21 「脚本显示范围超出透明部分」 report).

This test extracts markOcclusion / markOcclusionH / vToY / uToH / sampleRect /
clampRow VERBATIM from the production PSDWindowBounds.java, compiles them
against a stub Quad/Sprite/NativeImage, and runs them on a synthetic
half-panel sprite:

  - the full-height pane quad: bottom ~45% (block y) must be occluded by the
    row scan, the top ~53% must stay a window, and the whole-extent shortcut
    must NOT fire (span 1.0 >= 0.5);
  - a small fully-opaque stile quad (span < 0.5): the whole-extent shortcut
    still fires, every row of its extent occluded (strips keep working);
  - a style-1-like small bottom frame (6/32 opaque rows): row scan only,
    unchanged window.
"""
import argparse
import os
from pathlib import Path
import re
import subprocess


HARNESS_HEAD = '''package com.lx862.jcm.mod.scripting.psd;

import java.util.*;

final class WindowScanHarness {
    private static final int GRID = 128;

    static final class SpriteContents implements SpriteContentsAccessor {
        final NativeImage image;
        SpriteContents(NativeImage image) { this.image = image; }
        public NativeImage jsblock$getImage() { return image; }
    }

    static final class Sprite {
        final float minU, maxU, minV, maxV;
        Sprite(float minU, float maxU, float minV, float maxV) { this.minU = minU; this.maxU = maxU; this.minV = minV; this.maxV = maxV; }
        SpriteContents getContents() { return new SpriteContents(image); }
        NativeImage image;
        float getMinU() { return minU; } float getMaxU() { return maxU; }
        float getMinV() { return minV; } float getMaxV() { return maxV; }
    }

    interface SpriteContentsAccessor { NativeImage jsblock$getImage(); }

    static final class NativeImage {
        final int w, h;
        final int[][] pixels;
        NativeImage(int w, int h, int[][] pixels) { this.w = w; this.h = h; this.pixels = pixels; }
        int getWidth() { return w; } int getHeight() { return h; }
        int getOpacity(int x, int y) { return pixels[y][x] & 0xFF; }
    }

    /** Mirrors the production Quad's members used by the extracted methods. */
    static final class Quad {
        float[] px = new float[4], py = new float[4], pz = new float[4];
        float[] pu = new float[4], pv = new float[4];
        float yMin, yMax, hMin, hMax;
        boolean hAlongX;
        int frameWidth, frameHeight;
        Sprite sprite;
    }

'''

HARNESS_TAIL = '''

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    /** The largest unoccluded [bottom, top) span, like scanWindow's core. */
    private static float[] largestWindow(boolean[] occluded) {
        int bestStart = -1, bestLen = 0, run = 0;
        for (int y = 0; y < GRID; y++) {
            if (!occluded[y]) {
                run++;
                if (run > bestLen) { bestLen = run; bestStart = y - run + 1; }
            } else {
                run = 0;
            }
        }
        if (bestLen <= 0 || bestLen / (float) GRID < 0.05F) return null;
        return new float[]{bestStart / (float) GRID, (bestStart + bestLen) / (float) GRID};
    }

    /** A full-height pane quad sampling the whole sprite (like psd_glass
        left/right variants): u 0..1 -> h 0..1, v 0..1 -> y 1..0. */
    private static Quad paneQuad(int frameWidth, int frameHeight) {
        final Quad quad = new Quad();
        quad.frameWidth = frameWidth;
        quad.frameHeight = frameHeight;
        quad.sprite = new Sprite(0, 1, 0, 1);
        quad.pu = new float[]{0, 1, 1, 0};
        quad.pv = new float[]{0, 0, 1, 1};
        quad.px = new float[]{0, 0, 0, 0};
        quad.pz = new float[]{0, 1, 1, 0};
        quad.py = new float[]{1, 1, 0, 0};
        quad.yMin = 0; quad.yMax = 1;
        quad.hMin = 0; quad.hMax = 1;
        return quad;
    }

    /** A small stile quad: y span 0.09, h span 0.5, fully opaque. */
    private static Quad stileQuad() {
        final Quad quad = paneQuad(16, 16);
        quad.py = new float[]{0.09F, 0.09F, 0, 0};
        quad.yMin = 0; quad.yMax = 0.09F;
        quad.pz = new float[]{0, 0.5F, 0.5F, 0};
        quad.hMin = 0; quad.hMax = 0.5F;
        return quad;
    }

    private static NativeImage halfPanelSprite() {
        // psd_glass_2's bottom texture: 58 fully transparent rows on top
        // (the window), 70 fully opaque rows below (the LED panel).
        final int[][] px = new int[128][128];
        for (int y = 58; y < 128; y++) {
            for (int x = 0; x < 128; x++) px[y][x] = 0xFF;
        }
        return new NativeImage(128, 128, px);
    }

    public static void main(String[] args) {
        // 1. Half-panel full-height pane: the row scan must keep the window.
        final Quad pane = paneQuad(128, 128);
        pane.sprite.image = halfPanelSprite();
        pane.sprite.getContents(); // contents accessor path unused here
        final boolean[] occluded = new boolean[GRID];
        markOcclusion(pane, occluded, 51);
        final float[] window = largestWindow(occluded);
        check(window != null, "half-panel pane lost its window entirely (full-face fallback -> canvas overflows)");
        check(window[0] > 0.4F && window[1] > 0.99F,
              "half-panel window should be the upper ~53%, got [" + window[0] + ", " + window[1] + "]");

        // 2. A small fully-opaque stile quad: the whole-extent shortcut must
        //    still fire (span 0.09 < 0.5), occluding its whole extent.
        final Quad stile = stileQuad();
        stile.sprite.image = opaqueSprite(16, 16);
        final boolean[] occludedStile = new boolean[GRID];
        markOcclusion(stile, occludedStile, 51);
        for (int r = 0; r < GRID; r++) {
            final boolean inExtent = r < (int) Math.ceil(0.09F * GRID);
            check(occludedStile[r] == inExtent,
                  "small opaque stile quad must occlude exactly its own extent (row " + r + ")");
        }

        // 3. Style-1-like small bottom frame (6 of 32 rows): row scan only.
        final Quad style1 = paneQuad(32, 32);
        style1.sprite.image = bottomFrameSprite(32, 32, 6);
        final boolean[] occluded1 = new boolean[GRID];
        markOcclusion(style1, occluded1, 51);
        final float[] window1 = largestWindow(occluded1);
        check(window1 != null && window1[0] > 0.1F && window1[0] < 0.25F,
              "style-1 small bottom frame: window should start just above the frame, got "
              + (window1 == null ? "none" : "[" + window1[0] + ", " + window1[1] + "]"));

        // 4. Horizontal twin on the half-panel pane: with the LED panel
        //    majority-opaque in every sampled column the columns all occlude,
        //    but the pane's own window rows still carry the vertical result.
        final boolean[] occludedH = new boolean[GRID];
        markOcclusionH(pane, occludedH, 51);
        // No assertion on a specific horizontal window here - the pane's
        // columns are majority-opaque so all occlude; the vertical window
        // (assertion 1) is what keeps the canvas off the LED panel.

        System.out.println("PSD window scan: half-panel keeps its window, small strips stay occluded, style-1 unchanged");
    }

    private static NativeImage opaqueSprite(int w, int h) {
        final int[][] px = new int[h][w];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) px[y][x] = 0xFF;
        return new NativeImage(w, h, px);
    }

    private static NativeImage bottomFrameSprite(int w, int h, int frameRows) {
        final int[][] px = new int[h][w];
        for (int y = h - frameRows; y < h; y++) for (int x = 0; x < w; x++) px[y][x] = 0xFF;
        return new NativeImage(w, h, px);
    }
}
'''


def extract_method(source, signature):
    start = source.index(signature)
    depth = 0
    for index in range(source.index('{', start), len(source)):
        if source[index] == '{':
            depth += 1
        elif source[index] == '}':
            depth -= 1
            if depth == 0:
                return source[start:index + 1]
    raise ValueError(f'Unterminated method: {signature}')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--build', required=True, type=Path)
    parser.add_argument('--java-home', type=Path)
    args = parser.parse_args()
    repo = Path(__file__).resolve().parents[2]
    source = (repo / 'fabric/src/main/java/com/lx862/jcm/mod/scripting/psd/PSDWindowBounds.java').read_text(encoding='utf8')

    methods = []
    for signature in (
            '    private static void markOcclusion(Quad quad, boolean[] occluded, int frameThreshold) {',
            '    private static void markOcclusionH(Quad quad, boolean[] occluded, int frameThreshold) {',
            '    private static float vToY(Quad quad, float v) {',
            '    private static float uToH(Quad quad, float u) {',
            '    private static int[][] sampleRect(Quad quad) {',
            '    private static int clampRow(float y) {',
            '    private static float clamp01(float v) {',
            '    private static float min(float[] arr) {',
            '    private static float max(float[] arr) {',
    ):
        methods.append(extract_method(source, signature))
    body = '\n\n'.join(methods)

    # The extracted methods reference quad.rowToV/columnToU (production Quad
    # members) - add delegating members on the stub Quad.
    quad_helpers = '''
    static final class QuadHelpers {
        static float rowToV(Quad quad, int row) {
            final float local = (row + 0.5F) / quad.frameHeight;
            return quad.sprite.getMinV() + local * (quad.sprite.getMaxV() - quad.sprite.getMinV());
        }
        static float columnToU(Quad quad, int col) {
            final float local = (col + 0.5F) / quad.frameWidth;
            return quad.sprite.getMinU() + local * (quad.sprite.getMaxU() - quad.sprite.getMinU());
        }
    }
'''
    # Rewrite quad.rowToV(...) -> QuadHelpers.rowToV(quad, ...) in the body.
    body = re.sub(r'quad\.rowToV\(', 'QuadHelpers.rowToV(quad, ', body)
    body = re.sub(r'quad\.columnToU\(', 'QuadHelpers.columnToU(quad, ', body)

    work = args.build.resolve() / 'psd-window-scan-test'
    work.mkdir(parents=True, exist_ok=True)
    harness = work / 'src' / 'com' / 'lx862' / 'jcm' / 'mod' / 'scripting' / 'psd' / 'WindowScanHarness.java'
    harness.parent.mkdir(parents=True, exist_ok=True)
    harness.write_text(HARNESS_HEAD + quad_helpers + body + HARNESS_TAIL, encoding='utf8')

    java_home = args.java_home or os.environ.get('JAVA_HOME')

    def tool(name):
        return str(Path(java_home) / 'bin' / (name + ('.exe' if os.name == 'nt' else ''))) if java_home else name

    classes = work / 'classes'
    classes.mkdir(exist_ok=True)
    subprocess.run([tool('javac'), '-J-Dfile.encoding=UTF-8', '--release', '17', '-encoding', 'UTF-8',
                    '-d', str(classes), str(harness)], check=True)
    subprocess.run([tool('java'), '-cp', str(classes), 'com.lx862.jcm.mod.scripting.psd.WindowScanHarness'], check=True)


if __name__ == '__main__':
    main()
