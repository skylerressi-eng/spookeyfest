package com.spookybirch.hud;

import com.spookybirch.core.CandyTracker;
import com.spookybirch.core.SpookyConfig;
import com.spookybirch.core.SpookyState;
import com.spookybirch.data.CandyData;
import com.spookybirch.data.CandyMob;
import com.spookybirch.util.Fmt;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the Birch-style overlay box in the corner. It only builds a line when
 * the matching config toggle is on, then renders a translucent panel behind the
 * text so it stays readable over any background.
 */
public class SpookyHud extends Gui {

    private static final int BG = 0x90000000;      // translucent black panel
    private static final int TITLE = 0xFFFF8C1A;   // pumpkin orange
    private static final int GREEN = 0xFF55FF55;
    private static final int PURPLE = 0xFFAA55FF;
    private static final int CYAN = 0xFF55FFFF;
    private static final int GREY = 0xFFBBBBBB;
    private static final int RED = 0xFFFF5555;

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;

        SpookyConfig cfg = SpookyConfig.INSTANCE;
        SpookyState st = SpookyState.INSTANCE;
        if (!cfg.hudEnabled) return;
        if (cfg.onlyDuringFestival && !st.festivalActive) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        if (mc.currentScreen != null) return; // hide behind menus

        List<Line> lines = build(cfg, st);
        if (lines.isEmpty()) return;

        GlStateManager.pushMatrix();
        GlStateManager.translate(cfg.hudX, cfg.hudY, 0);
        GlStateManager.scale(cfg.hudScale, cfg.hudScale, 1f);

        int pad = 4;
        int width = 0;
        for (Line l : lines) width = Math.max(width, mc.fontRendererObj.getStringWidth(l.text));
        int lineH = mc.fontRendererObj.FONT_HEIGHT + 2;
        int height = lines.size() * lineH;

        drawRect(-pad, -pad, width + pad, height + pad, BG);

        int y = 0;
        for (Line l : lines) {
            mc.fontRendererObj.drawStringWithShadow(l.text, 0, y, l.color);
            y += lineH;
        }

        GlStateManager.popMatrix();
    }

    /** The rendered version of the panel — used for a live preview in the move GUI too. */
    public static List<Line> build(SpookyConfig cfg, SpookyState st) {
        List<Line> lines = new ArrayList<Line>();
        lines.add(new Line("☠ SpookyBirch", TITLE));

        if (cfg.showEvent) {
            if (st.festivalActive) {
                String t = st.festivalTimeLeft.isEmpty() ? "LIVE" : st.festivalTimeLeft + " left";
                lines.add(new Line("Festival: " + t, GREEN));
            } else {
                lines.add(new Line("Festival: not active", GREY));
            }
        }

        if (cfg.showMana) {
            if (st.manaKnown) {
                lines.add(new Line("Mana: " + st.mana + "/" + st.maxMana, CYAN));
            } else {
                lines.add(new Line("Mana: --/--", GREY));
            }
        }

        if (cfg.showCandy) {
            lines.add(new Line("Green: " + st.greenCandy + "  (+" + st.greenGained() + ")", GREEN));
            lines.add(new Line("Purple: " + st.purpleCandy + "  (+" + st.purpleGained() + ")", PURPLE));
        }

        if (cfg.showScore) {
            CandyTracker t = CandyTracker.INSTANCE;
            lines.add(new Line("Score: " + Fmt.num(t.score()) + "  " + Fmt.rate(t.recentRatePerHour()), TITLE));
            // Nudge the player if candy has stopped coming in.
            long idle = t.secondsSinceGain();
            if (idle >= 30) {
                lines.add(new Line("Idle " + Fmt.duration(idle) + " — move spots?", RED));
            }
        }

        if (cfg.showEta) {
            long eta = CandyTracker.INSTANCE.etaSecondsToGoal();
            if (eta == 0L) {
                lines.add(new Line("Goal reached! ✔", GREEN));
            } else if (eta > 0L) {
                lines.add(new Line("Goal in " + Fmt.duration(eta), CYAN));
            }
        }

        if (cfg.showNearbyMobs) {
            int n = st.nearbySpookyMobs;
            lines.add(new Line("Spooky mobs near: " + n, n > 0 ? RED : GREY));
        }

        if (cfg.showBestMobTip) {
            CandyMob best = CandyData.best();
            lines.add(new Line("Best candy: " + best.name, TITLE));
        }

        return lines;
    }

    /** A HUD text line with its color. */
    public static class Line {
        public final String text;
        public final int color;
        public Line(String text, int color) { this.text = text; this.color = color; }
    }

    /** Rough pixel size of the panel at the given scale, for move-GUI hit testing. */
    public static int[] panelSize(List<Line> lines, float scale) {
        Minecraft mc = Minecraft.getMinecraft();
        int width = 0;
        for (Line l : lines) width = Math.max(width, mc.fontRendererObj.getStringWidth(l.text));
        int lineH = mc.fontRendererObj.FONT_HEIGHT + 2;
        int height = lines.size() * lineH;
        int pad = 4;
        return new int[] {
                (int) ((width + pad * 2) * scale),
                (int) ((height + pad * 2) * scale)
        };
    }

    // Keep ScaledResolution import meaningful for downstream tooling / future use.
    @SuppressWarnings("unused")
    private static ScaledResolution res(Minecraft mc) {
        return new ScaledResolution(mc);
    }
}
