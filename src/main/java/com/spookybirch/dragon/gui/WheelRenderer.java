package com.spookybirch.dragon.gui;

import java.util.List;

import com.spookybirch.dragon.data.DragonLoot;
import com.spookybirch.dragon.data.DragonType;
import com.spookybirch.dragon.data.LootEntry;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;

/**
 * Renders the roulette wheel, its loot segments, the orbiting ball, the top
 * pointer and the central hub. Per-segment geometry and colours are cached when
 * the loot pool changes via {@link #setLoot}, so the per-frame
 * {@link #render} path does no allocation beyond vanilla font rendering.
 */
public final class WheelRenderer {

    private static final int GOLD = 0xFFFFC531;
    private static final int BEZEL = 0xFF141019;
    private static final int HUB = 0xFF0E0A14;
    private static final int DIVIDER = 0x70FFE4A8;

    // Radius fractions of the wheel radius.
    private static final double F_RIM_OUT = 1.00;
    private static final double F_RIM_IN = 0.90;
    private static final double F_SEG_OUT = 0.895;
    private static final double F_SEG_IN = 0.34;
    private static final double F_GEM = 0.74;
    private static final double F_LABEL = 0.60;
    private static final double F_HUB = 0.315;
    private static final double F_BALL = 0.74;

    private DragonLoot loot;
    private int n;
    private double segAngle;
    private int[] segColor = new int[0];
    private int[] segBase = new int[0];
    private String[] shortNames = new String[0];

    public void setLoot(DragonLoot loot) {
        if (loot == this.loot) return;
        this.loot = loot;
        List<LootEntry> es = loot.entries();
        n = es.size();
        segAngle = 360.0 / Math.max(1, n);
        segColor = new int[n];
        segBase = new int[n];
        shortNames = new String[n];
        for (int i = 0; i < n; i++) {
            LootEntry e = es.get(i);
            segColor[i] = e.rarity.color;
            // Alternating dark base, lightly tinted by rarity for subtle variety.
            int baseDark = (i % 2 == 0) ? 0xFF241B30 : 0xFF1A1426;
            segBase[i] = DrawUtil.lerp(baseDark, e.rarity.color, 0.16f);
            shortNames[i] = e.shortName;
        }
    }

