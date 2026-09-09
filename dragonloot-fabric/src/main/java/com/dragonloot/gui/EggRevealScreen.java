package com.dragonloot.gui;

import com.dragonloot.client.Sfx;
import com.dragonloot.core.DragonConfig;
import com.dragonloot.core.Easing;
import com.dragonloot.core.Rarity;
import com.dragonloot.core.RollResult;
import com.dragonloot.data.DragonReward;
import com.dragonloot.render.EggRenderer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Random;

/**
 * The gambling reveal, built like a slot machine so it feels smooth and
 * addicting rather than a static animation:
 *
 *  1. INTRO  — the dragon egg pops in with an overshoot bounce.
 *  2. SPIN   — a horizontal rarity reel whips around and decelerates on an
 *              ease-out-quint curve, so the "tick... tick... tick" spaces out
 *              into real suspense as it crawls to a stop on your result. The egg
 *              shakes hard early, cracks accumulate as it spins, and its glow
 *              bleeds toward the final rarity's color.
 *  3. LOCK   — the reel snaps home, the screen flashes, the egg cracks fully.
 *  4. BREAK  — the egg splits open on a beam of light, particles burst, and the
 *              reward card springs up (ease-out-back). Legendaries get a JACKPOT.
 *
 * Wall-clock timed (frame-rate independent), scaled by the configured animation
 * speed. All drawing is rectangles + text via GuiGraphics — no textures.
 */
public class EggRevealScreen extends Screen {

    private static final double INTRO_MS = 550;
    private static final double SPIN_MS  = 3400;
    private static final double LOCK_MS  = 380;
    private static final double BREAK_MS = 2700;
    private static final double AUTO_CLOSE_MS = 6500;

    private static final int SPLIT_MAX = 5;

    private static final Rarity[] STRIP = {
            Rarity.UNCOMMON, Rarity.RARE, Rarity.UNCOMMON, Rarity.EPIC,
            Rarity.UNCOMMON, Rarity.RARE, Rarity.UNCOMMON, Rarity.LEGENDARY,
            Rarity.UNCOMMON, Rarity.RARE, Rarity.UNCOMMON, Rarity.EPIC
    };
    private static final int LOOPS = 6;

    private final RollResult result;
    private final DragonReward reward;
    private final Rarity finalR;
    private final long startMs;

    private final int targetCells;
    private int lastTickCell = Integer.MIN_VALUE;
    private boolean lockSounded = false;
    private boolean breakSounded = false;

    private final double[] pAngle, pSpeed, pSize;
    private final int[] pColor;

