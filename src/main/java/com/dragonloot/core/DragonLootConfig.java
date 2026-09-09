package com.dragonloot.core;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

/**
 * Persistent settings for DragonLoot, stored in config/dragonloot.cfg. Holds
 * the gambling odds (pushed into {@link RollEngine}), the trigger behaviour, and
 * the lifetime stats (pushed into {@link DragonStats}). Same shape as
 * {@code SpookyConfig}.
 */
public final class DragonLootConfig {

    public static final DragonLootConfig INSTANCE = new DragonLootConfig();

    private Configuration cfg;

    // ---- behaviour ----
    public boolean autoTrigger = true;   // play the reveal when a dragon dies
    public boolean requireEyes = true;   // ...only if we saw the summon first
    public boolean playSounds  = true;
    public double  animationSpeed = 1.0; // 0.5 (slow) .. 2.0 (fast)

    // ---- odds (fed into RollEngine) ----
    public double uncommonToRare  = 0.50;
    public double rareToEpic      = 0.30;
    public double epicToLegendary = 0.15;
    public double luck            = 1.0;

    // ---- persisted lifetime stats (fed into DragonStats) ----
    public long statUncommon = 0L, statRare = 0L, statEpic = 0L, statLegendary = 0L;
    public long statTotal = 0L;
    public int  statBestOrdinal = -1;

    public void load(File dir) {
        File file = new File(dir, "dragonloot.cfg");
        cfg = new Configuration(file);
        read();
    }

    private void read() {
        cfg.load();
        autoTrigger    = cfg.getBoolean("autoTrigger", "trigger", true, "Auto-play the reveal when a dragon dies");
        requireEyes    = cfg.getBoolean("requireEyes", "trigger", true, "Only auto-play if a summon was seen first");
        playSounds     = cfg.getBoolean("playSounds", "trigger", true, "Play crack/reveal sounds");
        animationSpeed = clamp(cfg.get("trigger", "animationSpeed", 1.0D, "Reveal speed (0.5 slow .. 2.0 fast)", 0.5D, 2.0D).getDouble(), 0.5, 2.0);

        uncommonToRare  = odd(cfg.get("odds", "uncommonToRare",  0.50D, "Chance to crack Uncommon -> Rare",  0.0D, RollEngine.MAX_ADVANCE).getDouble());
        rareToEpic      = odd(cfg.get("odds", "rareToEpic",      0.30D, "Chance to crack Rare -> Epic",      0.0D, RollEngine.MAX_ADVANCE).getDouble());
        epicToLegendary = odd(cfg.get("odds", "epicToLegendary", 0.15D, "Chance to crack Epic -> Legendary", 0.0D, RollEngine.MAX_ADVANCE).getDouble());
        luck            = clamp(cfg.get("odds", "luck", 1.0D, "Global luck multiplier on all crack odds", 0.0D, 10.0D).getDouble(), 0.0, 10.0);

        statUncommon    = cfg.getInt("uncommon", "stats", 0, 0, Integer.MAX_VALUE, "Lifetime Uncommon pulls");
        statRare        = cfg.getInt("rare", "stats", 0, 0, Integer.MAX_VALUE, "Lifetime Rare pulls");
        statEpic        = cfg.getInt("epic", "stats", 0, 0, Integer.MAX_VALUE, "Lifetime Epic pulls");
        statLegendary   = cfg.getInt("legendary", "stats", 0, 0, Integer.MAX_VALUE, "Lifetime Legendary pulls");
        statTotal       = cfg.getInt("total", "stats", 0, 0, Integer.MAX_VALUE, "Lifetime total rolls");
        statBestOrdinal = cfg.getInt("bestOrdinal", "stats", -1, -1, Rarity.values().length - 1, "Best pull (rarity ordinal, -1 = none)");

        if (cfg.hasChanged()) cfg.save();
        applyToEngine();
        applyToStats();
    }

    /** Push the odds into the roll engine. */
    public void applyToEngine() {
        RollEngine e = RollEngine.INSTANCE;
        e.setAdvanceChance(Rarity.UNCOMMON, uncommonToRare);
        e.setAdvanceChance(Rarity.RARE, rareToEpic);
        e.setAdvanceChance(Rarity.EPIC, epicToLegendary);
        e.setLuck(luck);
    }

    /** Push the saved counts into the live stats tracker. */
    public void applyToStats() {
        DragonStats.INSTANCE.loadCounts(
                new long[] { statUncommon, statRare, statEpic, statLegendary },
                statTotal, statBestOrdinal);
    }

    /** Pull the live stats back out and persist them. Call after each roll. */
    public void saveStats() {
        long[] c = DragonStats.INSTANCE.countsSnapshot();
        statUncommon    = c.length > 0 ? c[0] : 0L;
        statRare        = c.length > 1 ? c[1] : 0L;
        statEpic        = c.length > 2 ? c[2] : 0L;
        statLegendary   = c.length > 3 ? c[3] : 0L;
        statTotal       = DragonStats.INSTANCE.total();
        statBestOrdinal = DragonStats.INSTANCE.bestOrdinal();
        save();
    }

    public void save() {
        if (cfg == null) return;
        cfg.get("trigger", "autoTrigger", true).set(autoTrigger);
        cfg.get("trigger", "requireEyes", true).set(requireEyes);
        cfg.get("trigger", "playSounds", true).set(playSounds);
        cfg.get("trigger", "animationSpeed", 1.0D).set(animationSpeed);
        cfg.get("odds", "uncommonToRare", 0.50D).set(uncommonToRare);
        cfg.get("odds", "rareToEpic", 0.30D).set(rareToEpic);
        cfg.get("odds", "epicToLegendary", 0.15D).set(epicToLegendary);
        cfg.get("odds", "luck", 1.0D).set(luck);
        cfg.get("stats", "uncommon", 0).set((int) Math.min(Integer.MAX_VALUE, statUncommon));
        cfg.get("stats", "rare", 0).set((int) Math.min(Integer.MAX_VALUE, statRare));
        cfg.get("stats", "epic", 0).set((int) Math.min(Integer.MAX_VALUE, statEpic));
        cfg.get("stats", "legendary", 0).set((int) Math.min(Integer.MAX_VALUE, statLegendary));
        cfg.get("stats", "total", 0).set((int) Math.min(Integer.MAX_VALUE, statTotal));
        cfg.get("stats", "bestOrdinal", -1).set(statBestOrdinal);
        applyToEngine();
        cfg.save();
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static double odd(double v) {
        return clamp(v, 0.0, RollEngine.MAX_ADVANCE);
    }

    private DragonLootConfig() {}
}
