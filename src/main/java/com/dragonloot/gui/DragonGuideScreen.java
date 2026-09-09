package com.dragonloot.gui;

import com.dragonloot.core.DragonStats;
import com.dragonloot.core.Rarity;
import com.dragonloot.core.RollEngine;
import com.dragonloot.data.DragonReward;
import com.dragonloot.data.RewardData;
import com.dragonloot.event.DragonReveal;
import com.spookybirch.util.Fmt;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.util.List;

/**
 * The Dragon Luck book (keybind H, or /dragonloot guide). Three tabs: the live
 * odds you're gambling against, your lifetime pull stats, and the reward tables.
 */
public class DragonGuideScreen extends GuiScreen {

    private int tab = 0; // 0 = odds, 1 = stats, 2 = rewards

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        buttonList.add(new GuiButton(0, width / 2 - 152, 24, 98, 20, "Odds"));
        buttonList.add(new GuiButton(1, width / 2 - 49, 24, 98, 20, "My Luck"));
        buttonList.add(new GuiButton(2, width / 2 + 54, 24, 98, 20, "Rewards"));
        buttonList.add(new GuiButton(3, width / 2 + 108, height - 28, 44, 20, "Test"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 3) {
            DragonReveal.rollAndShow(); // play a test reveal
            return;
        }
        tab = button.id;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(mc.fontRendererObj, "§5§l☬ Dragon Luck", width / 2, 8, 0xFFFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);

        int left = width / 2 - 200;
        int y = 56;
        if (tab == 0) drawOdds(left, y);
        else if (tab == 1) drawStatsTab(left, y);
        else drawRewards(left, y);

        drawCenteredString(mc.fontRendererObj, "§8Press Test to play a demo reveal", width / 2, height - 44, 0xFFFFFFFF);
    }

    private void drawOdds(int left, int y) {
        RollEngine e = RollEngine.INSTANCE;
        drawString(mc.fontRendererObj, "§7What you're gambling against (luck ×" + Fmt.num(e.getLuck()) + "):", left, y, 0xFFFFFFFF);
        y += 18;
        drawString(mc.fontRendererObj, "§fCrack chances (chance to escalate a tier):", left, y, 0xFFFFFFFF);
        y += 13;
        y = row(left, y, "Uncommon → Rare", pct(e.effectiveAdvance(Rarity.UNCOMMON)), 0xFF5599FF);
        y = row(left, y, "Rare → Epic", pct(e.effectiveAdvance(Rarity.RARE)), 0xFFAA33FF);
        y = row(left, y, "Epic → Legendary", pct(e.effectiveAdvance(Rarity.EPIC)), 0xFFFFAA00);
        y += 8;

        drawString(mc.fontRendererObj, "§fChance of each final pull:", left, y, 0xFFFFFFFF);
        y += 13;
        double[] probs = e.finalProbabilities();
        for (Rarity r : Rarity.values()) {
            y = row(left, y, r.colored(), pct(probs[r.ordinal()]), r.argb);
        }
        y += 8;
        drawString(mc.fontRendererObj, "§8Tune odds with /dragonloot odds <tier> <percent> or /dragonloot luck <x>", left, y, 0xFFFFFFFF);
    }

    private void drawStatsTab(int left, int y) {
        DragonStats s = DragonStats.INSTANCE;
        drawString(mc.fontRendererObj, "§7Lifetime eggs cracked: §f" + Fmt.num(s.total()), left, y, 0xFFFFFFFF);
        y += 18;
        if (s.total() == 0) {
            drawString(mc.fontRendererObj, "§8No eggs cracked yet — go slay a dragon (or hit Test).", left, y, 0xFFFFFFFF);
            return;
        }
        for (Rarity r : Rarity.values()) {
            String v = Fmt.num(s.count(r)) + "  §8(" + pct(s.share(r)) + ")";
            y = row(left, y, r.colored(), v, r.argb);
        }
        y += 8;
        Rarity best = s.best();
        drawString(mc.fontRendererObj, "§7Best pull: " + (best == null ? "§8none" : best.colored()), left, y, 0xFFFFFFFF);
        y += 16;
        List<Rarity> hist = s.history();
        StringBuilder sb = new StringBuilder("§7Recent: ");
        for (int i = hist.size() - 1; i >= 0; i--) {
            sb.append(hist.get(i).colorCode).append("◆");
        }
        drawString(mc.fontRendererObj, sb.toString(), left, y, 0xFFFFFFFF);
        y += 16;
        drawString(mc.fontRendererObj, "§8/dragonloot reset clears these stats", left, y, 0xFFFFFFFF);
    }

    private void drawRewards(int left, int y) {
        drawString(mc.fontRendererObj, "§7Cosmetic loot the egg can hatch (flavor only):", left, y, 0xFFFFFFFF);
        y += 16;
        for (Rarity r : Rarity.values()) {
            drawString(mc.fontRendererObj, r.colored() + ":", left, y, 0xFFFFFFFF);
            y += 12;
            for (DragonReward d : RewardData.forRarity(r)) {
                drawString(mc.fontRendererObj, "  " + r.colorCode + d.name + " §8— " + d.note, left, y, 0xFFFFFFFF);
                y += 11;
            }
            y += 4;
        }
    }

    private int row(int left, int y, String label, String value, int valueColor) {
        drawString(mc.fontRendererObj, pad(label, 24), left, y, 0xFFCCCCCC);
        drawString(mc.fontRendererObj, value, left + 150, y, valueColor);
        return y + 13;
    }

    private static String pct(double p) {
        return String.format("%.1f%%", p * 100.0);
    }

    private static String pad(String s, int width) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < width) sb.append(' ');
        return sb.toString();
    }
}
