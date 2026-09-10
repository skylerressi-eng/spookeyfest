package com.spookybirch.dragon.gui;

import java.io.IOException;
import java.util.List;

import com.spookybirch.core.SpookyConfig;
import com.spookybirch.dragon.data.DragonLoot;
import com.spookybirch.dragon.data.DragonLootData;
import com.spookybirch.dragon.data.DragonType;
import com.spookybirch.dragon.data.DragonWeight;
import com.spookybirch.dragon.data.LootEntry;
import com.spookybirch.dragon.sim.AnimationState;
import com.spookybirch.dragon.sim.Easing;
import com.spookybirch.dragon.sim.RouletteAnimator;
import com.spookybirch.dragon.sim.RouletteResult;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.input.Keyboard;

/**
 * The Dragon Altar Roulette — a purely cosmetic, simulation-only visualisation
 * of Hypixel SkyBlock Ender Dragon loot. It never reads or changes the player's
 * inventory, never touches the economy, never automates gameplay, and always
 * labels its outcome as a visual simulation.
 *
 * Controls:
 * <pre>
 *   SPACE  spin            R      replay
 *   ← / →  change dragon   ESC    close
 *   [ / ]  eyes −/+        - / =  damage rank −/+     F  toggle final blow
 * </pre>
 */
public class DragonRouletteScreen extends GuiScreen {

    private static final int GOLD = 0xFFFFC531;
    private static final int PANEL_BG = 0xC80C0814;
    private static final int PANEL_BORDER = 0x66FFE4A8;
    private static final int TEXT = 0xFFE7E2F0;
    private static final int DIM = 0xFF8C86A0;

    private final RouletteAnimator animator = new RouletteAnimator();
    private final WheelRenderer wheel = new WheelRenderer();
    private final DragonWeight weight = new DragonWeight();

    private DragonType[] dragons;
    private int dragonIndex;
    private DragonLoot loot;
    private final DragonType initialType;

    private AnimationState prevState = AnimationState.IDLE;

    public DragonRouletteScreen() {
        this(DragonType.SUPERIOR);
    }

    public DragonRouletteScreen(DragonType initialType) {
        this.initialType = (initialType != null) ? initialType : DragonType.SUPERIOR;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false; // keep animation ticking off the render thread clock
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        // All eight types are selectable so players can browse Holy too; the
        // simulator refuses to "summon" Holy (see handling in trySpin()).
        dragons = DragonType.values();
        if (loot == null) {
            selectDragon(initialType.ordinal());
        } else {
            wheel.setLoot(loot);
        }
        applyConfig();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private void applyConfig() {
        SpookyConfig cfg = SpookyConfig.INSTANCE;
        animator.setAnimSpeed(cfg.dragonAnimSpeed);
        animator.setReducedMotion(cfg.dragonReducedMotion);
    }

    private void selectDragon(int index) {
        int count = dragons.length;
        dragonIndex = ((index % count) + count) % count;
        loot = DragonLootData.pool(dragons[dragonIndex]);
        wheel.setLoot(loot);
        animator.reset();
    }

    private DragonType dragon() {
        return dragons[dragonIndex];
    }

    private void trySpin(Long seed) {
        if (animator.state().spinning()) return;
        if (!dragon().summonable) {
            // Holy is a reference pool, not a real fight — don't pretend to spin it.
            return;
        }
        applyConfig();
        animator.startSpin(loot, seed);
    }

    // ---- input ------------------------------------------------------------

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        switch (keyCode) {
            case Keyboard.KEY_SPACE:
                trySpin(null);
                return;
            case Keyboard.KEY_R:
                if (!animator.state().spinning()) trySpin(null);
                return;
            case Keyboard.KEY_LEFT:
                if (!animator.state().spinning()) selectDragon(dragonIndex - 1);
                return;
            case Keyboard.KEY_RIGHT:
                if (!animator.state().spinning()) selectDragon(dragonIndex + 1);
                return;
            case Keyboard.KEY_LBRACKET:
                weight.addEyes(-1); return;
            case Keyboard.KEY_RBRACKET:
                weight.addEyes(1); return;
            case Keyboard.KEY_MINUS:
                weight.addRank(1); return;   // worse rank = lower weight
            case Keyboard.KEY_EQUALS:
                weight.addRank(-1); return;  // better rank = higher weight
            case Keyboard.KEY_F:
                weight.toggleFinalBlow(); return;
            default:
                super.keyTyped(typedChar, keyCode); // ESC closes
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0 && !animator.state().spinning()) {
            trySpin(null); // click the wheel to spin too
        }
    }

