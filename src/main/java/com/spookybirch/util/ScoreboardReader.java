package com.spookybirch.util;

import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Reads the sidebar scoreboard lines. On Hypixel SkyBlock the sidebar carries
 * the location, date/time and event info, so we use it to know whether the
 * Spooky Festival is running and how long is left.
 */
public final class ScoreboardReader {

    /** All visible sidebar lines, top-to-bottom, color codes stripped. */
    public static List<String> lines() {
        List<String> out = new ArrayList<String>();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null) return out;

        Scoreboard sb = mc.theWorld.getScoreboard();
        if (sb == null) return out;

        ScoreObjective obj = sb.getObjectiveInDisplaySlot(1); // slot 1 = sidebar
        if (obj == null) return out;

        Collection<Score> scores = sb.getSortedScores(obj);
        List<Score> filtered = new ArrayList<Score>();
        for (Score s : scores) {
            if (s != null && s.getPlayerName() != null && !s.getPlayerName().startsWith("#")) {
                filtered.add(s);
            }
        }
        // Sidebar shows at most 15 lines, highest score at the top.
        int start = Math.max(0, filtered.size() - 15);
        for (int i = filtered.size() - 1; i >= start; i--) {
            Score s = filtered.get(i);
            ScorePlayerTeam team = sb.getPlayersTeam(s.getPlayerName());
            String line = ScorePlayerTeam.formatPlayerName(team, s.getPlayerName());
            out.add(TextUtil.stripColor(line).trim());
        }
        return out;
    }

    /** The scoreboard title (usually "SKYBLOCK"), color codes stripped. */
    public static String title() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null) return "";
        Scoreboard sb = mc.theWorld.getScoreboard();
        if (sb == null) return "";
        ScoreObjective obj = sb.getObjectiveInDisplaySlot(1);
        if (obj == null) return "";
        return TextUtil.stripColor(obj.getDisplayName());
    }

    private ScoreboardReader() {}
}