    public EggRevealScreen(RollResult result, DragonReward reward) {
        super(Component.literal("Dragon Egg"));
        this.result = result;
        this.reward = reward;
        this.finalR = result.finalRarity();
        this.startMs = System.currentTimeMillis();

        Random r = new Random(0xE6601L + finalR.ordinal() + System.nanoTime());
        int p;
        do { p = r.nextInt(STRIP.length); } while (STRIP[p] != finalR);
        this.targetCells = LOOPS * STRIP.length + p;

        int n = 54;
        pAngle = new double[n]; pSpeed = new double[n]; pSize = new double[n]; pColor = new int[n];
        int rarA = finalR.argb;
        for (int i = 0; i < n; i++) {
            pAngle[i] = r.nextDouble() * Math.PI * 2.0;
            pSpeed[i] = 1.2 + r.nextDouble() * 3.4;
            pSize[i]  = 2 + r.nextInt(3);
            pColor[i] = (i % 3 == 0) ? 0xFFFFFFFF : (i % 3 == 1 ? rarA : Easing.mixColor(rarA, 0xFFFFFFFF, 0.5));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private double elapsed() {
        double speed = DragonConfig.INSTANCE.animationSpeed;
        if (speed < 0.5) speed = 0.5;
        if (speed > 2.0) speed = 2.0;
        return (System.currentTimeMillis() - startMs) * speed;
    }

    private double spinEnd() { return INTRO_MS + SPIN_MS; }
    private double lockEnd() { return spinEnd() + LOCK_MS; }
    private boolean finished() { return elapsed() >= lockEnd() + 300; }

    private double cellUnits(double e) {
        if (e <= INTRO_MS) return 0;
        double t = Easing.clamp01((e - INTRO_MS) / SPIN_MS);
        return Easing.outQuint(t) * targetCells;
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        double e = elapsed();

        ctx.fill(0, 0, width, height, 0xC0000000);

        int px = Math.max(4, Math.min(width, height) / 46);
        int eggH = EggRenderer.ROWS * px;
        int cx = width / 2;
        int topY = height / 2 - eggH / 2 - 26;

        ctx.drawCenteredString(font, Component.literal("☘ DRAGON EGG GAMBLE ☘"), cx, topY - 40, 0xFFF1C40F);

        if (e < spinEnd()) {
            renderSpin(ctx, e, cx, topY, px);
        } else if (e < lockEnd()) {
            renderLock(ctx, e, cx, topY, px);
        } else {
            renderBreak(ctx, e - lockEnd(), cx, topY, px);
        }

        handleSounds(e);
        maybeAutoClose(e);
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderSpin(GuiGraphics ctx, double e, int cx, int topY, int px) {
        double introT = Easing.clamp01(e / INTRO_MS);
        double spinT = Easing.clamp01((e - INTRO_MS) / SPIN_MS);

        double bounce = e < INTRO_MS ? Easing.outBack(introT) : 1.0;
        int shake = (int) (Math.sin(e / 26.0) * (5.0 * (1.0 - spinT)) * (e < INTRO_MS ? 0 : 1));

        int crackCount = (int) Math.round(Easing.inOutCubic(spinT) * finalR.cracks);
        int glowColor = Easing.mixColor(Rarity.UNCOMMON.argb, finalR.argb, Easing.inOutCubic(spinT));
        double pulse = 0.5 + 0.5 * Math.sin(e / 90.0);

        int drawTop = topY + (int) ((1.0 - bounce) * 40);
        EggRenderer.drawEgg(ctx, cx, drawTop, px, glowColor, crackCount, shake, pulse);

        drawReel(ctx, cellUnits(e), cx, topY + EggRenderer.ROWS * px + 26, false);

        ctx.drawCenteredString(font, Component.literal("spinning for your fate..."),
                cx, topY + EggRenderer.ROWS * px + 62, 0xFFBBBBBB);
    }

    private void renderLock(GuiGraphics ctx, double e, int cx, int topY, int px) {
        double t = Easing.clamp01((e - spinEnd()) / LOCK_MS);
        int shake = (int) (Math.sin(e / 14.0) * 6 * (1 - t));
        EggRenderer.drawEgg(ctx, cx, topY, px, finalR.argb, finalR.cracks, shake, 1.0);
        drawReel(ctx, targetCells, cx, topY + EggRenderer.ROWS * px + 26, true);

        int a = (int) (0x88 * (1 - t));
        ctx.fill(0, 0, width, height, (a << 24) | (finalR.argb & 0xFFFFFF));
    }

    private void renderBreak(GuiGraphics ctx, double tb, int cx, int topY, int px) {
        int split = (int) (Easing.outCubic(Easing.clamp01(tb / 700.0)) * SPLIT_MAX);
        double pulse = 0.5 + 0.5 * Math.sin(tb / 70.0);

        int cyMid = topY + (EggRenderer.ROWS * px) / 2;
        drawParticles(ctx, cx, cyMid, tb);
        EggRenderer.drawEggBroken(ctx, cx, topY, px, finalR.argb, split, pulse);

        double cardT = Easing.outBack(Easing.clamp01((tb - 350) / 650.0));
        if (cardT > 0) {
            int cardW = Math.min(width - 40, 260);
            int cardH = 74;
            int cardX = cx - cardW / 2;
            int restY = topY + EggRenderer.ROWS * px + 20;
            int cardY = (int) (height - (height - restY) * cardT);

            ctx.fill(cardX, cardY, cardX + cardW, cardY + cardH, 0xE0101018);
            drawBorder(ctx, cardX, cardY, cardW, cardH, Easing.mixColor(finalR.argb, 0xFFFFFFFF, pulse * 0.4), 2);

            if (finalR == Rarity.LEGENDARY) {
                ctx.drawCenteredString(font, Component.literal("★  J A C K P O T  ★"),
                        cx, cardY - 16, 0xFFFFD700);
            }
            ctx.drawCenteredString(font, Component.literal(finalR.displayName.toUpperCase() + "!"),
                    cx, cardY + 10, finalR.argb);
            ctx.drawCenteredString(font, Component.literal(reward.name),
                    cx, cardY + 28, Easing.mixColor(finalR.argb, 0xFFFFFFFF, 0.35));
            ctx.drawCenteredString(font, Component.literal(reward.note),
                    cx, cardY + 44, 0xFF9AA0A8);
            ctx.drawCenteredString(font, Component.literal("press any key to continue"),
                    cx, height - 14, 0xFF666B72);
        }
    }

    private void drawReel(GuiGraphics ctx, double cellUnits, int cx, int cyCenter, boolean landed) {
        int cellW = 78, cellH = 30;
        int winW = Math.min(width - 40, 300);
        int winLeft = cx - winW / 2, winRight = cx + winW / 2;
        int top = cyCenter - cellH / 2, bot = cyCenter + cellH / 2;

        ctx.fill(winLeft - 3, top - 3, winRight + 3, bot + 3, 0xFF05070C);

        int center = (int) Math.floor(cellUnits);
        for (int k = center - 4; k <= center + 4; k++) {
            int idx = ((k % STRIP.length) + STRIP.length) % STRIP.length;
            Rarity r = STRIP[idx];
            int cellCx = cx + (int) ((k - cellUnits) * cellW);
            int x1 = cellCx - cellW / 2 + 3, x2 = cellCx + cellW / 2 - 3;
            if (x2 < winLeft || x1 > winRight) continue;
            int body = Easing.mixColor(0xFF000000, r.argb, 0.30);
            ctx.fill(x1, top + 2, x2, bot - 2, body);
            ctx.drawCenteredString(font, Component.literal(shortName(r)), cellCx, cyCenter - 4, r.argb);
        }

        ctx.fill(0, top - 3, winLeft, bot + 3, 0xF00A0A0E);
        ctx.fill(winRight, top - 3, width, bot + 3, 0xF00A0A0E);

        drawBorder(ctx, winLeft - 3, top - 3, winW + 6, cellH + 6, 0xFF2A2F3A, 2);
        int pointer = landed ? finalR.argb : 0xFFFFFFFF;
        drawBorder(ctx, cx - cellW / 2 + 1, top - 1, cellW - 2, cellH + 2, pointer, 2);
        ctx.fill(cx - 4, top - 11, cx + 4, top - 5, pointer);
        ctx.fill(cx - 4, bot + 5, cx + 4, bot + 11, pointer);
    }

    private static String shortName(Rarity r) {
        switch (r) {
            case UNCOMMON: return "UNCMN";
            case RARE: return "RARE";
            case EPIC: return "EPIC";
            default: return "LEGND";
        }
    }

    private void drawParticles(GuiGraphics ctx, int cx, int cy, double tb) {
        double life = tb / 900.0;
        for (int i = 0; i < pAngle.length; i++) {
            double dist = pSpeed[i] * tb / 13.0;
            int x = cx + (int) (Math.cos(pAngle[i]) * dist);
            int y = cy + (int) (Math.sin(pAngle[i]) * dist + 0.02 * tb * life);
            int s = (int) pSize[i];
            int alpha = (int) (0xFF * Math.max(0.0, 1.0 - life * 0.8));
            if (alpha <= 0) continue;
            ctx.fill(x, y, x + s, y + s, ((alpha & 0xFF) << 24) | (pColor[i] & 0xFFFFFF));
        }
    }

    private void drawBorder(GuiGraphics ctx, int x, int y, int w, int h, int color, int t) {
        ctx.fill(x, y, x + w, y + t, color);
        ctx.fill(x, y + h - t, x + w, y + h, color);
        ctx.fill(x, y, x + t, y + h, color);
        ctx.fill(x + w - t, y, x + w, y + h, color);
    }

    private void handleSounds(double e) {
        if (!DragonConfig.INSTANCE.playSounds) return;
        if (e < spinEnd()) {
            int cell = (int) Math.floor(cellUnits(e));
            if (cell != lastTickCell) {
                lastTickCell = cell;
                float progress = (float) Easing.clamp01((e - INTRO_MS) / SPIN_MS);
                Sfx.tick(0.8f + progress * 1.0f);
            }
        } else if (e < lockEnd()) {
            if (!lockSounded) { lockSounded = true; Sfx.crack(1.4f); }
        } else if (!breakSounded) {
            breakSounded = true;
            Sfx.breakOpen();
            Sfx.payout(finalR == Rarity.LEGENDARY);
        }
    }

    private void maybeAutoClose(double e) {
        if (e >= lockEnd() + AUTO_CLOSE_MS) close();
    }

    private void close() {
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (finished() || keyCode == 256) { close(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (finished()) { close(); return true; }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