    // ---- render -----------------------------------------------------------

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        animator.update(System.nanoTime());
        AnimationState state = animator.state();

        // Fire the reveal sound / effects exactly once on entry.
        if (state == AnimationState.RESULT_REVEAL && prevState != AnimationState.RESULT_REVEAL) {
            playRevealSound();
        }
        prevState = state;

        // Dark fantasy backdrop.
        drawRect(0, 0, width, height, 0xF00A0713);

        SpookyConfig cfg = SpookyConfig.INSTANCE;
        double base = Math.min(width, height);
        double radius = base * 0.30 * clampScale(cfg.dragonWheelScale);
        if (radius < 60) radius = 60;
        double cx = width / 2.0;
        double cy = height * 0.54;

        // A rare-reveal UI pulse: a brief accent flash over the whole screen.
        if (cfg.dragonRareEffects && state.showingResult() && animator.result() != null) {
            float intensity = animator.result().entry.rarity.intensity;
            if (intensity >= 0.7f) {
                float flash = (float) (1.0 - animator.revealProgress());
                flash = flash * flash * 0.28f * intensity;
                if (flash > 0.003f) {
                    drawRect(0, 0, width, height,
                            DrawUtil.withAlpha(animator.result().entry.rarity.color, flash));
                }
            }
        }

        boolean showTrail = state == AnimationState.ACCELERATING
                || state == AnimationState.SPINNING || state == AnimationState.DECELERATING;
        float trailAlpha = (float) animator.spinProgress();

        wheel.render(mc.fontRendererObj, cx, cy, radius, animator.wheelAngle(), animator.ballAngle(),
                dragon(), showTrail, trailAlpha, true);

        drawHubText(cx, cy, radius);
        drawHeader();
        drawDragonPanel();
        drawWeightPanel(cfg);
        drawControlsFooter();
        drawPointerReadout(cx, cy, radius, state);

        if (state.showingResult() && animator.result() != null) {
            drawRevealBurst(cx, cy, radius, cfg);
            drawResultCard(animator.result(), cfg);
        }

        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    /** Dragon name + simulated weight in the hub. */
    private void drawHubText(double cx, double cy, double radius) {
        DragonType d = dragon();
        String name = d.display.toUpperCase();
        drawCenteredStringScaled(name, (float) cx, (float) (cy - 10), 0.9f, d.accent);
        drawCenteredStringScaled(weight.total() + " wt", (float) cx, (float) (cy + 4), 0.8f, GOLD);
        if (!d.summonable) {
            drawCenteredStringScaled("reference", (float) cx, (float) (cy + 16), 0.7f, DIM);
        }
    }

    private void drawHeader() {
        String title = EnumChatFormatting.BOLD + "DRAGON ALTAR ROULETTE";
        drawCenteredString(mc.fontRendererObj, GOLD_STR + title, width / 2, 8, GOLD);
        drawCenteredString(mc.fontRendererObj, "Visual simulation • not a real Hypixel reward",
                width / 2, 20, DIM);
    }

    private static final String GOLD_STR = "" + EnumChatFormatting.GOLD;

    /** Left panel: selected dragon + its armour ability and a summonable note. */
    private void drawDragonPanel() {
        DragonType d = dragon();
        int x = 6, y = 40, w = 150;
        if (width < 560) return; // hide on narrow screens to avoid clutter
        int lines = d.summonable ? 5 : 6;
        int h = 16 + lines * 11;
        panel(x, y, w, h);
        int ty = y + 6;
        drawString(mc.fontRendererObj, EnumChatFormatting.BOLD + "Dragon", x + 6, ty, GOLD); ty += 12;
        drawString(mc.fontRendererObj, d.display + " Dragon", x + 6, ty, d.accent); ty += 11;
        drawString(mc.fontRendererObj, (dragonIndex + 1) + "/" + dragons.length
                + "  (< >)", x + 6, ty, DIM); ty += 13;
        ty = drawWrapped(d.blurb, x + 6, ty, w - 12, TEXT);
        if (!d.summonable) {
            drawString(mc.fontRendererObj, EnumChatFormatting.RED + "Not summonable", x + 6, ty, 0xFFFF6B6B);
        }
    }

