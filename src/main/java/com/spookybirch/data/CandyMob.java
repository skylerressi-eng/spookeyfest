package com.spookybirch.data;

/**
 * One row in the "which mobs give the most candy" table.
 *
 * Values are community-sourced averages for the Spooky Festival and are meant
 * as a guide, not exact game constants — Hypixel does not publish drop tables.
 * Everything here can be tuned in {@link CandyData} without touching code
 * anywhere else in the mod.
 */
public class CandyMob {
    public final String name;        // in-game mob name
    public final String where;       // where it spawns during the festival
    public final double greenPerKill; // average GREEN candy per kill
    public final double purpleChance; // chance (0-1) of a PURPLE candy on kill
    public final String note;        // anything worth calling out

    public CandyMob(String name, String where, double greenPerKill, double purpleChance, String note) {
        this.name = name;
        this.where = where;
        this.greenPerKill = greenPerKill;
        this.purpleChance = purpleChance;
        this.note = note;
    }

    /**
     * A single "candy score" so the guide can rank mobs. Purple candy is worth
     * ~8 green when trading, so we weight it that way.
     */
    public double score() {
        return greenPerKill + purpleChance * 8.0;
    }
}
