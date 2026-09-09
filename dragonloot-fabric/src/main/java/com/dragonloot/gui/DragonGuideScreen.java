package com.dragonloot.gui;

import com.dragonloot.client.DragonReveal;
import com.dragonloot.core.DragonStats;
import com.dragonloot.core.Rarity;
import com.dragonloot.core.RollEngine;
import com.dragonloot.data.DragonReward;
import com.dragonloot.data.RewardData;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * The Dragon Luck book (keybind H, or /dragonloot guide). Tabs for the live odds
 * you're gambling against, your lifetime pull stats, and the reward tables, plus
 * a Test button that plays a full reveal.
 */
public class DragonGuideScreen extends Screen {

    private int tab = 0; // 0 = odds, 1 = stats, 2 = rewards

    public DragonGuideScreen() {
        super(Component.literal("Dragon Luck"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        addRenderableWidget(Button.builder(Component.literal("Odds"), b -> tab = 0)
                .bounds(cx - 152, 24, 98, 20).build());
        addRenderableWidget(Button.builder(Component.literal("My Luck"), b -> tab = 1)
                .bounds(cx - 49, 24, 98, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Rewards"), b -> tab = 2)
                .bounds(cx + 54, 24, 98, 20).build());
        addRenderableWidget(Button.builder(Component.literal("▶ Test Reveal"), b -> DragonReveal.rollAndShow())
                .bounds(cx - 60, height - 30, 120, 20).build());
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0xC0000000);
        ctx.drawCenteredString(font, Component.literal("☬ Dragon Luck"), width / 2, 9, 0xFFAA33FF);

        int left = width / 2 - 190;
        int y = 56;
        if (tab == 0) drawOdds(ctx, left, y);
        else if (tab == 1) drawStats(ctx, left, y);
        else drawRewards(ctx, left, y);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawOdds(GuiGraphics ctx, int left, int y) {
        RollEngine e = RollEngine.INSTANCE;
        line(ctx, left, y, "What you're gambling against  (luck x" + trim(e.getLuck()) + ")", 0xFFBBBBBB); y += 20;
        line(ctx, left, y, "Crack chances (chance to escalate a tier):", 0xFFFFFFFF); y += 14;
        y = row(ctx, left, y, "Uncommon -> Rare", pct(e.effectiveAdvance(Rarity.UNCOMMON)), 0xFF5599FF);
        y = row(ctx, left, y, "Rare -> Epic", pct(e.effectiveAdvance(Rarity.RARE)), 0xFFAA33FF);
        y = row(ctx, left, y, "Epic -> Legendary", pct(e.effectiveAdvance(Rarity.EPIC)), 0xFFFFAA00);
        y += 8;
        line(ctx, left, y, "Chance of each final pull:", 0xFFFFFFFF); y += 14;
        double[] probs = e.finalProbabilities();
        for (Rarity r : Rarity.values()) y = row(ctx, left, y, r.displayName, pct(probs[r.ordinal()]), r.argb);
        y += 8;
        line(ctx, left, y, "Tune with /dragonloot odds <tier> <%>  or  /dragonloot luck <x>", 0xFF777777);
    }

    private void drawStats(GuiGraphics ctx, int left, int y) {
        DragonStats s = DragonStats.INSTANCE;
        line(ctx, left, y, "Lifetime eggs cracked: " + s.total(), 0xFFFFFFFF); y += 20;
        if (s.total() == 0) {
            line(ctx, left, y, "No eggs cracked yet — hit Test Reveal, or /dragongamble.", 0xFF777777);
            return;
        }
        for (Rarity r : Rarity.values()) {
            y = row(ctx, left, y, r.displayName, s.count(r) + "  (" + pct(s.share(r)) + ")", r.argb);
        }
        y += 8;
        Rarity best = s.best();
        line(ctx, left, y, "Best pull: " + (best == null ? "none" : best.displayName),
                best == null ? 0xFF777777 : best.argb); y += 16;
        StringBuilder sb = new StringBuilder("Recent: ");
        List<Rarity> hist = s.history();
        for (int i = hist.size() - 1; i >= 0; i--) sb.append("*");
        line(ctx, left, y, sb.toString(), 0xFFBBBBBB); y += 16;
        line(ctx, left, y, "/dragonloot reset clears these stats", 0xFF777777);
    }

    private void drawRewards(GuiGraphics ctx, int left, int y) {
        line(ctx, left, y, "Cosmetic loot the egg can hatch (flavor only):", 0xFFBBBBBB); y += 16;
        for (Rarity r : Rarity.values()) {
            line(ctx, left, y, r.displayName + ":", r.argb); y += 12;
            for (DragonReward d : RewardData.forRarity(r)) {
                line(ctx, left + 8, y, d.name + "  — " + d.note, 0xFFDDDDDD); y += 11;
            }
            y += 4;
        }
    }

    private void line(GuiGraphics ctx, int x, int y, String text, int color) {
        ctx.drawString(font, Component.literal(text), x, y, color);
    }

    private int row(GuiGraphics ctx, int left, int y, String label, String value, int valueColor) {
        ctx.drawString(font, Component.literal(label), left, y, 0xFFCCCCCC);
        ctx.drawString(font, Component.literal(value), left + 160, y, valueColor);
        return y + 13;
    }

    private static String pct(double p) { return String.format("%.1f%%", p * 100.0); }
    private static String trim(double v) {
        String s = String.format("%.2f", v);
        if (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        if (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return s;
    }
}
