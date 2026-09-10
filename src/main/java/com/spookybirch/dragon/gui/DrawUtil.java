package com.spookybirch.dragon.gui;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

import org.lwjgl.opengl.GL11;

/**
 * Low-level 2D drawing helpers for the roulette: filled discs, rings, wheel
 * wedges and radial glows, built on Minecraft 1.8.9's {@link Tessellator} /
 * {@link WorldRenderer}. Colours are per-vertex ARGB ints.
 *
 * <p>Angle convention matches the animator: degrees, 0 = top (12 o'clock),
 * clockwise positive. {@link #px}/{@link #py} convert an angle+radius to screen
 * coordinates.
 *
 * <p>All methods assume {@link #begin()} has put GL into untextured, blended 2D
 * mode and that {@link #end()} restores it. No per-call object allocation.
 */
public final class DrawUtil {

    private static final double DEG2RAD = Math.PI / 180.0;

    /** Enter untextured, alpha-blended 2D drawing mode. */
    public static void begin() {
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
    }

    /** Restore normal GL state for text / the rest of the GUI. */
    public static void end() {
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    public static double px(double cx, double angleDeg, double radius) {
        return cx + radius * Math.sin(angleDeg * DEG2RAD);
    }

    public static double py(double cy, double angleDeg, double radius) {
        return cy - radius * Math.cos(angleDeg * DEG2RAD);
    }

    /** Filled disc in a flat colour. */
    public static void disc(double cx, double cy, double radius, int argb, int steps) {
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        float a = a(argb), r = r(argb), g = g(argb), b = b(argb);
        wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(cx, cy, 0).color(r, g, b, a).endVertex();
        for (int i = 0; i <= steps; i++) {
            double ang = 360.0 * i / steps;
            wr.pos(px(cx, ang, radius), py(cy, ang, radius), 0).color(r, g, b, a).endVertex();
        }
        tess.draw();
    }

    /**
     * Radial glow: bright {@code centreArgb} fading to {@code edgeArgb} (usually
     * alpha 0) at the rim. Great for soft halos.
     */
    public static void radialGlow(double cx, double cy, double radius,
                                  int centreArgb, int edgeArgb, int steps) {
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(cx, cy, 0).color(r(centreArgb), g(centreArgb), b(centreArgb), a(centreArgb)).endVertex();
        float er = r(edgeArgb), eg = g(edgeArgb), eb = b(edgeArgb), ea = a(edgeArgb);
        for (int i = 0; i <= steps; i++) {
            double ang = 360.0 * i / steps;
            wr.pos(px(cx, ang, radius), py(cy, ang, radius), 0).color(er, eg, eb, ea).endVertex();
        }
        tess.draw();
    }

    /** Flat-colour ring band between two radii. */
    public static void ring(double cx, double cy, double rInner, double rOuter, int argb, int steps) {
        arc(cx, cy, rInner, rOuter, 0, 360, argb, steps);
    }

    /**
     * A wheel wedge / arc band between two radii from {@code startDeg} to
     * {@code endDeg} (clockwise), in a flat colour.
     */
    public static void arc(double cx, double cy, double rInner, double rOuter,
                           double startDeg, double endDeg, int argb, int steps) {
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        float a = a(argb), r = r(argb), g = g(argb), b = b(argb);
        wr.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= steps; i++) {
            double ang = startDeg + (endDeg - startDeg) * i / steps;
            wr.pos(px(cx, ang, rOuter), py(cy, ang, rOuter), 0).color(r, g, b, a).endVertex();
            wr.pos(px(cx, ang, rInner), py(cy, ang, rInner), 0).color(r, g, b, a).endVertex();
        }
        tess.draw();
    }

    /** Anti-aliased-ish line segment drawn as a thin quad. */
    public static void thickLine(double x1, double y1, double x2, double y2, double width, int argb) {
        double dx = x2 - x1, dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-6) return;
        double nx = -dy / len * width * 0.5;
        double ny = dx / len * width * 0.5;
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        float a = a(argb), r = r(argb), g = g(argb), b = b(argb);
        wr.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x1 + nx, y1 + ny, 0).color(r, g, b, a).endVertex();
        wr.pos(x1 - nx, y1 - ny, 0).color(r, g, b, a).endVertex();
        wr.pos(x2 + nx, y2 + ny, 0).color(r, g, b, a).endVertex();
        wr.pos(x2 - nx, y2 - ny, 0).color(r, g, b, a).endVertex();
        tess.draw();
    }

    /** Filled triangle (used for the pointer). */
    public static void triangle(double x1, double y1, double x2, double y2, double x3, double y3, int argb) {
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        float a = a(argb), r = r(argb), g = g(argb), b = b(argb);
        wr.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x1, y1, 0).color(r, g, b, a).endVertex();
        wr.pos(x2, y2, 0).color(r, g, b, a).endVertex();
        wr.pos(x3, y3, 0).color(r, g, b, a).endVertex();
        tess.draw();
    }

    // --- ARGB component extraction (0-1 floats) ---
    public static float a(int argb) { return ((argb >>> 24) & 0xFF) / 255f; }
    public static float r(int argb) { return ((argb >> 16) & 0xFF) / 255f; }
    public static float g(int argb) { return ((argb >> 8) & 0xFF) / 255f; }
    public static float b(int argb) { return (argb & 0xFF) / 255f; }

    /** Replace the alpha byte of an ARGB colour with {@code alpha} in [0,1]. */
    public static int withAlpha(int argb, float alpha) {
        int a = Math.round(clamp01(alpha) * 255f) & 0xFF;
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    /** Linear blend between two ARGB colours. */
    public static int lerp(int c0, int c1, float t) {
        t = clamp01(t);
        int a = (int) (((c0 >>> 24) & 0xFF) + (((c1 >>> 24) & 0xFF) - ((c0 >>> 24) & 0xFF)) * t);
        int r = (int) (((c0 >> 16) & 0xFF) + (((c1 >> 16) & 0xFF) - ((c0 >> 16) & 0xFF)) * t);
        int g = (int) (((c0 >> 8) & 0xFF) + (((c1 >> 8) & 0xFF) - ((c0 >> 8) & 0xFF)) * t);
        int b = (int) ((c0 & 0xFF) + ((c1 & 0xFF) - (c0 & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static float clamp01(float t) {
        return t < 0 ? 0 : (t > 1 ? 1 : t);
    }

    private DrawUtil() {}
}
