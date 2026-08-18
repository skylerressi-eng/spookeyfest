package com.spookybirch.gui;

import com.spookybirch.data.CandyData;
import com.spookybirch.data.CandyMob;
import com.spookybirch.data.FishingData;
import com.spookybirch.data.SeaCreature;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.util.List;

/**
 * The reference book (open with /spooky guide). Two tabs: which mobs give the
 * most candy, and the spooky-fishing sea creatures with catch chances.
 */
public class GuideScreen extends GuiScreen {

    private int tab = 0; // 0 = candy, 1 = fishing

    @Override
    public void initGui() {
        buttonList.clear();
        buttonList.add(new GuiButton(0, width / 2 - 105, 24, 100, 20, "Candy Mobs"));
        buttonList.add(new GuiButton(1, width / 2 + 5, 24, 100, 20, "Fishing"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) tab = 0;
        if (button.id == 1) tab = 1;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(mc.fontRendererObj, "☠ SpookyBirch Guide", width / 2, 8, 0xFFFF8C1A);
        super.drawScreen(mouseX, mouseY, partialTicks);

        int left = width / 2 - 200;
        int y = 56;
        if (tab == 0) {
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

    /** Right-pad to a fixed width so columns line up in the fixed-width-ish font. */
    private static String pad(String s, int width) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < width) sb.append(' ');
        return sb.toString();
    }
}
