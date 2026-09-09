package com.dragonloot.render;

import com.dragonloot.core.Easing;

import net.minecraft.client.gui.GuiGraphics;

import java.util.Random;

/**
 * Draws a Minecraft-style dragon egg entirely from filled rectangles via
 * GuiGraphics.fill — no texture assets to ship. The egg is a small pixel
 * grid scaled up; cracks are jagged trails baked once with a fixed seed so they
 * look natural but render identically every frame. More cracks = higher tier,
 * and the "break open" mode splits the egg with a beam of rarity light.
 */
public final class EggRenderer {

    // Half-width (egg-pixels) per row, top -> bottom: pointed crown, a bulge
    // low-middle, tucked base — the dragon-egg silhouette.
    private static final int[] HALF_WIDTH = {
            1, 2, 3, 4, 4, 5, 5, 6, 6, 6, 6, 7, 7, 6, 6, 5, 4, 2
    };
    public static final int ROWS = HALF_WIDTH.length;

    private static final int EGG_DARK   = 0xFF160E22;
    private static final int EGG_MID    = 0xFF241634;
    private static final int EGG_LIGHT  = 0xFF33244A;
    private static final int SPECKLE    = 0xFFB14BE0;
    private static final int SPECKLE_2  = 0xFF7A2FB0;
    private static final int SHELL_EDGE = 0xFF0B0714;

    private static final int[][] SPECKLES = {
            {-2, 3, 0}, {1, 5, 1}, {-3, 7, 0}, {3, 9, 1}, {0, 8, 0},
            {-1, 11, 1}, {2, 12, 0}, {-4, 12, 1}, {4, 11, 0}, {-2, 14, 1},
            {1, 15, 0}, {-1, 6, 0}, {3, 6, 1}, {-3, 10, 0}
    };

    private static final int[][][] CRACKS = bakeCracks(11);

    private EggRenderer() {}

    /** The intact (but cracked) egg. */
    public static void drawEgg(GuiGraphics ctx, int cx, int topY, int px, int rarityArgb, int crackCount, int shakeX, double glowPulse) {
        drawGlow(ctx, cx, topY, px, rarityArgb, 0, glowPulse);
        fillBody(ctx, cx + shakeX, topY, px, 0);
        drawCracks(ctx, cx + shakeX, topY, px, rarityArgb, crackCount);
    }

    /** The egg broken open: two halves shoved apart with a beam of light between. */
    public static void drawEggBroken(GuiGraphics ctx, int cx, int topY, int px, int rarityArgb, int split, double glowPulse) {
        drawGlow(ctx, cx, topY, px, rarityArgb, split, glowPulse);

        int beamHalf = Math.max(px, split * px / 2);
        int beamTop = topY - px * 5;
        int beamBot = topY + ROWS * px + px * 2;
        ctx.fill(cx - beamHalf, beamTop, cx + beamHalf, beamBot, withAlpha(rarityArgb, 0x66));
        ctx.fill(cx - beamHalf / 2, beamTop - px * 2, cx + beamHalf / 2, beamBot, withAlpha(0xFFFFFF, 0x99));

        fillBody(ctx, cx, topY, px, -split);
        fillBody(ctx, cx, topY, px, split);
    }

    private static void fillBody(GuiGraphics ctx, int cx, int topY, int px, int halfShift) {
        for (int row = 0; row < ROWS; row++) {
            int hw = HALF_WIDTH[row];
            int y = topY + row * px;
            for (int col = -hw; col <= hw; col++) {
                if (halfShift < 0 && col > 0) continue;
                if (halfShift > 0 && col < 0) continue;
                int x = cx + col * px + halfShift * px;
                ctx.fill(x, y, x + px, y + px, bodyColor(col, row, hw));
            }
        }
    }

    private static int bodyColor(int col, int row, int hw) {
        for (int[] s : SPECKLES) {
            if (s[0] == col && s[1] == row) return s[2] == 0 ? SPECKLE : SPECKLE_2;
        }
        if (col == -hw || col == hw) return SHELL_EDGE;
        int depth = (col + hw) + (ROWS - row);
        if (col < -1 && row < ROWS / 2) return EGG_LIGHT;
        return (depth % 3 == 0) ? EGG_MID : EGG_DARK;
    }

    private static void drawCracks(GuiGraphics ctx, int cx, int topY, int px, int rarityArgb, int crackCount) {
        if (crackCount <= 0) return;
        int crackColor = Easing.mixColor(0xFFFFFFFF, rarityArgb, 0.45);
        int n = Math.min(crackCount, CRACKS.length);
        for (int i = 0; i < n; i++) {
            for (int[] pt : CRACKS[i]) {
                int x = cx + pt[0] * px;
                int y = topY + pt[1] * px;
                int w = Math.max(1, px - 1);
                ctx.fill(x, y, x + w, y + w, crackColor);
            }
        }
    }

    /** Soft rarity-colored halo behind the egg; {@code pulse} in [0,1] brightens it. */
    private static void drawGlow(GuiGraphics ctx, int cx, int topY, int px, int rarityArgb, int split, double pulse) {
        int cyMid = topY + (ROWS * px) / 2;
        int baseR = (maxHalfWidth() + 2) * px + split * px;
        int[] alphas = { 0x10, 0x16, 0x1E, 0x2C };
        int boost = (int) (pulse * 0x22);
        for (int i = alphas.length - 1; i >= 0; i--) {
            int r = baseR + i * px * 2;
            int a = Math.min(0x55, alphas[i] + boost);
            ctx.fill(cx - r, cyMid - r, cx + r, cyMid + r, withAlpha(rarityArgb, a));
        }
    }

    private static int maxHalfWidth() {
        int m = 0;
        for (int h : HALF_WIDTH) m = Math.max(m, h);
        return m;
    }

    private static int[][][] bakeCracks(int count) {
        Random r = new Random(0xD4A6017L);
        int[][][] out = new int[count][][];
        for (int i = 0; i < count; i++) {
            int col = (i % 2 == 0) ? -(i / 2) : (i / 2 + 1);
            int row = 1 + r.nextInt(3);
            int len = 5 + i;
            int[][] path = new int[len][];
            for (int j = 0; j < len; j++) {
                int hw = HALF_WIDTH[Math.min(row, ROWS - 1)];
                int c = clamp(col, -hw, hw);
                path[j] = new int[] { c, Math.min(row, ROWS - 1) };
                row += 1;
                col += r.nextInt(3) - 1;
                if (row >= ROWS) row = ROWS - 1;
            }
            out[i] = path;
        }
        return out;
    }

    private static int withAlpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
