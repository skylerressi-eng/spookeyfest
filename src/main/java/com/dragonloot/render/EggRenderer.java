package com.dragonloot.render;

import net.minecraft.client.gui.Gui;

import java.util.Random;

/**
 * Draws a Minecraft-style dragon egg entirely from filled rectangles — no
 * texture assets to ship. The egg is a small pixel grid scaled up; cracks are
 * jagged rectangle trails baked once with a fixed seed so they look natural but
 * render identically every frame. Higher rarities simply reveal more cracks,
 * and the "break open" mode splits the egg with a beam of rarity-colored light.
 */
public final class EggRenderer {

    // Half-width (in egg-pixels) of each row, top -> bottom: pointed top, a
    // bulge low-middle, tucked base. Defines the egg silhouette.
    private static final int[] HALF_WIDTH = {
            1, 2, 3, 4, 4, 5, 5, 6, 6, 6, 6, 7, 7, 6, 6, 5, 4, 2
    };
    private static final int ROWS = HALF_WIDTH.length;

    private static final int EGG_DARK   = 0xFF160E22;
    private static final int EGG_MID    = 0xFF241634;
    private static final int EGG_LIGHT  = 0xFF33244A;
    private static final int SPECKLE    = 0xFFB14BE0;
    private static final int SPECKLE_2  = 0xFF7A2FB0;
    private static final int SHELL_EDGE = 0xFF0B0714;

    // Fixed speckle spots (col,row) in egg-space — the dragon egg's mottling.
    private static final int[][] SPECKLES = {
            {-2, 3, 0}, {1, 5, 1}, {-3, 7, 0}, {3, 9, 1}, {0, 8, 0},
            {-1, 11, 1}, {2, 12, 0}, {-4, 12, 1}, {4, 11, 0}, {-2, 14, 1},
            {1, 15, 0}, {-1, 6, 0}, {3, 6, 1}, {-3, 10, 0}
    };

    // Up to LEGENDARY.cracks crack paths, baked once. Each is a list of
    // {col,row} egg-space points forming a jagged line down from the crown.
    private static final int[][][] CRACKS = bakeCracks(11);

    private EggRenderer() {}

    /**
     * Draw the intact (but cracked) egg.
     *
     * @param cx        screen x of the egg's vertical centre line
     * @param topY      screen y of the egg's top
     * @param px        pixels per egg-pixel (the scale)
     * @param rarityArgb color of the cracks / inner glow at the current tier
     * @param crackCount how many cracks to reveal (0 = pristine)
     * @param shakeX    horizontal shake offset in screen px
     */
    public static void drawEgg(int cx, int topY, int px, int rarityArgb, int crackCount, int shakeX) {
        drawGlow(cx, topY, px, rarityArgb, false, 0);
        fillBody(cx + shakeX, topY, px, 0);
        drawCracks(cx + shakeX, topY, px, rarityArgb, crackCount, 0);
    }

    /**
     * Draw the egg broken open: two shell halves pushed apart by {@code split}
     * egg-pixels with a beam of rarity light bursting from the gap.
     */
    public static void drawEggBroken(int cx, int topY, int px, int rarityArgb, int split) {
        drawGlow(cx, topY, px, rarityArgb, true, split);

        // Beam of light up through the crack, brightest at the centre.
        int beamHalf = Math.max(px, split * px / 2);
        int beamTop = topY - px * 4;
        int beamBot = topY + ROWS * px + px * 2;
        Gui.drawRect(cx - beamHalf, beamTop, cx + beamHalf, beamBot, withAlpha(rarityArgb, 0x55));
        Gui.drawRect(cx - beamHalf / 2, beamTop - px * 2, cx + beamHalf / 2, beamBot, withAlpha(0xFFFFFF, 0x88));

        // Two shell halves, shoved left/right.
        fillBody(cx, topY, px, -split);
        fillBody(cx, topY, px, split);
    }

