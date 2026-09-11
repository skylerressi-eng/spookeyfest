package com.spookybirch.reforge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

import java.util.Random;

/**
 * Draws a {@link ReforgeAnimation} as an overlay on top of the reforge GUI.
 * Pure GL + {@link Gui#drawRect} primitives — no textures, no entities — so it
 * has no assets to ship and runs the same on any 1.8.9 client.
 */
public final class ReforgeGambleRenderer {

    private static final Random SHAKE = new Random();

    // Palette.
    private static final int CARD_BG   = 0xE60D0D12;
    private static final int CARD_EDGE = 0xFFFF8C1A;   // pumpkin orange
    private static final int TITLE     = 0xFFFF8C1A;
    private static final int GREY      = 0xFFBBBBBB;
    private static final int YELLOW    = 0xFFFFE255;
    private static final int AQUA      = 0xFF55FFFF;
    private static final int GOLD      = 0xFFFFD24A;
    private static final int ANVIL     = 0xFF3B3B42;
    private static final int ANVIL_HI  = 0xFF5C5C66;
    private static final int ANVIL_LO  = 0xFF212126;
    private static final int HAMMER_H  = 0xFF6E6E78;
    private static final int HAMMER_HI = 0xFF9A9AA6;
    private static final int HANDLE    = 0xFF7A4A22;

    private ReforgeGambleRenderer() {}

    public static void render(Minecraft mc, ReforgeAnimation anim) {
        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();
        FontRenderer fr = mc.fontRendererObj;

        int cx = sw / 2;
        int cy = sh / 2;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0); // SRC_ALPHA, ONE_MINUS_SRC_ALPHA
        GlStateManager.disableTexture2D();

        // Whole-scene screen shake.
        double sh2 = anim.shake();
        if (sh2 > 0.05) {
            GlStateManager.translate((SHAKE.nextDouble() - 0.5) * sh2 * 2,
                    (SHAKE.nextDouble() - 0.5) * sh2 * 2, 0);
        }

        // Card panel — kept compact so the GUI behind it stays readable.
        int halfW = 118, halfH = 92;
        GlStateManager.enableTexture2D();
        drawPanel(cx - halfW, cy - halfH, cx + halfW, cy + halfH);

        // Title.
        drawCentered(fr, "⚒ REFORGE GAMBLE", cx, cy - halfH + 7, TITLE);

        // The reforge label that swaps on each hit.
        drawReforgeLabel(fr, anim, cx, cy - halfH + 26);

        // Anvil + hammer live in the lower half of the card.
        int impactX = cx;
        int impactY = cy + 30;
        GlStateManager.disableTexture2D();
        if (anim.breakProgress() < 1.0) {
            drawAnvil(impactX, impactY, 1.0f - (float) anim.breakProgress(), anim.crackLevel());
        }
        drawFragments(anim, impactX, impactY);
        if (!anim.pastPointOfNoReturn() || anim.phase() == ReforgeAnimation.Phase.HIT3_IMPACT) {
            drawHammer(impactX, impactY, anim.hammerAngleDeg());
        }
        drawSparks(anim, impactX, impactY);

