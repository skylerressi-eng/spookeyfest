package com.spookybirch.gui;

import com.spookybirch.core.CandyTracker;
import com.spookybirch.core.SpookyConfig;
import com.spookybirch.data.CandyData;
import com.spookybirch.data.CandyMob;
import com.spookybirch.data.FishingData;
import com.spookybirch.data.SeaCreature;
import com.spookybirch.util.Fmt;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.util.List;

/**
 * The reference book (open with /spooky guide). Three tabs: a live candy score
 * tracker, which mobs give the most candy, and the spooky-fishing sea creatures.
 */
public class GuideScreen extends GuiScreen {

    private int tab = 0; // 0 = stats, 1 = candy, 2 = fishing

    @Override
    public boolean doesGuiPauseGame() {
        return false; // keep the live "My Score" tab ticking in singleplayer
    }

    @Override
    public void initGui() {
        buttonList.clear();
        buttonList.add(new GuiButton(0, width / 2 - 152, 24, 98, 20, "My Score"));
        buttonList.add(new GuiButton(1, width / 2 - 49, 24, 98, 20, "Candy Mobs"));
        buttonList.add(new GuiButton(2, width / 2 + 54, 24, 98, 20, "Fishing"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        tab = button.id;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(mc.fontRendererObj, "☠ SpookyBirch Guide", width / 2, 8, 0xFFFF8C1A);
        super.drawScreen(mouseX, mouseY, partialTicks);

        int left = width / 2 - 200;
        int y = 56;
        if (tab == 0) {
            drawStats(left, y);
        } else if (tab == 1) {
            drawString(mc.fontRendererObj, "Ranked by candy value (green + purple×8):", left, y, 0xFFBBBBBB);
            y += 14;
            drawString(mc.fontRendererObj, pad("Mob", 20) + pad("Green", 8) + pad("Purple%", 9) + "Where", left, y, 0xFFFFFFFF);
            y += 12;
            List<CandyMob> ranked = CandyData.ranked();
            for (CandyMob m : ranked) {
                String row = pad(m.name, 20)
                        + pad(String.format("%.1f", m.greenPerKill), 8)
                        + pad(String.format("%.1f%%", m.purpleChance * 100), 9)
                        + m.where;
                drawString(mc.fontRendererObj, row, left, y, 0xFFDDDDDD);
                y += 11;
            }
        } else {
            drawString(mc.fontRendererObj, "Spooky sea creatures (fish anywhere during the fest):", left, y, 0xFFBBBBBB);
            y += 14;
            drawString(mc.fontRendererObj, pad("Creature", 16) + pad("Chance", 9) + "Drops", left, y, 0xFFFFFFFF);
            y += 12;
            for (SeaCreature c : FishingData.all()) {
                String row = pad(c.name, 16)
                        + pad(String.format("%.0f%%", c.chance * 100), 9)
                        + c.drops;
                drawString(mc.fontRendererObj, row, left, y, 0xFF88CCFF);
                y += 11;
                drawString(mc.fontRendererObj, "   " + c.note, left, y, 0xFF777777);
                y += 11;
            }
        }
    }

    /** The live candy score tracker tab. */
    private void drawStats(int left, int y) {
        CandyTracker t = CandyTracker.INSTANCE;
        SpookyConfig cfg = SpookyConfig.INSTANCE;

        drawString(mc.fontRendererObj, "This session's candy score (updates live):", left, y, 0xFFBBBBBB);
        y += 18;

        y = statRow(left, y, "Candy score", Fmt.num(t.score()), 0xFFFF8C1A);
        y = statRow(left, y, "Green collected", Fmt.num(t.collectedGreen()), 0xFF55FF55);
        y = statRow(left, y, "Purple collected", Fmt.num(t.collectedPurple()), 0xFFAA55FF);
        y += 6;
        y = statRow(left, y, "Rate (recent)", Fmt.rate(t.recentRatePerHour()), 0xFF55FFFF);
        y = statRow(left, y, "Rate (session)", Fmt.rate(t.overallRatePerHour()), 0xFF55FFFF);
        y = statRow(left, y, "Best rate", Fmt.rate(t.bestRecentRate()), 0xFF55FFFF);
        y += 6;
        y = statRow(left, y, "Time played", Fmt.duration(t.elapsedMs() / 1000L), 0xFFFFFFFF);

        if (cfg.candyGoal > 0) {
            long eta = t.etaSecondsToGoal();
            String etaStr = eta == 0L ? "reached! ✔" : (eta < 0L ? "need more data" : "~" + Fmt.duration(eta));
            y = statRow(left, y, "Goal " + Fmt.num(cfg.candyGoal), etaStr, 0xFFFFFF55);
        } else {
            y = statRow(left, y, "Goal", "off — /spooky goal <n>", 0xFF777777);
        }

        y += 10;
        drawString(mc.fontRendererObj, "Purple weight ×" + Fmt.num(cfg.purpleWeight)
                + "   •   /spooky reset to clear   •   /spooky goal <n>", left, y, 0xFF777777);
    }

    /** One "Label: value" row; returns the next y. */
    private int statRow(int left, int y, String label, String value, int valueColor) {
        drawString(mc.fontRendererObj, pad(label, 20), left, y, 0xFFCCCCCC);
        drawString(mc.fontRendererObj, value, left + 130, y, valueColor);
        return y + 13;
    }

    /** Right-pad to a fixed width so columns line up in the fixed-width-ish font. */
    private static String pad(String s, int width) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < width) sb.append(' ');
        return sb.toString();
    }
}