    /**
     * Draw the whole wheel.
     *
     * @param wheelAngle  current wheel rotation (deg, clockwise)
     * @param ballAngle   current ball screen angle (deg, 0 = top)
     * @param showTrail   draw the ball's motion trail (during a spin)
     * @param trailAlpha  0-1 trail strength
     * @param showLabels  draw per-segment text labels
     */
    public void render(FontRenderer fr, double cx, double cy, double radius,
                       double wheelAngle, double ballAngle, DragonType dragon,
                       boolean showTrail, float trailAlpha, boolean showLabels) {
        if (n == 0) return;
        int accent = dragon.accent;

        DrawUtil.begin();

        // Ambient halo behind the wheel.
        DrawUtil.radialGlow(cx, cy, radius * 1.28,
                DrawUtil.withAlpha(accent, 0.18f), DrawUtil.withAlpha(accent, 0f), 48);

        // Outer bezel + accent rim.
        DrawUtil.disc(cx, cy, radius * 1.015, BEZEL, 64);
        DrawUtil.ring(cx, cy, radius * F_RIM_IN, radius * F_RIM_OUT, DrawUtil.withAlpha(accent, 0.85f), 72);
        DrawUtil.ring(cx, cy, radius * (F_RIM_IN - 0.015), radius * F_RIM_IN, 0xFF000000, 72);

        // Segment fills.
        double rOut = radius * F_SEG_OUT;
        double rIn = radius * F_SEG_IN;
        int steps = Math.max(4, (int) (segAngle / 6) + 3);
        for (int i = 0; i < n; i++) {
            double a0 = i * segAngle + wheelAngle;
            double a1 = (i + 1) * segAngle + wheelAngle;
            DrawUtil.arc(cx, cy, rIn, rOut, a0, a1, segBase[i], steps);
            // Thin rarity-coloured accent band at the outer edge of the pocket.
            DrawUtil.arc(cx, cy, rOut * 0.93, rOut, a0 + 0.5, a1 - 0.5,
                    DrawUtil.withAlpha(segColor[i], 0.8f), steps);
        }

        // Dividers between pockets.
        for (int i = 0; i < n; i++) {
            double a = i * segAngle + wheelAngle;
            DrawUtil.thickLine(DrawUtil.px(cx, a, rIn), DrawUtil.py(cy, a, rIn),
                    DrawUtil.px(cx, a, rOut), DrawUtil.py(cy, a, rOut), 1.4, DIVIDER);
        }

        // Rarity gems on the pocket ring.
        double rGem = radius * F_GEM;
        double gemR = Math.max(2.5, radius * 0.035);
        for (int i = 0; i < n; i++) {
            double a = (i + 0.5) * segAngle + wheelAngle;
            double gx = DrawUtil.px(cx, a, rGem), gy = DrawUtil.py(cy, a, rGem);
            DrawUtil.radialGlow(gx, gy, gemR * 2.4,
                    DrawUtil.withAlpha(segColor[i], 0.55f), DrawUtil.withAlpha(segColor[i], 0f), 14);
            DrawUtil.disc(gx, gy, gemR, segColor[i], 14);
            DrawUtil.disc(gx, gy, gemR * 0.45, 0xFFFFFFFF, 10);
        }

        // Central hub.
        DrawUtil.radialGlow(cx, cy, radius * (F_HUB + 0.12),
                DrawUtil.withAlpha(accent, 0.5f), DrawUtil.withAlpha(accent, 0f), 40);
        DrawUtil.disc(cx, cy, radius * F_HUB, HUB, 48);
        DrawUtil.ring(cx, cy, radius * (F_HUB - 0.02), radius * F_HUB, DrawUtil.withAlpha(accent, 0.9f), 48);
        // Small dragon emblem: a diamond gem in the accent colour.
        drawEmblem(cx, cy, radius * 0.11, accent);

        // Ball motion trail (drawn behind the ball, i.e. at higher angles since
        // the ball travels counter-clockwise = decreasing angle).
        if (showTrail && trailAlpha > 0.01f) {
            double rBall = radius * F_BALL;
            double ballR = Math.max(3.0, radius * 0.045);
            for (int k = 1; k <= 6; k++) {
                double a = ballAngle + k * 7.0;
                double fade = trailAlpha * (1f - k / 7f) * 0.5f;
                DrawUtil.radialGlow(DrawUtil.px(cx, a, rBall), DrawUtil.py(cy, a, rBall),
                        ballR * 1.8, DrawUtil.withAlpha(0xFFFFFFFF, fade),
                        DrawUtil.withAlpha(0xFFFFFFFF, 0f), 12);
            }
        }

        // The ball itself: glow + white sphere + highlight.
        double rBall = radius * F_BALL;
        double ballR = Math.max(3.0, radius * 0.045);
        double bx = DrawUtil.px(cx, ballAngle, rBall), by = DrawUtil.py(cy, ballAngle, rBall);
        DrawUtil.radialGlow(bx, by, ballR * 3.0,
                DrawUtil.withAlpha(0xFFFFFFFF, 0.35f), DrawUtil.withAlpha(0xFFFFFFFF, 0f), 16);
        DrawUtil.disc(bx, by, ballR, 0xFFF2F2F6, 18);
        DrawUtil.disc(bx - ballR * 0.3, by - ballR * 0.3, ballR * 0.4, 0xFFFFFFFF, 10);
        DrawUtil.disc(bx + ballR * 0.25, by + ballR * 0.25, ballR * 0.55, 0x40202028, 10);

        // Top pointer: a gold triangle pointing down into the rim.
        double pTipY = cy - radius * (F_RIM_IN - 0.01);
        double pTopY = cy - radius * (F_RIM_OUT + 0.12);
        double pHalf = Math.max(5, radius * 0.05);
        DrawUtil.triangle(cx, pTipY, cx - pHalf, pTopY, cx + pHalf, pTopY, GOLD);
        DrawUtil.triangle(cx, pTipY + 1, cx - pHalf * 0.55, pTopY + 2, cx + pHalf * 0.55, pTopY + 2, 0xFFFFE9A8);

        DrawUtil.end();

        // Upright labels (separate pass — text uses the font renderer).
        if (showLabels) {
            drawLabels(fr, cx, cy, radius, wheelAngle);
        }
    }

    private void drawEmblem(double cx, double cy, double r, int accent) {
        DrawUtil.triangle(cx, cy - r, cx - r * 0.8, cy, cx + r * 0.8, cy, accent);
        DrawUtil.triangle(cx, cy + r, cx - r * 0.8, cy, cx + r * 0.8, cy, DrawUtil.lerp(accent, 0xFF000000, 0.25f));
        DrawUtil.disc(cx - r * 0.2, cy - r * 0.15, r * 0.22, 0xFFFFFFFF, 8);
    }

    private void drawLabels(FontRenderer fr, double cx, double cy, double radius, double wheelAngle) {
        float scale = (float) (radius / 150.0) * 0.72f;
        scale = scale < 0.5f ? 0.5f : (scale > 1.0f ? 1.0f : scale);
        double rLabel = radius * F_LABEL;
        for (int i = 0; i < n; i++) {
            double a = (i + 0.5) * segAngle + wheelAngle;
            double lx = DrawUtil.px(cx, a, rLabel);
            double ly = DrawUtil.py(cy, a, rLabel);
            String s = shortNames[i];
            int w = fr.getStringWidth(s);
            GlStateManager.pushMatrix();
            GlStateManager.translate(lx, ly, 0);
            GlStateManager.scale(scale, scale, 1f);
            fr.drawStringWithShadow(s, -w / 2f, -fr.FONT_HEIGHT / 2f, segColor[i]);
            GlStateManager.popMatrix();
        }
        GlStateManager.color(1f, 1f, 1f, 1f);
    }
}