    /** Right panel: the Dragon Weight simulation and eligibility. */
    private void drawWeightPanel(SpookyConfig cfg) {
        if (width < 560) return;
        int w = 160, x = width - w - 6, y = 40;
        int h = 108;
        panel(x, y, w, h);
        int ty = y + 6;
        drawString(mc.fontRendererObj, EnumChatFormatting.BOLD + "Weight sim", x + 6, ty, GOLD); ty += 12;
        drawString(mc.fontRendererObj, "Eyes: " + weight.eyes() + "/" + DragonWeight.MAX_EYES
                + "  (+" + weight.eyeWeight() + ")", x + 6, ty, TEXT); ty += 11;
        drawString(mc.fontRendererObj, "Dmg rank: " + ordinal(weight.damageRank())
                + "  (+" + weight.rankWeight() + ")", x + 6, ty, TEXT); ty += 11;
        drawString(mc.fontRendererObj, "Final blow: " + (weight.finalBlow() ? "yes (+50)" : "no"),
                x + 6, ty, weight.finalBlow() ? 0xFF7CFF5A : DIM); ty += 11;
        drawString(mc.fontRendererObj, "Total: " + EnumChatFormatting.GOLD + weight.total() + " weight",
                x + 6, ty, GOLD); ty += 12;
        // Eligibility: how many of this pool's weight-gated items are reachable.
        int gated = 0, eligible = 0;
        for (LootEntry e : loot.entries()) {
            if (e.weightReq > 0) {
                gated++;
                if (weight.eligibleFor(e)) eligible++;
            }
        }
        drawString(mc.fontRendererObj, "Eligible: " + eligible + "/" + gated + " gated items",
                x + 6, ty, eligible == gated ? 0xFF7CFF5A : TEXT); ty += 11;
        drawString(mc.fontRendererObj, "[ ] eyes  - = rank  F blow", x + 6, ty, DIM);
    }

    private void drawControlsFooter() {
        String help = (dragon().summonable
                ? "SPACE / click spin    R replay    < > dragon    ESC close"
                : "Holy is reference-only    < > dragon    ESC close");
        drawCenteredString(mc.fontRendererObj, help, width / 2, height - 14, DIM);
    }

    /** The item currently under the pointer (live readout above the wheel). */
    private void drawPointerReadout(double cx, double cy, double radius, AnimationState state) {
        if (state.showingResult()) return;
        int n = loot.size();
        if (n == 0) return;
        double seg = 360.0 / n;
        int idx = (int) Math.floor(norm360(-animator.wheelAngle()) / seg) % n;
        LootEntry e = loot.get(idx);
        drawCenteredString(mc.fontRendererObj, e.name, (int) cx, (int) (cy - radius - 26), e.rarity.color);
    }

    /** A radiating spark burst from the winning pocket. */
    private void drawRevealBurst(double cx, double cy, double radius, SpookyConfig cfg) {
        if (cfg.dragonReducedMotion || cfg.dragonParticleIntensity <= 0.01) return;
        RouletteResult res = animator.result();
        float p = (float) animator.revealProgress();
        float fade = 1f - p;
        if (fade <= 0.02f) return;
        int color = res.entry.rarity.color;
        double wy = cy - radius * 0.74; // winning pocket at the top pointer
        int rays = (int) (10 * res.entry.rarity.intensity * clampScale(cfg.dragonParticleIntensity)) + 4;
        double reach = radius * (0.18 + 0.5 * Easing.easeOutCubic(p));
        DrawUtil.begin();
        for (int i = 0; i < rays; i++) {
            double a = 360.0 * i / rays;
            double x1 = DrawUtil.px(cx, a, radius * 0.06);
            double y1 = DrawUtil.py(wy, a, radius * 0.06);
            double x2 = DrawUtil.px(cx, a, reach);
            double y2 = DrawUtil.py(wy, a, reach);
            DrawUtil.thickLine(x1, y1, x2, y2, 1.6, DrawUtil.withAlpha(color, fade * 0.8f));
        }
        DrawUtil.radialGlow(cx, wy, radius * (0.1 + 0.25 * p),
                DrawUtil.withAlpha(res.entry.rarity.glow, fade * 0.6f),
                DrawUtil.withAlpha(res.entry.rarity.glow, 0f), 24);
        DrawUtil.end();
    }

