package com.treegifts.client;

import com.treegifts.core.GiftResult;
import com.treegifts.core.GiftStep;
import com.treegifts.core.LootTables;
import com.treegifts.core.Rarity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The tree-gift reveal (Minecraft 26.1.2 GUI system).
 *
 * A wrapped Hardened Wood drops in; axes fly in and thud into it one by one. Each
 * hit cracks it more and (per the pre-rolled {@link GiftResult}) either bumps it a
 * rarity or splits it open for loot. Reach Legendary and it might spin into a
 * Mango Dye. Then the reward flies out under rotating rarity-coloured rays, with
 * confetti.
 *
 * The screen only *plays* the pre-rolled {@link GiftResult}; the outcome is
 * decided by the Minecraft-free {@link com.treegifts.core.TreeGiftRoller}. Timing
 * runs off wall-clock ms so it's smooth regardless of tick rate.
 *
 * 26.x specifics: screens draw via {@code extractRenderState(GuiGraphicsExtractor)}
 * and transform through a JOML {@link Matrix3x2fStack} from {@code pose()}
 * (rotations in radians).
 */
public class TreeGiftScreen extends Screen {

    // --- timeline (ms) ---
    private static final long INTRO_MS       = 700;
    private static final long BEAT_MS        = 800;
    private static final long MANGO_BEAT_MS  = 1650;
    private static final long IMPACT_AT      = 340;
    private static final long MANGO_TWIST_AT = 1300;

    private static final int WOOD_SCALE = 6; // item px (16) -> screen px multiplier

    private final GiftResult result;
    private final long startMs = now();
    private final Random rng = new Random();
    private final List<Particle> particles = new ArrayList<>();

    private final ItemStack woodStack;
    private final ItemStack axeStack;
    private final ItemStack rewardStack;

    private int lastImpactHandled = -1;
    private boolean finaleBurstDone = false;
    private long skipToMs = -1;

    public TreeGiftScreen(GiftResult result) {
        super(Component.literal("Tree Gift"));
        this.result = result;
        this.woodStack = new ItemStack(Items.OAK_LOG);
        this.axeStack = new ItemStack(Items.IRON_AXE);
        this.rewardStack = new ItemStack(itemFor(result.loot.itemId));
        this.rewardStack.setCount(Math.max(1, Math.min(64, result.loot.amount)));
    }

    // ────────────────────────────────────────────────────────────── lifecycle

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private long elapsed() {
        long e = now() - startMs;
        return skipToMs >= 0 ? Math.max(e, skipToMs) : e;
    }

    private long beatDur(int i) {
        return result.steps.get(i).type == GiftStep.Type.MANGO ? MANGO_BEAT_MS : BEAT_MS;
    }

    private long beatStart(int i) {
        long t = 0;
        for (int k = 0; k < i; k++) t += beatDur(k);
        return t;
    }

    private long totalBeatsMs() {
        return beatStart(result.steps.size());
    }

    private long finaleStartMs() {
        return INTRO_MS + totalBeatsMs();
    }

    private long absImpact(int i) {
        long within = result.steps.get(i).type == GiftStep.Type.MANGO ? MANGO_TWIST_AT : IMPACT_AT;
        return INTRO_MS + beatStart(i) + within;
    }

