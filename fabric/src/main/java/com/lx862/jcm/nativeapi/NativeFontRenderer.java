package com.lx862.jcm.nativeapi;

import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.mtrscripting.core.util.ScriptResourceUtil;
import org.mtr.mapping.holder.Identifier;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Resource-pack fonts for native canvases, with the same baseline and metrics
 * as JavaScript Graphics2D. Cached masks retain bearings, advance and descenders;
 * no individual glyph is resized to fill a cell. */
final class NativeFontRenderer {
    private static final long MASK_BUDGET = 32L * 1024 * 1024;
    private final Map<String, Font> baseFonts = new HashMap<>();
    private final Map<String, Font> fonts = new HashMap<>();
    private final Map<String, double[]> metrics = new HashMap<>();
    private final LinkedHashMap<String, Mask> masks = new LinkedHashMap<>(64, 0.75f, true);
    private long maskBytes;

    private static final class Mask {
        final int x, y, width, height;
        final int[] pixels;
        Mask(int x, int y, BufferedImage image) {
            this.x = x;
            this.y = y;
            width = image.getWidth();
            height = image.getHeight();
            pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        }
        long bytes() { return (long) pixels.length * 4; }
    }

    private Font font(String path, double size) {
        Font base = baseFonts.get(path);
        if (base == null) {
            base = ScriptResourceUtil.readFont(new Identifier(path));
            if (base == null) {
                JCMLogger.warn("Native font {} is unavailable; using bundled Noto Sans.", path);
                base = ScriptResourceUtil.getSystemFont("Noto Sans");
            }
            if (base == null) return null;
            if (baseFonts.size() >= 64) baseFonts.clear();
            baseFonts.put(path, base);
        }
        final String key = path + "/" + size;
        Font derived = fonts.get(key);
        if (derived == null) {
            derived = base.deriveFont(Font.PLAIN, (float) size);
            if (fonts.size() >= 256) fonts.clear();
            fonts.put(key, derived);
        }
        return derived;
    }

    private static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    synchronized double[] measure(byte[] resource, byte[] utf8, double size, double sx, double sy) {
        if (resource == null || utf8 == null || !valid(size, sx, sy)) return null;
        final String path = new String(resource, StandardCharsets.UTF_8);
        final String text = new String(utf8, StandardCharsets.UTF_8);
        final String key = path + "/" + size + "/" + sx + "/" + sy + "/" + text;
        double[] result = metrics.get(key);
        if (result != null) return result;
        final Font font = font(path, size);
        if (font == null) return null;
        final Graphics2D g = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        try {
            hints(g);
            g.scale(sx, sy);
            final FontMetrics fm = g.getFontMetrics(font);
            result = new double[]{fm.stringWidth(text), fm.getAscent(), fm.getDescent(), fm.getHeight()};
        } finally {
            g.dispose();
        }
        if (metrics.size() >= 4096) metrics.clear();
        metrics.put(key, result);
        return result;
    }