    /** Fill the egg silhouette; {@code halfShift} moves each half sideways for the split. */
    private static void fillBody(int cx, int topY, int px, int halfShift) {
        for (int row = 0; row < ROWS; row++) {
            int hw = HALF_WIDTH[row];
            int y = topY + row * px;
            for (int col = -hw; col <= hw; col++) {
                // When split, only draw the matching half (skip the seam column
                // of the opposite side so the two halves read as separate).
                if (halfShift < 0 && col > 0) continue;
                if (halfShift > 0 && col < 0) continue;
                int shift = halfShift * px;
                int x = cx + col * px + shift;
                Gui.drawRect(x, y, x + px, y + px, bodyColor(col, row, hw));
            }
        }
    }

    private static int bodyColor(int col, int row, int hw) {
        // Speckles first.
        for (int[] s : SPECKLES) {
            if (s[0] == col && s[1] == row) return s[2] == 0 ? SPECKLE : SPECKLE_2;
        }
        // Shell edge on the outermost column of each row.
        if (col == -hw || col == hw) return SHELL_EDGE;
        // Simple diagonal shading: lighter top-left, darker bottom-right.
        int depth = (col + hw) + (ROWS - row);
        if (col < -1 && row < ROWS / 2) return EGG_LIGHT;
        return (depth % 3 == 0) ? EGG_MID : EGG_DARK;
    }

    private static void drawCracks(int cx, int topY, int px, int rarityArgb, int crackCount, int halfShift) {
        if (crackCount <= 0) return;
        int crackColor = mix(0xFFFFFF, rarityArgb, 0.45);
        int n = Math.min(crackCount, CRACKS.length);
        for (int i = 0; i < n; i++) {
            for (int[] pt : CRACKS[i]) {
                int x = cx + pt[0] * px;
                int y = topY + pt[1] * px;
                // Crack is a hair thinner than a full egg-pixel so it reads as a line.
                int w = Math.max(1, px - 1);
                Gui.drawRect(x, y, x + w, y + w, crackColor);
            }
        }
    }

    /** Soft rarity-colored halo behind the egg. */
    private static void drawGlow(int cx, int topY, int px, int rarityArgb, boolean broken, int split) {
        int cyMid = topY + (ROWS * px) / 2;
        int baseR = (HALF_WIDTH_MAX() + 2) * px + split * px;
        int[] alphas = { 0x10, 0x14, 0x1C, 0x2A };
        for (int i = alphas.length - 1; i >= 0; i--) {
            int r = baseR + i * px * 2;
            int a = broken ? Math.min(0x40, alphas[i] + 0x14) : alphas[i];
            Gui.drawRect(cx - r, cyMid - r, cx + r, cyMid + r, withAlpha(rarityArgb, a));
        }
    }

    private static int HALF_WIDTH_MAX() {
        int m = 0;
        for (int h : HALF_WIDTH) m = Math.max(m, h);
        return m;
    }

    // ---- crack baking ----

    private static int[][][] bakeCracks(int count) {
        Random r = new Random(0xD4A6017L); // fixed: identical cracks every launch
        int[][][] out = new int[count][][];
        for (int i = 0; i < count; i++) {
            // Later cracks start a touch off-centre and run longer.
            int col = (i % 2 == 0) ? -(i / 2) : (i / 2 + 1);
            int row = 1 + r.nextInt(3);
            int len = 5 + i;                // escalating length
            int[][] path = new int[len][];
            for (int j = 0; j < len; j++) {
                int hw = row < ROWS ? HALF_WIDTH[Math.min(row, ROWS - 1)] : 1;
                int c = clamp(col, -hw, hw);
                path[j] = new int[] { c, Math.min(row, ROWS - 1) };
                // Walk downward with a little sideways jitter.
                row += 1;
                col += r.nextInt(3) - 1;
                if (row >= ROWS) row = ROWS - 1;
            }
            out[i] = path;
        }
        return out;
    }

    // ---- color helpers ----

    private static int mix(int a, int b, double t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int rr = (int) (ar + (br - ar) * t);
        int rg = (int) (ag + (bg - ag) * t);
        int rb = (int) (ab + (bb - ab) * t);
        return 0xFF000000 | (rr << 16) | (rg << 8) | rb;
    }

    private static int withAlpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
