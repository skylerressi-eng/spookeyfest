package com.treegifts.client;

import com.treegifts.core.Rarity;
import com.treegifts.core.TreeGiftConfig;
import com.treegifts.core.TreeGiftResult;
import com.treegifts.core.TreeGiftStats;

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
 * The REAL Tree Gift reveal (Minecraft 26.1.2 GUI system).
 *
 * A Hardened Wood target drops in; three iron axes fly in from varied directions,
 * spinning, and thud into it with shake + particles; the wood cracks and breaks;
 * then the ACTUAL drop Hypixel gave you — {@link TreeGiftResult}, parsed from chat
 * — bursts out with rarity-scaled glow, rays, and confetti.
 *
 * The result is locked: this screen only decides HOW the real drop is revealed,
 * never WHAT it is. Higher rarities get a bigger reveal. It auto-closes so it
 * doesn't interrupt foraging for long, and the queue advances to the next drop.
 *
 * 26.x rendering: draws via GuiGraphicsExtractor; transforms via the JOML
 * Matrix3x2fStack from pose() (rotations in radians).
 */
public class TreeGiftScreen extends Screen {

    // --- timeline (ms) ---
    private static final long INTRO_MS   = 650;   // Hardened Wood drops in
    private static final int  AXES       = 3;     // iron axes thrown
    private static final long STAGGER_MS = 520;   // gap between axe throws
    private static final long FLIGHT_MS  = 300;   // time from launch to impact
    private static final long BREAK_MS   = 150;   // shake before the wood breaks
    private static final long SPLIT_MS   = 280;   // wood splitting open
    private static final long AUTO_CLOSE_AFTER_REVEAL_MS = 4200;

    private static final int WOOD_SCALE = 6;

    private final TreeGiftResult result;
    private final long startMs = now();
    private final Random rng = new Random();
    private final List<Particle> particles = new ArrayList<>();

    private final ItemStack woodStack;   // the Hardened Wood target
    private final ItemStack axeStack;     // iron axe
    private final ItemStack rewardStack;  // the REAL drop's icon

    private int lastImpactHandled = -1;
    private boolean breakSoundDone = false;
    private boolean finaleBurstDone = false;
    private long skipToMs = -1;
    private boolean closed = false;

    // Config-driven feel.
    private final double animSpeed;
    private final double particleScale;
    private final boolean sounds;
    private final long giftNumber;

    public TreeGiftScreen(TreeGiftResult result) {
        super(Component.literal("Tree Gift"));
        this.result = result;
        this.woodStack = new ItemStack(Items.OAK_LOG); // Hardened Wood stand-in
        this.axeStack = new ItemStack(Items.IRON_AXE);
        this.rewardStack = new ItemStack(itemFor(result.iconItemId));
        if (result.hasAmount()) rewardStack.setCount(Math.max(1, Math.min(64, result.amount)));
        TreeGiftConfig cfg = TreeGiftConfig.INSTANCE;
        this.animSpeed = cfg.animationSpeed <= 0 ? 1.0 : cfg.animationSpeed;
        this.particleScale = cfg.particleScale;
        this.sounds = cfg.playSounds;
        this.giftNumber = TreeGiftStats.INSTANCE.total();
    }

    // ────────────────────────────────────────────── timeline helpers

    @Override public boolean isPauseScreen() { return false; }

    private static long now() { return System.currentTimeMillis(); }

    private long elapsed() {
        long e = (long) ((now() - startMs) * animSpeed); // animationSpeed scales the whole reveal
        return skipToMs >= 0 ? Math.max(e, skipToMs) : e;
    }

    private long lastImpactMs() { return INTRO_MS + (AXES - 1) * STAGGER_MS + FLIGHT_MS; }
    private long breakMs()      { return lastImpactMs() + BREAK_MS; }
    private long revealMs()     { return breakMs() + SPLIT_MS; }
    private long autoCloseMs()  { return revealMs() + AUTO_CLOSE_AFTER_REVEAL_MS; }

    private long axeLaunch(int i) { return INTRO_MS + (long) i * STAGGER_MS; }
    private long axeImpact(int i) { return axeLaunch(i) + FLIGHT_MS; }