        // Reveal flourish.
        if (anim.revealAlpha() > 0.01) {
            drawReveal(fr, anim, cx, cy - 6);
        }

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.popMatrix();
    }

    // --------------------------------------------------------------- pieces

    private static void drawPanel(int x1, int y1, int x2, int y2) {
        Gui.drawRect(x1, y1, x2, y2, CARD_BG);
        // 1px orange frame.
        Gui.drawRect(x1, y1, x2, y1 + 1, CARD_EDGE);
        Gui.drawRect(x1, y2 - 1, x2, y2, CARD_EDGE);
        Gui.drawRect(x1, y1, x1 + 1, y2, CARD_EDGE);
        Gui.drawRect(x2 - 1, y1, x2, y2, CARD_EDGE);
    }

    private static void drawReforgeLabel(FontRenderer fr, ReforgeAnimation anim, int cx, int y) {
        int color;
        switch (anim.shownStage()) {
            case 0:  color = GREY; break;
            case 1:  color = YELLOW; break;
            case 2:  color = AQUA; break;
            default: color = GOLD; break;
        }
        String name = anim.shownName();
        if (name == null || name.isEmpty()) name = "—";
        // Pop the text slightly right after a hit lands.
        float scale = 1.6f;
        drawCenteredScaled(fr, name, cx, y, scale, color);
    }

    private static void drawReveal(FontRenderer fr, ReforgeAnimation anim, int cx, int cy) {
        float a = (float) ReforgeAnimation.clamp01(anim.revealAlpha());
        int white = withAlpha(0xFFFFFFFF, a);
        int gold = withAlpha(GOLD, a);
        GlStateManager.enableTexture2D();
        drawCenteredScaled(fr, "✦ " + anim.finalName() + " ✦", cx,
                cy, (float) (2.0 * anim.revealScale()), gold);
        drawCentered(fr, "reforge applied", cx, cy + 22, white);
    }

    private static void drawAnvil(int ix, int iy, float alpha, double crack) {
        int body = withAlpha(ANVIL, alpha);
        int hi = withAlpha(ANVIL_HI, alpha);
        int lo = withAlpha(ANVIL_LO, alpha);

        // Top working surface (this is where the hammer lands, at iy).
        Gui.drawRect(ix - 26, iy, ix + 26, iy + 8, body);
        Gui.drawRect(ix - 26, iy, ix + 26, iy + 2, hi);          // top highlight
        // Neck.
        Gui.drawRect(ix - 12, iy + 8, ix + 16, iy + 14, body);
        // Waist.
        Gui.drawRect(ix - 8, iy + 14, ix + 8, iy + 22, lo);
        // Base.
        Gui.drawRect(ix - 20, iy + 22, ix + 20, iy + 30, body);
        Gui.drawRect(ix - 20, iy + 28, ix + 20, iy + 30, lo);    // base shadow

        // Cracks grow across the top surface.
        if (crack > 0.02) {
            int crackC = withAlpha(0xFF0A0A0A, (float) Math.min(1.0, crack) * alpha);
            Gui.drawRect(ix - 2, iy + 1, ix - 1, iy + 7, crackC);
            if (crack > 0.4) {
                Gui.drawRect(ix + 6, iy + 1, ix + 7, iy + 6, crackC);
                Gui.drawRect(ix - 12, iy + 2, ix - 11, iy + 7, crackC);
            }
            if (crack > 0.75) {
                Gui.drawRect(ix - 26, iy + 4, ix + 26, iy + 5, crackC); // splitting line
                Gui.drawRect(ix + 14, iy + 1, ix + 15, iy + 8, crackC);
            }
        }
    }

    private static void drawFragments(ReforgeAnimation anim, int ix, int iy) {
        for (ReforgeAnimation.Frag f : anim.frags()) {
            if (!f.active) continue;
            float a = f.alpha();
            int c = withAlpha(ANVIL, a);
            int hi = withAlpha(ANVIL_HI, a);
            GlStateManager.pushMatrix();
            GlStateManager.translate(ix + f.x, iy + f.y, 0);
            GlStateManager.rotate((float) f.rot, 0, 0, 1);
            int s = f.size;
            Gui.drawRect(-s, -s, s, s, c);
            Gui.drawRect(-s, -s, s, -s + 2, hi);
            GlStateManager.popMatrix();
        }
    }

    private static void drawHammer(int ix, int iy, double angleDeg) {
        // Pivot sits up and to the right of the anvil; the head swings down onto
        // the impact point. Local coords: handle runs up-right, head at the tip.
        GlStateManager.pushMatrix();
        GlStateManager.translate(ix + 30, iy - 4, 0);
        GlStateManager.rotate((float) angleDeg, 0, 0, 1);
        // Handle (wooden shaft).
        Gui.drawRect(-3, -2, 40, 2, HANDLE);
        // Head (steel block) at the far end of the handle.
        Gui.drawRect(36, -9, 52, 9, HAMMER_H);
        Gui.drawRect(36, -9, 52, -5, HAMMER_HI);   // top highlight
        Gui.drawRect(48, -9, 52, 9, HAMMER_HI);    // striking face highlight
        GlStateManager.popMatrix();
    }

    private static void drawSparks(ReforgeAnimation anim, int ix, int iy) {
        for (ReforgeAnimation.Spark s : anim.sparks()) {
            if (!s.active) continue;
            int c = withAlpha(s.color, s.alpha());
            int x = (int) (ix + s.x);
            int y = (int) (iy + s.y);
            Gui.drawRect(x, y, x + s.size, y + s.size, c);
        }
    }

    // --------------------------------------------------------------- text utils

    private static void drawCentered(FontRenderer fr, String text, int cx, int y, int color) {
        GlStateManager.enableTexture2D();
        fr.drawStringWithShadow(text, cx - fr.getStringWidth(text) / 2f, y, color);
    }

    private static void drawCenteredScaled(FontRenderer fr, String text, int cx, int cy, float scale, int color) {
        GlStateManager.enableTexture2D();
        GlStateManager.pushMatrix();
        GlStateManager.translate(cx, cy, 0);
        GlStateManager.scale(scale, scale, 1f);
        fr.drawStringWithShadow(text, -fr.getStringWidth(text) / 2f, -fr.FONT_HEIGHT / 2f, color);
        GlStateManager.popMatrix();
    }

    private static int withAlpha(int argb, float a) {
        int alpha = (int) (ReforgeAnimation.clamp01(a) * ((argb >>> 24) == 0 ? 255 : (argb >>> 24)));
        return (alpha << 24) | (argb & 0xFFFFFF);
    }
}