    // ────────────────────────────────────────────────────────────── input

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) return super.keyPressed(event); // Esc closes normally
        advanceOrClose();
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        advanceOrClose();
        return true;
    }

    /** Before the finale: skip straight to the reward. During it: collect & close. */
    private void advanceOrClose() {
        if (elapsed() < finaleStartMs()) {
            skipToMs = finaleStartMs();
            lastImpactHandled = result.steps.size() - 1; // don't machine-gun skipped sounds
            playSound(SoundEvents.ITEM_PICKUP, 1.4f);
        } else if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    // ────────────────────────────────────────────────────────────── render

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        long e = elapsed();
        long finaleStart = finaleStartMs();
        boolean finale = e >= finaleStart;

        fireImpactsUpTo(e);
        updateParticles();

        // Dim backdrop (drawn ourselves so it's independent of the base screen).
        g.fill(0, 0, width, height, 0xC8000000);
        g.fillGradient(0, 0, width, height, 0x00000000, 0x66000000);

        int cx = width / 2;
        int cy = height / 2 - 6;

        Matrix3x2fStack pose = g.pose();
        float shake = shakeAt(e);
        pose.pushMatrix();
        if (shake > 0.01f) {
            pose.translate((rng.nextFloat() - 0.5f) * shake, (rng.nextFloat() - 0.5f) * shake);
        }

        if (finale) {
            renderFinale(g, cx, cy, e - finaleStart);
        } else if (e < INTRO_MS) {
            renderIntro(g, cx, cy, e / (float) INTRO_MS);
        } else {
            renderBeats(g, cx, cy, e - INTRO_MS);
        }

        renderParticles(g);
        pose.popMatrix();

        renderHud(g, e, finale);
    }

    private void renderIntro(GuiGraphicsExtractor g, int cx, int cy, float t) {
        float s = easeOutBack(clamp01(t));
        drawGlowDisc(g, cx, cy, (int) (46 * s), 0x33FFFFFF);
        drawWood(g, cx, cy, WOOD_SCALE * s, 0, true, null, 0f);
        if (rng.nextInt(3) == 0) {
            spawn(cx + rand(-40, 40), cy + rand(-30, 30), rand(-10, 10), rand(-30, -5),
                    0xFFFFF0A0, 500 + rng.nextInt(300), 2f);
        }
    }

    private void renderBeats(GuiGraphicsExtractor g, int cx, int cy, long t) {
        int b = 0;
        long acc = 0;
        while (b < result.steps.size() - 1 && t >= acc + beatDur(b)) {
            acc += beatDur(b);
            b++;
        }
        long local = t - acc;
        GiftStep step = result.steps.get(b);

        long within = step.type == GiftStep.Type.MANGO ? MANGO_TWIST_AT : IMPACT_AT;
        boolean landed = local >= within;
        int impactsDone = b + (landed ? 1 : 0);
        Rarity shown = impactsDone == 0 ? null : result.steps.get(impactsDone - 1).rarity;
        int cracks = Math.max(0, impactsDone - 1) + (landed && step.type != GiftStep.Type.MANGO ? 1 : 0);

        int glow = shown == null ? 0x33FFFFFF : withAlpha(shown.color, 0.28f);
        drawGlowDisc(g, cx, cy, 48 + (shown == null ? 0 : shown.ordinal() * 5), glow);

        if (step.type == GiftStep.Type.MANGO) {
            renderSpin(g, cx, cy, local);
            return;
        }

        boolean terminalCrack = step.type == GiftStep.Type.CRACK;
        if (landed && terminalCrack) {
            float sp = easeOutCubic(clamp01((local - within) / (float) (beatDur(b) - within)));
            drawSplitWood(g, cx, cy, shown, cracks, sp);
        } else {
            float punch = landed ? 1f + 0.18f * (1f - clamp01((local - within) / 130f)) : 1f;
            drawWood(g, cx, cy, WOOD_SCALE * punch, cracks, false, shown, landed ? flashAt(local - within) : 0f);
        }

        if (!landed) {
            float f = clamp01(local / (float) within);
            drawFlyingAxe(g, cx, cy, f, b);
        }
    }

    private void renderSpin(GuiGraphicsExtractor g, int cx, int cy, long local) {
        if (local < MANGO_TWIST_AT) {
            float p = clamp01(local / (float) MANGO_TWIST_AT);
            float spins = 2f + easeInCubic(p) * 10f;
            float angleDeg = spins * 360f * p;
            float scale = WOOD_SCALE * (1f + 0.15f * (float) Math.sin(p * Math.PI * 6));
            drawGlowDisc(g, cx, cy, (int) (54 + 30 * p), withAlpha(LootTables.MANGO_COLOR, 0.35f + 0.4f * p));
            drawWoodRotated(g, cx, cy, scale, angleDeg);
            if (rng.nextInt(2) == 0) {
                spawn(cx + rand(-30, 30), cy + rand(-30, 30), rand(-40, 40), rand(-60, 20),
                        LootTables.MANGO_COLOR, 400, 2.5f);
            }
        } else {
            float p = clamp01((local - MANGO_TWIST_AT) / (float) (MANGO_BEAT_MS - MANGO_TWIST_AT));
            drawGlowDisc(g, cx, cy, (int) (84 - 20 * p), withAlpha(LootTables.MANGO_COLOR, 0.6f * (1f - p)));
            drawBigItem(g, rewardStack, cx, cy, WOOD_SCALE * easeOutBack(p));
        }
    }

    private void renderFinale(GuiGraphicsExtractor g, int cx, int cy, long local) {
        int color = result.loot.color;
        float in = easeOutBack(clamp01(local / 400f));

        drawRays(g, cx, cy, 30, 150, 16, withAlpha(color, 0.22f), local * 0.03f);
        drawRays(g, cx, cy, 20, 110, 16, withAlpha(color, 0.30f), -local * 0.05f + 11);
        drawGlowDisc(g, cx, cy, 60, withAlpha(color, 0.30f));
        drawBigItem(g, rewardStack, cx, cy - 4, WOOD_SCALE * in);

        if (!finaleBurstDone) {
            finaleBurstDone = true;
            playSound(result.mango ? SoundEvents.TOTEM_USE : SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f);
            for (int i = 0; i < 60; i++) {
                spawn(cx, cy, rand(-120, 120), rand(-160, -20), confetti(color), 900 + rng.nextInt(700), 3f);
            }
        }
        if (rng.nextInt(2) == 0) {
            spawn(rand(0, width), -6, rand(-15, 15), rand(20, 70), confetti(color), 1400, 3f);
        }
    }

    // ────────────────────────────────────────────────────────────── HUD text

    private void renderHud(GuiGraphicsExtractor g, long e, boolean finale) {
        int top = Math.max(14, height / 2 - 118);
        g.centeredText(font, "TREE GIFT", width / 2, top, 0xFFFFD24A);

        if (finale) {
            String name = result.loot.name + (rewardStack.getCount() > 1 ? " x" + result.loot.amount : "");
            g.centeredText(font, name, width / 2, height / 2 + 60, result.loot.color);
            g.centeredText(font, result.loot.flavor, width / 2, height / 2 + 74, 0xFFBBBBBB);
            String tier = result.mango ? "* MANGO DYE *" : result.finalRarity.display;
            g.centeredText(font, tier, width / 2, height / 2 + 90,
                    result.mango ? LootTables.MANGO_COLOR : result.finalRarity.color);
            if ((e / 500) % 2 == 0) {
                g.centeredText(font, "Click or press a key to collect", width / 2, height / 2 + 108, 0xFFFFF055);
            }
        } else {
            Rarity shown = currentShownRarity(e);
            String banner = shown == null ? "Wrapped Hardened Wood..."
                    : shown.display + " Hardened Wood";
            g.centeredText(font, banner, width / 2, height / 2 + 66, shown == null ? 0xFFAAAAAA : shown.color);
            g.centeredText(font, "click to skip", width / 2, height - 24, 0xFF888888);
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

    private void fireImpactsUpTo(long e) {
        int cx = width / 2, cy = height / 2 - 6;
        for (int b = lastImpactHandled + 1; b < result.steps.size(); b++) {
            if (e < absImpact(b)) break;
            lastImpactHandled = b;
            GiftStep step = result.steps.get(b);
            switch (step.type) {
                case REVEAL:
                    playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f);
                    burst(cx, cy, 14, step.rarity.color, 60);
                    break;
                case UPGRADE:
                    playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f + step.rarity.ordinal() * 0.22f);
                    burst(cx, cy, 18, step.rarity.color, 80);
                    break;
                case CRACK:
                    playSound(SoundEvents.ITEM_PICKUP, 0.9f);
                    playSound(SoundEvents.PLAYER_LEVELUP, 1.1f);
                    burst(cx, cy, 40, step.rarity.color, 130);
                    break;
                case MANGO:
                    playSound(SoundEvents.ENDER_DRAGON_GROWL, 1.4f);
                    burst(cx, cy, 70, LootTables.MANGO_COLOR, 170);
                    break;
            }
        }
    }

    private void playSound(SoundEvent event, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.getSoundManager() != null) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(event, pitch));
        }
    }

    // ────────────────────────────────────────────────────────── drawing helpers

    private void drawWood(GuiGraphicsExtractor g, int cx, int cy, float scale, int cracks,
                          boolean wrapped, Rarity rarity, float flash) {
        if (rarity != null) {
            int r = (int) (16 * scale / 2f) + 6;
            drawGlowDisc(g, cx, cy, r, withAlpha(rarity.color, 0.5f));
        }
        drawBigItem(g, woodStack, cx, cy, scale);
        drawCracks(g, cx, cy, (int) (16 * scale), cracks);
        if (wrapped) {
            g.centeredText(font, "?", cx, cy - 4, 0xFFFFFFFF);
        }
        if (flash > 0.01f) {
            int r = (int) (16 * scale / 2f) + 4;
            drawGlowDisc(g, cx, cy, r, withAlpha(0xFFFFFFFF, 0.6f * flash));
        }
    }

    private void drawWoodRotated(GuiGraphicsExtractor g, int cx, int cy, float scale, float angleDeg) {
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(cx, cy);
        pose.rotate((float) Math.toRadians(angleDeg));
        pose.scale(scale, scale);
        pose.translate(-8, -8);
        g.item(woodStack, 0, 0);
        pose.popMatrix();
    }

    private void drawSplitWood(GuiGraphicsExtractor g, int cx, int cy, Rarity rarity, int cracks, float sp) {
        int off = (int) (sp * 34);
        drawGlowDisc(g, cx, cy, 40, withAlpha(rarity == null ? 0xFFFFFFFF : rarity.color, 0.4f * (1f - sp)));
        drawBigItem(g, woodStack, cx - off, cy - (int) (sp * 8), WOOD_SCALE * (1f - 0.3f * sp));
        drawBigItem(g, woodStack, cx + off, cy + (int) (sp * 8), WOOD_SCALE * (1f - 0.3f * sp));
        if (sp > 0.35f) {
            drawBigItem(g, rewardStack, cx, cy, WOOD_SCALE * easeOutBack(clamp01((sp - 0.35f) / 0.65f)) * 0.9f);
        }
    }

    private void drawFlyingAxe(GuiGraphicsExtractor g, int cx, int cy, float f, int beat) {
        boolean fromLeft = (beat % 2) == 0;
        float startX = fromLeft ? -30 : width + 30;
        float x = lerp(startX, cx, easeOutCubic(f));
        float y = lerp(cy - 70, cy, f) + (float) Math.sin(f * Math.PI) * -18f;
        float spinDeg = (fromLeft ? 1 : -1) * f * 540f;
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.rotate((float) Math.toRadians(spinDeg));
        pose.scale(2.2f, 2.2f);
        pose.translate(-8, -8);
        g.item(axeStack, 0, 0);
        pose.popMatrix();
    }

    private void drawBigItem(GuiGraphicsExtractor g, ItemStack stack, int cx, int cy, float scale) {
        if (scale <= 0.01f) return;
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(cx, cy);
        pose.scale(scale, scale);
        pose.translate(-8, -8);
        g.item(stack, 0, 0);
        pose.popMatrix();
    }

    private void drawCracks(GuiGraphicsExtractor g, int cx, int cy, int size, int count) {
        if (count <= 0) return;
        Random cr = new Random(0xC0FFEEL + count * 31L);
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
                thickLine(g, px, py, nx, ny, 0xCC201008);
                px = nx; py = ny;
            }
        }
    }

    private void drawGlowDisc(GuiGraphicsExtractor g, int cx, int cy, int radius, int color) {
        if (radius <= 0) return;
        for (int dy = -radius; dy <= radius; dy++) {
            int dx = (int) Math.sqrt((double) radius * radius - dy * dy);
            g.fill(cx - dx, cy + dy, cx + dx, cy + dy + 1, color);
        }
    }

    private void drawRays(GuiGraphicsExtractor g, int cx, int cy, int inner, int outer, int count, int color, float rotDeg) {
        Matrix3x2fStack pose = g.pose();
        for (int i = 0; i < count; i++) {
            float a = rotDeg + i * (360f / count);
            pose.pushMatrix();
            pose.translate(cx, cy);
            pose.rotate((float) Math.toRadians(a));
            g.fill(inner, -2, outer, 2, color);
            pose.popMatrix();
        }
    }

    private void thickLine(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1)) + 1;
        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / steps;
            int y = y1 + (y2 - y1) * i / steps;
            g.fill(x, y, x + 2, y + 2, color);
        }
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

    private void renderParticles(GuiGraphicsExtractor g) {
        long t = now();
        for (Particle p : particles) {
            double age = (t - p.spawn) / 1000.0;
            double px = p.x + p.vx * age;
            double py = p.y + p.vy * age + 0.5 * 320.0 * age * age;
            float lifeFrac = 1f - clamp01((t - p.spawn) / (float) p.life);
            int color = withAlpha(p.color, 0.15f + 0.85f * lifeFrac);
            int s = Math.max(1, (int) (p.size * (0.5f + lifeFrac)));
            g.fill((int) px, (int) py, (int) px + s, (int) py + s, color);
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

    private static int withAlpha(int argb, float a) {
        int alpha = Math.max(0, Math.min(255, (int) (a * 255)));
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    /** Map a loot table's vanilla item id to an {@link ItemLike} (render stand-in). */
    private static ItemLike itemFor(String id) {
        switch (id) {
            case "minecraft:oak_planks":       return Items.OAK_PLANKS;
            case "minecraft:stripped_oak_log": return Items.STRIPPED_OAK_LOG;
            case "minecraft:wheat":            return Items.WHEAT;
            case "minecraft:bread":            return Items.BREAD;
            case "minecraft:jungle_sapling":   return Items.JUNGLE_SAPLING;
            case "minecraft:oak_sapling":      return Items.OAK_SAPLING;
            case "minecraft:enchanted_book":   return Items.ENCHANTED_BOOK;
            case "minecraft:emerald":          return Items.EMERALD;
            case "minecraft:diamond_axe":      return Items.DIAMOND_AXE;
            case "minecraft:iron_axe":         return Items.IRON_AXE;
            case "minecraft:gold_ingot":       return Items.GOLD_INGOT;
            case "minecraft:tripwire_hook":    return Items.TRIPWIRE_HOOK;
            case "minecraft:cocoa_beans":      return Items.COCOA_BEANS;
            case "minecraft:orange_dye":       return Items.ORANGE_DYE;
            case "minecraft:oak_log":
            default:                           return Items.OAK_LOG;
        }
    }
}
