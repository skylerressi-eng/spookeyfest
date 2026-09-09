package com.treegifts.client;

import com.treegifts.core.GiftResult;
import com.treegifts.core.GiftStep;
import com.treegifts.core.LootTables;
import com.treegifts.core.Rarity;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The tree-gift reveal.
 *
 * A wrapped Hardened Wood drops in; axes fly in and thud into it one by one. Each
 * hit cracks it a little more and (per the pre-rolled {@link GiftResult}) either
 * bumps it to a higher rarity or splits it open for loot. Reach Legendary and it
 * might just spin into a Mango Dye instead. Then the reward flies out under a fan
 * of rotating rarity-coloured rays, with confetti.
 *
 * The screen is a pure *player* of the {@link GiftResult} — it never decides the
 * outcome (that's the Minecraft-free {@link com.treegifts.core.TreeGiftRoller}).
 * Timing runs off wall-clock ms so the animation is smooth regardless of tick rate.
 *
 * NOTE: written against the Fabric / Yarn 1.21.x client API (DrawContext +
 * MatrixStack). If your mappings differ, only the rendering / sound calls in this
 * one file need touching — the roll engine and tests are API-agnostic.
 */
public class TreeGiftScreen extends Screen {

    // --- timeline (ms) ---
    private static final long INTRO_MS      = 700;   // wrapped wood bounces in
    private static final long BEAT_MS       = 800;   // one axe throw + settle
    private static final long MANGO_BEAT_MS = 1650;  // the legendary spin is longer
    private static final long IMPACT_AT     = 340;   // when a thrown axe lands
    private static final long MANGO_TWIST_AT= 1300;  // when the spin becomes a dye

    // --- geometry ---
    private static final int WOOD_SCALE = 6;         // item px -> screen px

    private final GiftResult result;
    private final long startMs = now();
    private final Random rng = new Random();
    private final List<Particle> particles = new ArrayList<Particle>();

    private final ItemStack woodStack;
    private final ItemStack axeStack;
    private final ItemStack rewardStack;

    private int lastImpactHandled = -1;
    private boolean finaleBurstDone = false;
    private long skipToMs = -1; // set when the player clicks to skip

    public TreeGiftScreen(GiftResult result) {
        super(Text.literal("Tree Gift"));
        this.result = result;
        this.woodStack = stack("minecraft:oak_log", Items.OAK_LOG);
        this.axeStack = stack("minecraft:iron_axe", Items.IRON_AXE);
        this.rewardStack = stack(result.loot.itemId, Items.OAK_LOG);
        this.rewardStack.setCount(Math.max(1, Math.min(64, result.loot.amount)));
    }

    // ────────────────────────────────────────────────────────────── lifecycle

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private long elapsed() {
        long e = now() - startMs;
        return skipToMs >= 0 ? Math.max(e, skipToMs) : e;
    }

    // Duration of beat i (the mango beat runs longer for its spin).
    private long beatDur(int i) {
        return result.steps.get(i).type == GiftStep.Type.MANGO ? MANGO_BEAT_MS : BEAT_MS;
    }

    private long beatStart(int i) {
        long t = 0;
        for (int k = 0; k < i; k++) t += beatDur(k);
        return t; // relative to end of intro
    }

    private long totalBeatsMs() {
        return beatStart(result.steps.size());
    }

    private long finaleStartMs() {
        return INTRO_MS + totalBeatsMs();
    }

    // Absolute ms (from screen open) at which beat i's axe lands.
    private long absImpact(int i) {
        long within = result.steps.get(i).type == GiftStep.Type.MANGO ? MANGO_TWIST_AT : IMPACT_AT;
        return INTRO_MS + beatStart(i) + within;
    }

    // ────────────────────────────────────────────────────────────── input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return advanceOrClose() || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Let Esc fall through to the default close.
        if (keyCode == 256) return super.keyPressed(keyCode, scanCode, modifiers);
        return advanceOrClose() || super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** Before the finale: skip straight to the reward. During it: collect & close. */
    private boolean advanceOrClose() {
        if (elapsed() < finaleStartMs()) {
            skipToMs = finaleStartMs();
            lastImpactHandled = result.steps.size() - 1; // don't machine-gun the skipped sounds
            playSound(SoundEvents.BLOCK_WOOD_BREAK, 1.4f);
            return true;
        }
        collectAndClose();
        return true;
    }

    private void collectAndClose() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            String c = result.mango ? "§6" : rarityChatColor(result.finalRarity);
            String line = result.mango
                    ? "§6§lMANGO DYE! §r§7from your tree gift — the 1-in-1000 spin!"
                    : c + result.loot.name + " §7(" + result.finalRarity.display + ") from your tree gift!";
            mc.player.sendMessage(Text.literal("§8[§aTree Gifts§8] §r" + line), false);
        }
        close();
    }

    // ────────────────────────────────────────────────────────────── render

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long e = elapsed();
        long finaleStart = finaleStartMs();
        boolean finale = e >= finaleStart;

        fireImpactsUpTo(e);
        updateParticles();

        // Dim + subtle vignette, our own fill so it's version-proof.
        context.fill(0, 0, width, height, 0xC8000000);
        context.fillGradient(0, 0, width, height, 0x00000000, 0x66000000);

        int cx = width / 2;
        int cy = height / 2 - 6;

        // Screen shake pulses briefly after each impact.
        float shake = shakeAt(e);
        MatrixStack ms = context.getMatrices();
        ms.push();
        if (shake > 0.01f) {
            ms.translate((rng.nextFloat() - 0.5f) * shake, (rng.nextFloat() - 0.5f) * shake, 0);
        }

        if (finale) {
            renderFinale(context, cx, cy, e - finaleStart);
        } else if (e < INTRO_MS) {
            renderIntro(context, cx, cy, e / (float) INTRO_MS);
        } else {
            renderBeats(context, cx, cy, e - INTRO_MS);
        }

        renderParticles(context);
        ms.pop();

        renderHud(context, e, finale);
    }

    /** Wrapped wood bounces in from nothing. */
    private void renderIntro(DrawContext ctx, int cx, int cy, float t) {
        float s = easeOutBack(clamp01(t));
        drawGlowDisc(ctx, cx, cy, (int) (46 * s), 0x33FFFFFF);
        drawWood(ctx, cx, cy, WOOD_SCALE * s, 0, /*wrapped*/ true, null, 0);
        // little anticipation sparkles
        if (rng.nextInt(3) == 0) {
            spawn(cx + rand(-40, 40), cy + rand(-30, 30), rand(-10, 10), rand(-30, -5),
                    0xFFFFF0A0, 500 + rng.nextInt(300), 2f);
        }
    }

    /** The main sequence: fly an axe in, thud, reveal/upgrade/crack/spin. */
    private void renderBeats(DrawContext ctx, int cx, int cy, long t) {
        // Which beat are we in?
        int b = 0;
        long acc = 0;
        while (b < result.steps.size() - 1 && t >= acc + beatDur(b)) {
            acc += beatDur(b);
            b++;
        }
        long local = t - acc;
        GiftStep step = result.steps.get(b);

        // Rarity + cracks shown right now depend on whether this beat has landed.
        long within = step.type == GiftStep.Type.MANGO ? MANGO_TWIST_AT : IMPACT_AT;
        boolean landed = local >= within;
        int impactsDone = b + (landed ? 1 : 0);
        Rarity shown = impactsDone == 0 ? null : result.steps.get(impactsDone - 1).rarity;
        int cracks = Math.max(0, impactsDone - 1) + (landed && step.type != GiftStep.Type.MANGO ? 1 : 0);

        // Glow behind, brightening as rarity climbs.
        int glow = shown == null ? 0x33FFFFFF : withAlpha(shown.color, 0.28f);
        drawGlowDisc(ctx, cx, cy, 48 + (shown == null ? 0 : shown.ordinal() * 5), glow);

        if (step.type == GiftStep.Type.MANGO) {
            renderSpin(ctx, cx, cy, local);
            return;
        }

        boolean terminalCrack = step.type == GiftStep.Type.CRACK;
        if (landed && terminalCrack) {
            // Wood splits apart; halves fly outward as the reward emerges.
            float sp = easeOutCubic(clamp01((local - within) / (float) (beatDur(b) - within)));
            drawSplitWood(ctx, cx, cy, shown, cracks, sp);
        } else {
            // Impact "punch": briefly overscale on landing, then settle.
            float punch = landed ? 1f + 0.18f * (1f - clamp01((local - within) / 130f)) : 1f;
            drawWood(ctx, cx, cy, WOOD_SCALE * punch, cracks, false, shown, landed ? flashAt(local - within) : 0f);
        }

        // The flying axe (only before it lands).
        if (!landed) {
            float f = clamp01(local / (float) within);
            drawFlyingAxe(ctx, cx, cy, f, b);
        }
    }

    /** Legendary → Mango: the wood spins up, glows, and pops into a dye. */
    private void renderSpin(DrawContext ctx, int cx, int cy, long local) {
        if (local < MANGO_TWIST_AT) {
            // Accelerating spin, tightening glow.
            float p = clamp01(local / (float) MANGO_TWIST_AT);
            float spins = 2f + easeInCubic(p) * 10f;          // gets faster
            float angle = spins * 360f * p;
            float scale = WOOD_SCALE * (1f + 0.15f * (float) Math.sin(p * Math.PI * 6));
            drawGlowDisc(ctx, cx, cy, (int) (54 + 30 * p), withAlpha(LootTables.MANGO_COLOR, 0.35f + 0.4f * p));
            drawWoodRotated(ctx, cx, cy, scale, angle, Rarity.LEGENDARY);
            if (rng.nextInt(2) == 0) {
                spawn(cx + rand(-30, 30), cy + rand(-30, 30), rand(-40, 40), rand(-60, 20),
                        LootTables.MANGO_COLOR, 400, 2.5f);
            }
        } else {
            // The pop: dye appears with a warm burst.
            float p = clamp01((local - MANGO_TWIST_AT) / (float) (MANGO_BEAT_MS - MANGO_TWIST_AT));
            drawGlowDisc(ctx, cx, cy, (int) (84 - 20 * p), withAlpha(LootTables.MANGO_COLOR, 0.6f * (1f - p)));
            drawBigItem(ctx, rewardStack, cx, cy, WOOD_SCALE * easeOutBack(p));
        }
    }

    /** Reward on a fan of rotating rays, confetti raining, a collect prompt. */
    private void renderFinale(DrawContext ctx, int cx, int cy, long local) {
        int color = result.loot.color;
        float in = easeOutBack(clamp01(local / 400f));

        drawRays(ctx, cx, cy, 30, 150, 16, withAlpha(color, 0.22f), local * 0.03f);
        drawRays(ctx, cx, cy, 20, 110, 16, withAlpha(color, 0.30f), -local * 0.05f + 11);
        drawGlowDisc(ctx, cx, cy, 60, withAlpha(color, 0.30f));
        drawBigItem(ctx, rewardStack, cx, cy - 4, WOOD_SCALE * in);

        if (!finaleBurstDone) {
            finaleBurstDone = true;
            playSound(result.mango ? SoundEvents.ITEM_TOTEM_USE : SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f);
            for (int i = 0; i < 60; i++) {
                spawn(cx, cy, rand(-120, 120), rand(-160, -20), confetti(color), 900 + rng.nextInt(700), 3f);
            }
        }
        // Ongoing confetti drizzle from the top.
        if (rng.nextInt(2) == 0) {
            spawn(rand(0, width), -6, rand(-15, 15), rand(20, 70), confetti(color), 1400, 3f);
        }
    }

    // ────────────────────────────────────────────────────────────── HUD text

    private void renderHud(DrawContext ctx, long e, boolean finale) {
        int top = Math.max(14, height / 2 - 118);
        drawCentered(ctx, "§6§lTREE GIFT", width / 2, top, 0xFFFFFFFF);

        // Current rarity banner (mid-animation) or the reward name (finale).
        if (finale) {
            String name = result.loot.name + (rewardStack.getCount() > 1 ? " §7x" + result.loot.amount : "");
            drawCentered(ctx, boldColored(result.loot.color, name), width / 2, height / 2 + 60, 0xFFFFFFFF);
            drawCentered(ctx, "§7" + result.loot.flavor, width / 2, height / 2 + 74, 0xFFFFFFFF);
            String tier = result.mango ? "§6✦ MANGO DYE ✦" : rarityChatColor(result.finalRarity) + result.finalRarity.display;
            drawCentered(ctx, tier, width / 2, height / 2 + 90, 0xFFFFFFFF);
            // Pulsing prompt.
            if ((e / 500) % 2 == 0) {
                drawCentered(ctx, "§eClick or press a key to collect", width / 2, height / 2 + 108, 0xFFFFFFFF);
            }
        } else {
            Rarity shown = currentShownRarity(e);
            String banner = shown == null ? "§7Wrapped Hardened Wood…"
                    : rarityChatColor(shown) + shown.display + " §7Hardened Wood";
            drawCentered(ctx, banner, width / 2, height / 2 + 66, 0xFFFFFFFF);
            drawCentered(ctx, "§8click to skip", width / 2, height - 24, 0xFFFFFFFF);
        }
    }

    private Rarity currentShownRarity(long e) {
        if (e < INTRO_MS) return null;
        long t = e - INTRO_MS;
        Rarity shown = null;
        long acc = 0;
        for (int i = 0; i < result.steps.size(); i++) {
            long within = result.steps.get(i).type == GiftStep.Type.MANGO ? MANGO_TWIST_AT : IMPACT_AT;
            if (t >= acc + within) shown = result.steps.get(i).rarity;
            acc += beatDur(i);
        }
        return shown;
    }

    // ────────────────────────────────────────────────────────── impacts / sound

    /** Fire the burst + sound for every beat whose axe has landed but wasn't handled yet. */
    private void fireImpactsUpTo(long e) {
        int cx = width / 2, cy = height / 2 - 6;
        for (int b = lastImpactHandled + 1; b < result.steps.size(); b++) {
            if (e < absImpact(b)) break;
            lastImpactHandled = b;
            GiftStep step = result.steps.get(b);
            switch (step.type) {
                case REVEAL:
                    playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f);
                    burst(cx, cy, 14, step.rarity.color, 60);
                    break;
                case UPGRADE:
                    // Rising ding as the rarity climbs.
                    playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f + step.rarity.ordinal() * 0.22f);
                    burst(cx, cy, 18, step.rarity.color, 80);
                    break;
                case CRACK:
                    playSound(SoundEvents.BLOCK_WOOD_BREAK, 0.9f);
                    playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.1f);
                    burst(cx, cy, 40, step.rarity.color, 130);
                    break;
                case MANGO:
                    playSound(SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 1.4f);
                    burst(cx, cy, 70, LootTables.MANGO_COLOR, 170);
                    break;
            }
        }
    }

    private void playSound(SoundEvent event, float pitch) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.getSoundManager() != null) {
            mc.getSoundManager().play(PositionedSoundInstance.master(event, pitch));
        }
    }

    // ────────────────────────────────────────────────────────── drawing helpers

    /** The wood block, cracks overlaid, optional rarity tint and a white impact flash. */
    private void drawWood(DrawContext ctx, int cx, int cy, float scale, int cracks,
                          boolean wrapped, Rarity rarity, float flash) {
        // Rarity-tinted backing plate so the wood reads at its current tier.
        if (rarity != null) {
            int r = (int) (16 * scale / 2f) + 6;
            drawGlowDisc(ctx, cx, cy, r, withAlpha(rarity.color, 0.5f));
        }
        drawBigItem(ctx, wrapped ? tag(woodStack) : woodStack, cx, cy, scale);
        drawCracks(ctx, cx, cy, (int) (16 * scale), cracks);
        if (flash > 0.01f) {
            int r = (int) (16 * scale / 2f) + 4;
            drawGlowDisc(ctx, cx, cy, r, withAlpha(0xFFFFFFFF, 0.6f * flash));
        }
    }

    private void drawWoodRotated(DrawContext ctx, int cx, int cy, float scale, float angleDeg, Rarity rarity) {
        MatrixStack ms = ctx.getMatrices();
        ms.push();
        ms.translate(cx, cy, 0);
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angleDeg));
        ms.translate(-cx, -cy, 0);
        drawBigItem(ctx, woodStack, cx, cy, scale);
        ms.pop();
    }

    /** Terminal crack: the two halves slide apart while the reward peeks through. */
    private void drawSplitWood(DrawContext ctx, int cx, int cy, Rarity rarity, int cracks, float sp) {
        int off = (int) (sp * 34);
        drawGlowDisc(ctx, cx, cy, 40, withAlpha(rarity == null ? 0xFFFFFFFF : rarity.color, 0.4f * (1f - sp)));
        drawBigItem(ctx, woodStack, cx - off, cy - (int) (sp * 8), WOOD_SCALE * (1f - 0.3f * sp));
        drawBigItem(ctx, woodStack, cx + off, cy + (int) (sp * 8), WOOD_SCALE * (1f - 0.3f * sp));
        if (sp > 0.35f) {
            drawBigItem(ctx, rewardStack, cx, cy, WOOD_SCALE * easeOutBack(clamp01((sp - 0.35f) / 0.65f)) * 0.9f);
        }
    }

    private void drawFlyingAxe(DrawContext ctx, int cx, int cy, float f, int beat) {
        boolean fromLeft = (beat % 2) == 0;
        float startX = fromLeft ? -30 : width + 30;
        float x = lerp(startX, cx, easeOutCubic(f));
        float y = lerp(cy - 70, cy, f) + (float) Math.sin(f * Math.PI) * -18f; // slight arc
        float spin = (fromLeft ? 1 : -1) * f * 540f;
        MatrixStack ms = ctx.getMatrices();
        ms.push();
        ms.translate(x, y, 0);
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(spin));
        ms.scale(2.2f, 2.2f, 1f);
        ms.translate(-8, -8, 0);
        ctx.drawItem(axeStack, 0, 0);
        ms.pop();
    }

    private void drawBigItem(DrawContext ctx, ItemStack stack, int cx, int cy, float scale) {
        if (scale <= 0.01f) return;
        MatrixStack ms = ctx.getMatrices();
        ms.push();
        ms.translate(cx, cy, 0);
        ms.scale(scale, scale, 1f);
        ms.translate(-8, -8, 0);
        ctx.drawItem(stack, 0, 0);
        ms.pop();
    }

    /** Jagged dark cracks spidering out from the centre; stable frame to frame. */
    private void drawCracks(DrawContext ctx, int cx, int cy, int size, int count) {
        if (count <= 0) return;
        Random cr = new Random(0xC0FFEEL + count * 31L); // deterministic per crack-count
        int reach = size / 2 + 2;
        for (int i = 0; i < count * 3; i++) {
            double a = cr.nextDouble() * Math.PI * 2;
            int len = reach / 2 + cr.nextInt(reach / 2 + 1);
            int px = cx, py = cy;
            int segs = 4;
            for (int s = 0; s < segs; s++) {
                double aa = a + (cr.nextDouble() - 0.5) * 0.6;
                int nx = px + (int) (Math.cos(aa) * len / segs);
                int ny = py + (int) (Math.sin(aa) * len / segs);
                thickLine(ctx, px, py, nx, ny, 0xCC201008);
                px = nx; py = ny;
            }
        }
    }

    private void drawGlowDisc(DrawContext ctx, int cx, int cy, int radius, int color) {
        if (radius <= 0) return;
        for (int dy = -radius; dy <= radius; dy++) {
            int dx = (int) Math.sqrt((double) radius * radius - dy * dy);
            ctx.fill(cx - dx, cy + dy, cx + dx, cy + dy + 1, color);
        }
    }

    /** A fan of thin rotating rays (matrix-rotated rectangles). */
    private void drawRays(DrawContext ctx, int cx, int cy, int inner, int outer, int count, int color, float rotDeg) {
        MatrixStack ms = ctx.getMatrices();
        for (int i = 0; i < count; i++) {
            float a = rotDeg + i * (360f / count);
            ms.push();
            ms.translate(cx, cy, 0);
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(a));
            ctx.fill(inner, -2, outer, 2, color);
            ms.pop();
        }
    }

    private void thickLine(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        // Cheap 2px line via stepping — fine for a handful of short crack segments.
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1)) + 1;
        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / steps;
            int y = y1 + (y2 - y1) * i / steps;
            ctx.fill(x, y, x + 2, y + 2, color);
        }
    }

    private void drawCentered(DrawContext ctx, String legacy, int x, int y, int color) {
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(legacy), x, y, color);
    }

    // ────────────────────────────────────────────────────────── particles

    private static final class Particle {
        double x, y, vx, vy;
        long spawn, life;
        int color;
        float size;
    }

    private void spawn(double x, double y, double vx, double vy, int color, long life, float size) {
        Particle p = new Particle();
        p.x = x; p.y = y; p.vx = vx; p.vy = vy;
        p.spawn = now(); p.life = life; p.color = color; p.size = size;
        particles.add(p);
    }

    private void burst(int cx, int cy, int n, int color, double speed) {
        for (int i = 0; i < n; i++) {
            double a = rng.nextDouble() * Math.PI * 2;
            double sp = speed * (0.3 + rng.nextDouble());
            spawn(cx, cy, Math.cos(a) * sp, Math.sin(a) * sp - 20, color, 500 + rng.nextInt(500), 2.5f);
        }
    }

    private void updateParticles() {
        long t = now();
        particles.removeIf(p -> t - p.spawn > p.life);
    }

    private void renderParticles(DrawContext ctx) {
        long t = now();
        for (Particle p : particles) {
            double age = (t - p.spawn) / 1000.0;         // seconds
            double px = p.x + p.vx * age;
            double py = p.y + p.vy * age + 0.5 * 320.0 * age * age; // gravity
            float lifeFrac = 1f - clamp01((t - p.spawn) / (float) p.life);
            int color = withAlpha(p.color, 0.15f + 0.85f * lifeFrac);
            int s = Math.max(1, (int) (p.size * (0.5f + lifeFrac)));
            ctx.fill((int) px, (int) py, (int) px + s, (int) py + s, color);
        }
    }

    // ────────────────────────────────────────────────────────── math / util

    private float shakeAt(long e) {
        float max = 0f;
        for (int b = 0; b <= lastImpactHandled && b < result.steps.size(); b++) {
            long since = e - absImpact(b);
            if (since < 0 || since > 200) continue;
            boolean big = result.steps.get(b).isTerminal();
            float amt = (big ? 9f : 4f) * (1f - since / 200f);
            if (amt > max) max = amt;
        }
        return max;
    }

    private float flashAt(long sinceImpact) {
        return sinceImpact < 0 ? 0f : Math.max(0f, 1f - sinceImpact / 160f);
    }

    private int confetti(int base) {
        int[] palette = {0xFFFF5555, 0xFF55FF55, 0xFF5599FF, 0xFFFFD24A, 0xFFFF7BE0, base | 0xFF000000};
        return palette[rng.nextInt(palette.length)];
    }

    private double rand(double lo, double hi) {
        return lo + rng.nextDouble() * (hi - lo);
    }

    private static float clamp01(float t) { return t < 0 ? 0 : (t > 1 ? 1 : t); }
    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    private static float easeOutCubic(float t) { float u = 1 - t; return 1 - u * u * u; }
    private static float easeInCubic(float t) { return t * t * t; }
    private static float easeOutBack(float t) {
        float c1 = 1.70158f, c3 = c1 + 1;
        float u = t - 1;
        return 1 + c3 * u * u * u + c1 * u * u;
    }

    /** Replace the alpha of a packed ARGB with {@code a} in [0,1]. */
    private static int withAlpha(int argb, float a) {
        int alpha = Math.max(0, Math.min(255, (int) (a * 255)));
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    private static String rarityChatColor(Rarity r) {
        switch (r) {
            case COMMON:    return "§f";
            case UNCOMMON:  return "§a";
            case RARE:      return "§9";
            case EPIC:      return "§5";
            case LEGENDARY: return "§6";
            default:        return "§f";
        }
    }

    private static String boldColored(int argb, String text) {
        // Map the loot colour to the nearest legacy code for the label.
        String code;
        if (argb == LootTables.MANGO_COLOR) code = "§6";
        else if (argb == Rarity.COMMON.color) code = "§f";
        else if (argb == Rarity.UNCOMMON.color) code = "§a";
        else if (argb == Rarity.RARE.color) code = "§9";
        else if (argb == Rarity.EPIC.color) code = "§5";
        else code = "§6";
        return code + "§l" + text;
    }

    private static ItemStack stack(String id, net.minecraft.item.Item fallback) {
        try {
            net.minecraft.item.Item item = Registries.ITEM.get(Identifier.of(id));
            if (item != null && item != Items.AIR) return new ItemStack(item);
        } catch (Throwable ignored) {
            // Bad identifier — fall back so a typo in a loot table never crashes the reveal.
        }
        return new ItemStack(fallback);
    }

    /** Give the wrapped-wood stack a hint name so it reads as a mystery gift. */
    private static ItemStack tag(ItemStack base) {
        return base; // (kept simple; the wrapped look comes from the "?" banner + grey glow)
    }
}