    // ────────────────────────────────────────────── input

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) { close(); return true; } // Esc
        advanceOrClose();
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        advanceOrClose();
        return true;
    }

    private void advanceOrClose() {
        if (elapsed() < revealMs()) {
            skipToMs = revealMs();
            lastImpactHandled = AXES; // suppress skipped impact sounds
        } else {
            close();
        }
    }

    private void close() {
        if (closed) return;
        closed = true;
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public void removed() {
        // However the screen goes away (auto-close, Esc, click), advance the queue.
        RealTreeGiftReveal.INSTANCE.onRevealClosed();
    }

    // ────────────────────────────────────────────── render

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        long e = elapsed();
        if (e >= autoCloseMs()) { close(); return; }

        fireImpactsUpTo(e);
        maybeBreak(e);
        updateParticles();

        g.fill(0, 0, width, height, 0xC8000000);
        g.fillGradient(0, 0, width, height, 0x00000000, 0x66000000);

        int cx = width / 2;
        int cy = height / 2 - 6;

        Matrix3x2fStack pose = g.pose();
        float shake = shakeAt(e);
        pose.pushMatrix();
        if (shake > 0.01f) pose.translate((rng.nextFloat() - 0.5f) * shake, (rng.nextFloat() - 0.5f) * shake);

        boolean reveal = e >= breakMs();
        if (reveal) {
            renderReveal(g, cx, cy, e - breakMs());
        } else if (e < INTRO_MS) {
            renderIntro(g, cx, cy, e / (float) INTRO_MS);
        } else {
            renderAxePhase(g, cx, cy, e);
        }

        renderParticles(g);
        pose.popMatrix();

        drawFlash(g, e);
        renderHud(g, e, reveal);
    }

    /** A bright full-screen flash the instant a Legendary+ wood breaks open. */
    private void drawFlash(GuiGraphicsExtractor g, long e) {
        if (result.rarity.ordinal() < Rarity.LEGENDARY.ordinal()) return;
        long since = e - breakMs();
        if (since < 0 || since > 300) return;
        float a = 0.75f * (1f - since / 300f);
        g.fill(0, 0, width, height, withAlpha(0xFFFFFFFF, a));
    }

    private void renderIntro(GuiGraphicsExtractor g, int cx, int cy, float t) {
        float s = easeOutBack(clamp01(t));
        drawGlowDisc(g, cx, cy, (int) (46 * s), 0x33FFFFFF);
        drawBigItem(g, woodStack, cx, cy, WOOD_SCALE * s);
    }

    /** Hardened Wood sits center; iron axes fly in and thud, cracks accumulate. */
    private void renderAxePhase(GuiGraphicsExtractor g, int cx, int cy, long e) {
        int landed = 0;
        for (int i = 0; i < AXES; i++) if (e >= axeImpact(i)) landed++;

        drawGlowDisc(g, cx, cy, 44, 0x22FFFFFF);
        // Impact punch on the wood right after the latest hit.
        float punch = 1f;
        for (int i = 0; i < AXES; i++) {
            long since = e - axeImpact(i);
            if (since >= 0 && since < 130) punch = Math.max(punch, 1f + 0.18f * (1f - since / 130f));
        }
        drawBigItem(g, woodStack, cx, cy, WOOD_SCALE * punch);
        drawCracks(g, cx, cy, (int) (16 * WOOD_SCALE), landed);

        // Any axe currently in flight.
        for (int i = 0; i < AXES; i++) {
            long since = e - axeLaunch(i);
            if (since >= 0 && since < FLIGHT_MS) {
                drawFlyingAxe(g, cx, cy, since / (float) FLIGHT_MS, i);
            }
        }
    }

    /** Wood splits; the REAL drop scales up under rarity-coloured rays + confetti. */
    private void renderReveal(GuiGraphicsExtractor g, int cx, int cy, long local) {
        int color = result.rarity.color;
        float drama = 0.35f + 0.65f * result.rarity.drama();

        if (local < SPLIT_MS) {
            float sp = easeOutCubic(clamp01(local / (float) SPLIT_MS));
            int off = (int) (sp * 34);
            drawGlowDisc(g, cx, cy, (int) (40 * (1 - sp) + 30), withAlpha(color, 0.4f * (1f - sp)));
            drawBigItem(g, woodStack, cx - off, cy - (int) (sp * 8), WOOD_SCALE * (1f - 0.3f * sp));
            drawBigItem(g, woodStack, cx + off, cy + (int) (sp * 8), WOOD_SCALE * (1f - 0.3f * sp));
            if (sp > 0.35f) {
                drawBigItem(g, rewardStack, cx, cy, WOOD_SCALE * easeOutBack(clamp01((sp - 0.35f) / 0.65f)) * 0.9f);
            }
            return;
        }

        long t = local - SPLIT_MS;
        float in = easeOutBack(clamp01(t / 380f));
        int rays = (int) (10 + 12 * drama);
        drawRays(g, cx, cy, 30, (int) (110 + 70 * drama), rays, withAlpha(color, 0.22f), t * 0.03f);
        drawRays(g, cx, cy, 20, (int) (80 + 50 * drama), rays, withAlpha(color, 0.30f), -t * 0.05f + 11);
        drawGlowDisc(g, cx, cy, (int) (46 + 34 * drama), withAlpha(color, 0.28f + 0.22f * drama));
        drawBigItem(g, rewardStack, cx, cy - 4, WOOD_SCALE * in);

        if (!finaleBurstDone) {
            finaleBurstDone = true;
            playFinaleSound();
            int n = (int) ((25 + 90 * drama) * particleScale);
            for (int i = 0; i < n; i++) {
                spawn(cx, cy, rand(-120, 120), rand(-170, -20), confetti(color), 900 + rng.nextInt(800), 3f);
            }
        }
        // Ongoing confetti drizzle for the higher rarities.
        if (result.rarity.ordinal() >= Rarity.RARE.ordinal() && rng.nextInt(2) == 0) {
            spawn(rand(0, width), -6, rand(-15, 15), rand(20, 70), confetti(color), 1400, 3f);
        }
    }

    // ────────────────────────────────────────────── HUD text

    private void renderHud(GuiGraphicsExtractor g, long e, boolean reveal) {
        int top = Math.max(14, height / 2 - 118);
        g.centeredText(font, "TREE GIFT", width / 2, top, 0xFFFFD24A);
        if (!result.treeType.isEmpty()) {
            g.centeredText(font, result.treeType + " Tree", width / 2, top + 12, 0xFF9BE8A0);
        }
        if (giftNumber > 0) {
            g.centeredText(font, "Gift #" + giftNumber, width / 2, top + 24, 0xFF6D6D6D);
        }

        if (reveal && e >= breakMs() + SPLIT_MS) {
            String name = result.itemName + (result.hasAmount() ? " x" + result.amount : "");
            g.centeredText(font, name, width / 2, height / 2 + 60, result.rarity.color);
            g.centeredText(font, result.rarity.display, width / 2, height / 2 + 74, result.rarity.color);
            if (result.hasDropChance()) {
                g.centeredText(font, trimPct(result.dropChancePercent) + "% drop", width / 2, height / 2 + 88, 0xFF9AA0A6);
            }
            if ((e / 500) % 2 == 0) {
                g.centeredText(font, "Click to dismiss", width / 2, height / 2 + 106, 0xFFCFCFCF);
            }
        } else {
            g.centeredText(font, "click to skip", width / 2, height - 24, 0xFF888888);
        }
    }

    // ────────────────────────────────────────────── impacts / sound

    private void fireImpactsUpTo(long e) {
        int cx = width / 2, cy = height / 2 - 6;
        for (int i = lastImpactHandled + 1; i < AXES; i++) {
            if (e < axeImpact(i)) break;
            lastImpactHandled = i;
            playSound(SoundEvents.ITEM_PICKUP, 0.8f + i * 0.15f);
            burst(cx, cy, 16 + i * 6, 0xFFC9A66B, 80);
        }
    }

    private void maybeBreak(long e) {
        if (!breakSoundDone && e >= breakMs()) {
            breakSoundDone = true;
            int cx = width / 2, cy = height / 2 - 6;
            playSound(SoundEvents.PLAYER_LEVELUP, 1.2f);
            burst(cx, cy, 30, 0xFFC9A66B, 120);
        }
    }

    private void playFinaleSound() {
        boolean big = result.rarity.ordinal() >= Rarity.LEGENDARY.ordinal();
        playSound(big ? SoundEvents.ENDER_DRAGON_GROWL : SoundEvents.AMETHYST_BLOCK_CHIME, big ? 1.0f : 1.3f);
        playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f);
    }

    private void playSound(SoundEvent event, float pitch) {
        if (!sounds) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.getSoundManager() != null) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(event, pitch));
        }
    }

    // ────────────────────────────────────────────── drawing helpers

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

    private void drawFlyingAxe(GuiGraphicsExtractor g, int cx, int cy, float f, int i) {
        // Each axe comes from a slightly different direction and arc.
        boolean fromLeft = (i % 2) == 0;
        float startX = fromLeft ? -30 : width + 30;
        float startY = cy - 80 + i * 26;
        float x = lerp(startX, cx, easeOutCubic(f));
        float y = lerp(startY, cy, f) + (float) Math.sin(f * Math.PI) * -16f;
        float spinDeg = (fromLeft ? 1 : -1) * f * (500f + i * 60f);
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.rotate((float) Math.toRadians(spinDeg));
        pose.scale(2.2f, 2.2f);
        pose.translate(-8, -8);
        g.item(axeStack, 0, 0);
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
            for (int s = 0; s < 4; s++) {
                double aa = a + (cr.nextDouble() - 0.5) * 0.6;
                int nx = px + (int) (Math.cos(aa) * len / 4);
                int ny = py + (int) (Math.sin(aa) * len / 4);
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

    // ────────────────────────────────────────────── particles

    private static final class Particle {
        double x, y, vx, vy; long spawn, life; int color; float size;
    }

    private void spawn(double x, double y, double vx, double vy, int color, long life, float size) {
        Particle p = new Particle();
        p.x = x; p.y = y; p.vx = vx; p.vy = vy;
        p.spawn = now(); p.life = life; p.color = color; p.size = size;
        particles.add(p);
    }

    private void burst(int cx, int cy, int n, int color, double speed) {
        int count = Math.max(1, (int) Math.round(n * particleScale));
        for (int i = 0; i < count; i++) {
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

    // ────────────────────────────────────────────── math / util

    private float shakeAt(long e) {
        float max = 0f;
        for (int i = 0; i < AXES; i++) {
            long since = e - axeImpact(i);
            if (since >= 0 && since < 200) max = Math.max(max, 5f * (1f - since / 200f));
        }
        long sinceBreak = e - breakMs();
        if (sinceBreak >= 0 && sinceBreak < 220) {
            max = Math.max(max, (6f + 5f * result.rarity.drama()) * (1f - sinceBreak / 220f));
        }
        return max;
    }

    private int confetti(int base) {
        int[] palette = {0xFFFF5555, 0xFF55FF55, 0xFF5599FF, 0xFFFFD24A, 0xFFFF7BE0, base | 0xFF000000};
        return palette[rng.nextInt(palette.length)];
    }

    private double rand(double lo, double hi) { return lo + rng.nextDouble() * (hi - lo); }
    private static float clamp01(float t) { return t < 0 ? 0 : (t > 1 ? 1 : t); }
    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    private static float easeOutCubic(float t) { float u = 1 - t; return 1 - u * u * u; }
    private static float easeOutBack(float t) {
        float c1 = 1.70158f, c3 = c1 + 1;
        float u = t - 1;
        return 1 + c3 * u * u * u + c1 * u * u;
    }

    private static int withAlpha(int argb, float a) {
        int alpha = Math.max(0, Math.min(255, (int) (a * 255)));
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    private static String trimPct(double d) {
        if (d == Math.floor(d)) return String.valueOf((long) d);
        return String.valueOf(d);
    }

    /** Vanilla render stand-in for a loot icon id (presentation only). */
    private static ItemLike itemFor(String id) {
        if (id == null) return Items.OAK_LOG;
        switch (id) {
            case "minecraft:oak_planks":       return Items.OAK_PLANKS;
            case "minecraft:stripped_oak_log": return Items.STRIPPED_OAK_LOG;
            case "minecraft:lime_dye":         return Items.LIME_DYE;
            case "minecraft:orange_dye":       return Items.ORANGE_DYE;
            case "minecraft:cyan_dye":         return Items.CYAN_DYE;
            case "minecraft:experience_bottle":return Items.EXPERIENCE_BOTTLE;
            case "minecraft:nether_star":      return Items.NETHER_STAR;
            case "minecraft:slime_ball":       return Items.SLIME_BALL;
            case "minecraft:hanging_roots":    return Items.HANGING_ROOTS;
            case "minecraft:stick":            return Items.STICK;
            case "minecraft:enchanted_book":   return Items.ENCHANTED_BOOK;
            case "minecraft:paper":            return Items.PAPER;
            case "minecraft:redstone":         return Items.REDSTONE;
            case "minecraft:golden_apple":     return Items.GOLDEN_APPLE;
            case "minecraft:phantom_membrane": return Items.PHANTOM_MEMBRANE;
            case "minecraft:cod":              return Items.COD;
            case "minecraft:prismarine_shard": return Items.PRISMARINE_SHARD;
            case "minecraft:feather":          return Items.FEATHER;
            case "minecraft:honey_bottle":     return Items.HONEY_BOTTLE;
            case "minecraft:blaze_powder":     return Items.BLAZE_POWDER;
            case "minecraft:dirt":             return Items.DIRT;
            case "minecraft:dead_bush":        return Items.DEAD_BUSH;
            case "minecraft:snowball":         return Items.SNOWBALL;
            case "minecraft:bone":             return Items.BONE;
            case "minecraft:iron_axe":         return Items.IRON_AXE;
            case "minecraft:oak_log":
            default:                           return Items.OAK_LOG;
        }
    }
}