    /** The result card: DRAGON ALTAR ROULETTE RESULT → item, rarity, simulation. */
    private void drawResultCard(RouletteResult res, SpookyConfig cfg) {
        float pop = (float) Easing.easeOutBack(animator.revealProgress());
        int w = 210, h = 92;
        int cxp = width / 2;
        int top = height - h - 24;

        GlStateManager.pushMatrix();
        GlStateManager.translate(cxp, top + h / 2.0, 0);
        GlStateManager.scale(pop, pop, 1f);
        GlStateManager.translate(-cxp, -(top + h / 2.0), 0);

        int x = cxp - w / 2;
        panel(x, top, w, h);

        LootEntry e = res.entry;
        int ty = top + 6;
        drawCenteredString(mc.fontRendererObj, EnumChatFormatting.GOLD + "DRAGON ALTAR ROULETTE", cxp, ty, GOLD); ty += 11;
        // Item glyph.
        DrawUtil.begin();
        double gy = ty + 10;
        DrawUtil.radialGlow(cxp, gy, 16, DrawUtil.withAlpha(e.rarity.glow, 0.7f),
                DrawUtil.withAlpha(e.rarity.glow, 0f), 20);
        DrawUtil.disc(cxp, gy, 7, e.rarity.color, 16);
        DrawUtil.disc(cxp - 2, gy - 2, 3, 0xFFFFFFFF, 10);
        DrawUtil.end();
        ty += 24;
        drawCenteredString(mc.fontRendererObj, e.name, cxp, ty, e.rarity.color); ty += 11;
        String meta = e.rarity.display.toUpperCase();
        if (cfg.dragonShowWeights && e.weightReq > 0) meta += "  •  " + e.weightReq + " wt";
        if (cfg.dragonShowPercents && e.dropInfo != null) meta += "  •  " + e.dropInfo;
        drawCenteredString(mc.fontRendererObj, meta, cxp, ty, DIM); ty += 11;
        drawCenteredString(mc.fontRendererObj, EnumChatFormatting.ITALIC + "Simulation result — visual only",
                cxp, ty, 0xFF9A93AE);

        GlStateManager.popMatrix();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    // ---- sound ------------------------------------------------------------

    private void playRevealSound() {
        SpookyConfig cfg = SpookyConfig.INSTANCE;
        if (!cfg.dragonSound || mc.thePlayer == null || animator.result() == null) return;
        float intensity = animator.result().entry.rarity.intensity;
        if (intensity >= 0.85f) {
            mc.thePlayer.playSound("mob.enderdragon.growl", 0.5f, 1.4f);
            mc.thePlayer.playSound("random.levelup", 0.6f, 1.2f);
        } else if (intensity >= 0.6f) {
            mc.thePlayer.playSound("random.levelup", 0.5f, 1.3f);
        } else {
            mc.thePlayer.playSound("random.orb", 0.5f, 1.0f + intensity);
        }
    }

    // ---- small drawing helpers -------------------------------------------

    private void panel(int x, int y, int w, int h) {
        drawRect(x, y, x + w, y + h, PANEL_BG);
        drawRect(x, y, x + w, y + 1, PANEL_BORDER);
        drawRect(x, y + h - 1, x + w, y + h, PANEL_BORDER);
        drawRect(x, y, x + 1, y + h, PANEL_BORDER);
        drawRect(x + w - 1, y, x + w, y + h, PANEL_BORDER);
    }

    private void drawCenteredStringScaled(String s, float x, float y, float scale, int color) {
        int w = mc.fontRendererObj.getStringWidth(s);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(scale, scale, 1f);
        mc.fontRendererObj.drawStringWithShadow(s, -w / 2f, -mc.fontRendererObj.FONT_HEIGHT / 2f, color);
        GlStateManager.popMatrix();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    /** Word-wrap text in a box; returns the next y. */
    private int drawWrapped(String text, int x, int y, int maxWidth, int color) {
        List<String> lines = mc.fontRendererObj.listFormattedStringToWidth(text, maxWidth);
        for (String l : lines) {
            drawString(mc.fontRendererObj, l, x, y, color);
            y += 10;
        }
        return y;
    }

    private static String ordinal(int v) {
        int m = v % 100;
        if (m >= 11 && m <= 13) return v + "th";
        switch (v % 10) {
            case 1: return v + "st";
            case 2: return v + "nd";
            case 3: return v + "rd";
            default: return v + "th";
        }
    }

    private static double norm360(double a) {
        double r = a % 360.0;
        return r < 0 ? r + 360.0 : r;
    }

    private static float clampScale(double v) {
        return (float) (v < 0.5 ? 0.5 : (v > 2.0 ? 2.0 : v));
    }
}
