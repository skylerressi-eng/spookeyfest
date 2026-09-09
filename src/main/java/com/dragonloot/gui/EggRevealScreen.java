package com.dragonloot.gui;

import com.dragonloot.core.DragonLootConfig;
import com.dragonloot.core.Rarity;
import com.dragonloot.core.RollResult;
import com.dragonloot.data.DragonReward;
import com.dragonloot.render.EggRenderer;

import net.minecraft.client.gui.GuiScreen;

import java.util.List;

/**
 * The gambling reveal. A dragon egg starts cracking at Uncommon and, one crack
 * at a time, either escalates a tier (Rare -> Epic -> Legendary) or breaks open
 * where it stands — exactly the sequence the {@link RollResult} baked in. The
 * screen just plays that result out on a timeline: shake, crack, flash, hold,
 * repeat, then a light-bursting break-open and the reward.
 *
 * All timing is wall-clock (ms) so it's frame-rate independent, and scaled by
 * the configured animation speed. The egg itself is drawn by {@link EggRenderer}
 * from plain rectangles, so there are no textures to ship.
 */
public class EggRevealScreen extends GuiScreen {

    // Base durations (ms), before the animation-speed scale is applied.
    private static final double INTRO_MS = 450;
    private static final double SHAKE_MS = 620;
    private static final double POP_MS   = 260;   // flash window after a crack
    private static final double HOLD_MS  = 420;
    private static final double STAGE_MS = SHAKE_MS + POP_MS + HOLD_MS;
    private static final double SPLIT_GROW_MS = 700;
    private static final double REVEAL_DELAY  = 480;  // reward fades in after the split
    private static final double BREAK_MS = 2600;
    private static final double AUTO_CLOSE_MS = 6500; // after break, close on its own

    private static final int SPLIT_MAX = 5; // egg-pixels each half slides apart

    private final RollResult result;
    private final DragonReward reward;
    private final List<Rarity> stages;

    private final long startMs;
    private int soundedStage = -1;
    private boolean soundedBreak = false;

    // Particles for the break-open burst (deterministic, seeded per-instance).
    private final double[] pAngle, pSpeed, pSize;
    private final int[] pColor;