    synchronized int[] draw(byte[] resource, byte[] utf8, double size, double x, double baseline,
                            double sx, double sy, double compression, int color,
                            int clipX0, int clipY0, int clipX1, int clipY1,
                            ByteBuffer target, int width, int height) {
        if (resource == null || utf8 == null || target == null || !valid(size, sx, sy)
                || !Double.isFinite(x) || !Double.isFinite(baseline)
                || !Double.isFinite(compression) || compression <= 0 || width <= 0 || height <= 0
                || (long) width * height * 4 > target.capacity()) return null;
        if (utf8.length == 0 || (color >>> 24) == 0) return new int[]{0, 0, 0, 0};
        final String path = new String(resource, StandardCharsets.UTF_8);
        final String text = new String(utf8, StandardCharsets.UTF_8);
        final Font font = font(path, size);
        if (font == null) return null;
        final double dx = x * sx, dy = baseline * sy, textSx = sx * compression;
        final int ix = (int) Math.floor(dx), iy = (int) Math.floor(dy);
        final double phaseX = dx - ix, phaseY = dy - iy;
        // Integer translations share a mask; fractional translations affect
        // anti-aliasing and must remain part of its cache key.
        final String key = path + "/" + size + "/" + textSx + "/" + sy
                + "/" + phaseX + "/" + phaseY + "/" + text;
        Mask mask = masks.get(key);
        if (mask == null) {
            final AffineTransform transform = new AffineTransform(textSx, 0, 0, sy, phaseX, phaseY);
            final Graphics2D probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
            final Rectangle bounds;
            try {
                hints(probe);
                probe.setTransform(transform);
                final TextLayout layout = new TextLayout(text, font, probe.getFontRenderContext());
                bounds = transform.createTransformedShape(layout.getBounds()).getBounds();
                bounds.grow(3, 3);
            } finally {
                probe.dispose();
            }
            final long count = (long) bounds.width * bounds.height;
            if (bounds.width <= 0 || bounds.height <= 0 || count > MASK_BUDGET / 4) return null;
            final BufferedImage image = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
            final Graphics2D g = image.createGraphics();
            try {
                hints(g);
                g.translate(-bounds.x, -bounds.y);
                g.transform(transform);
                g.setFont(font);
                g.setColor(Color.WHITE);
                g.drawString(text, 0f, 0f);
            } finally {
                g.dispose();
            }
            mask = new Mask(bounds.x, bounds.y, image);
            while (!masks.isEmpty() && (masks.size() >= 4096 || maskBytes + mask.bytes() > MASK_BUDGET)) {
                final Map.Entry<String, Mask> first = masks.entrySet().iterator().next();
                maskBytes -= first.getValue().bytes();
                masks.remove(first.getKey());
            }
            masks.put(key, mask);
            maskBytes += mask.bytes();
        }
        final int originX = ix + mask.x, originY = iy + mask.y;
        final int x0 = Math.max(Math.max(0, clipX0), originX);
        final int y0 = Math.max(Math.max(0, clipY0), originY);
        final int x1 = Math.min(Math.min(width, clipX1), originX + mask.width);
        final int y1 = Math.min(Math.min(height, clipY1), originY + mask.height);
        if (x1 <= x0 || y1 <= y0) return new int[]{0, 0, 0, 0};
        target.order(ByteOrder.nativeOrder());
        for (int py = y0; py < y1; py++) {
            for (int px = x0; px < x1; px++) {
                final int coverage = mask.pixels[(py - originY) * mask.width + px - originX] >>> 24;
                final int alpha = (coverage * (color >>> 24) + 127) / 255;
                if (alpha == 0) continue;
                final int offset = (py * width + px) * 4;
                final int dst = target.getInt(offset);
                final int remaining = ((dst >>> 24) * (255 - alpha) + 127) / 255;
                final int outAlpha = alpha + remaining;
                final int r = blend(color >>> 16 & 255, dst >>> 16 & 255, alpha, remaining, outAlpha);
                final int g = blend(color >>> 8 & 255, dst >>> 8 & 255, alpha, remaining, outAlpha);
                final int b = blend(color & 255, dst & 255, alpha, remaining, outAlpha);
                target.putInt(offset, outAlpha << 24 | r << 16 | g << 8 | b);
            }
        }
        return new int[]{x0, y0, x1, y1};
    }

    private static int blend(int src, int dst, int alpha, int remaining, int outAlpha) {
        return (src * alpha + dst * remaining + outAlpha / 2) / outAlpha;
    }

    private static boolean valid(double size, double sx, double sy) {
        return Double.isFinite(size) && size > 0 && Double.isFinite(sx) && sx > 0
                && Double.isFinite(sy) && sy > 0;
    }

    synchronized void reset() {
        baseFonts.clear();
        fonts.clear();
        metrics.clear();
        masks.clear();
        maskBytes = 0;
    }
}