    public EggRevealScreen(RollResult result, DragonReward reward) {
        this.result = result;
        this.reward = reward;
        this.stages = result.stages();
        this.startMs = System.currentTimeMillis();

        int n = 46;
        pAngle = new double[n];
        pSpeed = new double[n];
        pSize  = new double[n];
        pColor = new int[n];
        java.util.Random r = new java.util.Random(0xE6601L + result.finalRarity().ordinal());
        int rarA = result.finalRarity().argb;
        for (int i = 0; i < n; i++) {
            pAngle[i] = r.nextDouble() * Math.PI * 2.0;
            pSpeed[i] = 1.2 + r.nextDouble() * 3.2;
            pSize[i]  = 2 + r.nextInt(3);
            pColor[i] = (i % 3 == 0) ? 0xFFFFFFFF : (i % 3 == 1 ? rarA : mix(rarA, 0xFFFFFF, 0.5));
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false; // SkyBlock is multiplayer — dim the screen ourselves instead
    }

    /** Scaled milliseconds since the reveal began. */
    private double elapsed() {
        double speed = DragonLootConfig.INSTANCE.animationSpeed;
        if (speed < 0.5) speed = 0.5;
        if (speed > 2.0) speed = 2.0;
        return (System.currentTimeMillis() - startMs) * speed;
    }

    private double breakStart() {
        return INTRO_MS + stages.size() * STAGE_MS;
    }

    private boolean finished() {
        return elapsed() >= breakStart() + REVEAL_DELAY + 250;
    }

    @Override
    public void updateScreen() {
        if (!DragonLootConfig.INSTANCE.playSounds) { maybeAutoClose(); return; }
        double e = elapsed();
        double bs = breakStart();

        // Crack sounds: one per stage, at that stage's pop moment, pitch rising.
        if (e < bs) {
            int stage = (int) ((e - INTRO_MS) / STAGE_MS);
            double within = (e - INTRO_MS) - stage * STAGE_MS;
            if (stage >= 0 && stage < stages.size() && within >= SHAKE_MS && stage > soundedStage) {
                soundedStage = stage;
                float pitch = 0.7f + 0.22f * stage;
                playSound("random.orb", 0.9f, pitch);
                if (stages.get(stage) == Rarity.LEGENDARY) playSound("mob.enderdragon.growl", 0.6f, 1.4f);
            }
        } else if (!soundedBreak) {
            soundedBreak = true;
            playSound("random.break", 1.0f, 0.9f);
            playSound("random.levelup", 0.9f, result.finalRarity() == Rarity.LEGENDARY ? 0.9f : 1.3f);
            if (result.finalRarity() == Rarity.LEGENDARY) playSound("mob.enderdragon.growl", 1.0f, 1.0f);
        }
        maybeAutoClose();
    }

    private void maybeAutoClose() {
        if (elapsed() >= breakStart() + AUTO_CLOSE_MS) close();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawRect(0, 0, width, height, 0xB0000000); // extra dim for drama

        double e = elapsed();
        double bs = breakStart();

        int px = Math.max(4, Math.min(width, height) / 44);
        int eggH = 18 * px;
        int cx = width / 2;
        int topY = height / 2 - eggH / 2 - 8;

        drawCenteredString(mc.fontRendererObj, "§5§l☬ §dDragon Egg §5§l☬", cx, topY - 34, 0xFFFFFFFF);

        if (e < bs) {
            drawCrackingPhase(e, cx, topY, px);
        } else {
            drawBreakPhase(e - bs, cx, topY, px);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /** The shake / crack / escalate loop, before the egg finally opens. */
    private void drawCrackingPhase(double e, int cx, int topY, int px) {
        double t = e - INTRO_MS;
        int stage = t < 0 ? -1 : (int) (t / STAGE_MS);
        if (stage >= stages.size()) stage = stages.size() - 1;

        int crackCount;
        Rarity tier;
        int shakeX = 0;
        double flash = 0;

        if (stage < 0) {
            crackCount = 0;
            tier = stages.get(0);
        } else {
            double within = t - stage * STAGE_MS;
            boolean popped = within >= SHAKE_MS;
            tier = stages.get(stage);
            int prevCracks = stage > 0 ? stages.get(stage - 1).cracks : 0;
            crackCount = popped ? tier.cracks : prevCracks;

            if (!popped) {
                // Build tension: shake grows toward the crack, harder each tier.
                double p = within / SHAKE_MS;
                double amp = (1.5 + stage * 1.2) * (0.3 + p);
                shakeX = (int) (Math.sin(e / 32.0) * amp);
            } else if (within < SHAKE_MS + POP_MS) {
                flash = 1.0 - (within - SHAKE_MS) / POP_MS; // fade the flash out
                shakeX = (int) (Math.sin(e / 18.0) * (2 + stage));
            }
        }

        EggRenderer.drawEgg(cx, topY, px, tier.argb, crackCount, shakeX);

        // Rarity label under the egg once a crack has shown.
        if (crackCount > 0) {
            String label = tier.colorCode + "§l" + tier.displayName.toUpperCase();
            drawCenteredString(mc.fontRendererObj, label, cx, topY + 18 * px + 12, 0xFFFFFFFF);
        } else {
            drawCenteredString(mc.fontRendererObj, "§7the shell begins to crack...", cx, topY + 18 * px + 12, 0xFFFFFFFF);
        }

        if (flash > 0) {
            int a = (int) (0x70 * flash);
            drawRect(0, 0, width, height, ((a & 0xFF) << 24) | (tier.argb & 0xFFFFFF));
        }
    }

    /** The egg breaks open and pays out. */
    private void drawBreakPhase(double tb, int cx, int topY, int px) {
        Rarity r = result.finalRarity();
        int split = (int) (Math.min(1.0, tb / SPLIT_GROW_MS) * SPLIT_MAX);

        // Burst particles from the egg's centre.
        int cyMid = topY + 9 * px;
        drawParticles(cx, cyMid, tb);

        EggRenderer.drawEggBroken(cx, topY, px, r.argb, split);

        if (tb >= REVEAL_DELAY) {
            String title = r.colorCode + "§l" + r.displayName.toUpperCase() + "!";
            drawCenteredString(mc.fontRendererObj, title, cx, topY + 18 * px + 14, 0xFFFFFFFF);
            drawCenteredString(mc.fontRendererObj, r.colorCode + reward.name, cx, topY + 18 * px + 28, 0xFFFFFFFF);
            drawCenteredString(mc.fontRendererObj, "§7" + reward.note, cx, topY + 18 * px + 40, 0xFFFFFFFF);

            if (r == Rarity.LEGENDARY) {
                drawCenteredString(mc.fontRendererObj, "§6§l★ JACKPOT ★", cx, topY - 20, 0xFFFFFFFF);
            }
            drawCenteredString(mc.fontRendererObj, "§8press any key to continue", cx, height - 16, 0xFFFFFFFF);
        }
    }

    private void drawParticles(int cx, int cy, double tb) {
        double life = tb / 900.0; // 0..~ over the burst
        for (int i = 0; i < pAngle.length; i++) {
            double dist = pSpeed[i] * tb / 14.0;
            int x = cx + (int) (Math.cos(pAngle[i]) * dist);
            int y = cy + (int) (Math.sin(pAngle[i]) * dist + 0.02 * tb * life); // slight gravity
            int s = (int) pSize[i];
            int alpha = (int) (0xFF * Math.max(0.0, 1.0 - life * 0.8));
            if (alpha <= 0) continue;
            int c = ((alpha & 0xFF) << 24) | (pColor[i] & 0xFFFFFF);
            drawRect(x, y, x + s, y + s, c);
        }
    }

    private void close() {
        mc.displayGuiScreen(null);
    }

    private void playSound(String name, float vol, float pitch) {
        if (mc != null && mc.thePlayer != null) {
            mc.thePlayer.playSound(name, vol, pitch);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        // Let the player skip once the reward is up; otherwise only Esc bails.
        if (finished() || keyCode == 1 /* ESC */) close();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (finished()) close();
    }

    private static int mix(int a, int b, double t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return 0xFF000000
                | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8)
                | (int) (ab + (bb - ab) * t);
    }
}
