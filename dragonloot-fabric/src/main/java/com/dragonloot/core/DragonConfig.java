package com.dragonloot.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Persistent settings + lifetime stats, saved as a plain .properties file. Uses
 * only the JDK (no Minecraft/Fabric types) so it stays testable and portable —
 * the client just hands it a config directory. Odds flow into {@link RollEngine}
 * and saved counts flow into {@link DragonStats}.
 */
public final class DragonConfig {

    public static final DragonConfig INSTANCE = new DragonConfig();

    private Path file;

    // behaviour
    public boolean autoTrigger = true;   // play the reveal when a dragon dies
    public boolean requireEyes = true;   // ...only if we saw the summon first
    public boolean playSounds  = true;
    public double  animationSpeed = 1.0; // 0.5 (slow) .. 2.0 (fast)

    // odds
    public double uncommonToRare  = 0.50;
    public double rareToEpic      = 0.30;
    public double epicToLegendary = 0.15;
    public double luck            = 1.0;

    // stats
    public long statUncommon = 0L, statRare = 0L, statEpic = 0L, statLegendary = 0L;
    public long statTotal = 0L;
    public int  statBestOrdinal = -1;

    private DragonConfig() {}

    public void load(Path configDir) {
        try {
            if (!Files.isDirectory(configDir)) Files.createDirectories(configDir);
            this.file = configDir.resolve("dragonloot.properties");
        } catch (IOException e) {
            this.file = null;
        }
        Properties p = new Properties();
        if (file != null && Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                p.load(in);
            } catch (IOException ignored) { }
        }

        autoTrigger    = getBool(p, "autoTrigger", true);
        requireEyes    = getBool(p, "requireEyes", true);
        playSounds     = getBool(p, "playSounds", true);
        animationSpeed = clamp(getDouble(p, "animationSpeed", 1.0), 0.5, 2.0);

        uncommonToRare  = odd(getDouble(p, "uncommonToRare", 0.50));
        rareToEpic      = odd(getDouble(p, "rareToEpic", 0.30));
        epicToLegendary = odd(getDouble(p, "epicToLegendary", 0.15));
        luck            = clamp(getDouble(p, "luck", 1.0), 0.0, 10.0);

        statUncommon    = getLong(p, "statUncommon", 0);
        statRare        = getLong(p, "statRare", 0);
        statEpic        = getLong(p, "statEpic", 0);
        statLegendary   = getLong(p, "statLegendary", 0);
        statTotal       = getLong(p, "statTotal", 0);
        statBestOrdinal = (int) getLong(p, "statBestOrdinal", -1);

        applyToEngine();
        applyToStats();
        save();
    }

    public void applyToEngine() {
        RollEngine e = RollEngine.INSTANCE;
        e.setAdvanceChance(Rarity.UNCOMMON, uncommonToRare);
        e.setAdvanceChance(Rarity.RARE, rareToEpic);
        e.setAdvanceChance(Rarity.EPIC, epicToLegendary);
        e.setLuck(luck);
    }

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
        // Mirror odds into the engine even if there's no file to write.
        applyToEngine();
        if (file == null) return;
        Properties p = new Properties();
        p.setProperty("autoTrigger", Boolean.toString(autoTrigger));
        p.setProperty("requireEyes", Boolean.toString(requireEyes));
        p.setProperty("playSounds", Boolean.toString(playSounds));
        p.setProperty("animationSpeed", Double.toString(animationSpeed));
        p.setProperty("uncommonToRare", Double.toString(uncommonToRare));
        p.setProperty("rareToEpic", Double.toString(rareToEpic));
        p.setProperty("epicToLegendary", Double.toString(epicToLegendary));
        p.setProperty("luck", Double.toString(luck));
        p.setProperty("statUncommon", Long.toString(statUncommon));
        p.setProperty("statRare", Long.toString(statRare));
        p.setProperty("statEpic", Long.toString(statEpic));
        p.setProperty("statLegendary", Long.toString(statLegendary));
        p.setProperty("statTotal", Long.toString(statTotal));
        p.setProperty("statBestOrdinal", Integer.toString(statBestOrdinal));
        try (OutputStream out = Files.newOutputStream(file)) {
            p.store(out, "DragonLoot config + lifetime stats");
        } catch (IOException ignored) { }
    }

    // ---- helpers ----
    private static boolean getBool(Properties p, String k, boolean def) {
        String v = p.getProperty(k);
        return v == null ? def : Boolean.parseBoolean(v.trim());
    }
    private static double getDouble(Properties p, String k, double def) {
        try { return p.containsKey(k) ? Double.parseDouble(p.getProperty(k).trim()) : def; }
        catch (NumberFormatException e) { return def; }
    }
    private static long getLong(Properties p, String k, long def) {
        try { return p.containsKey(k) ? Long.parseLong(p.getProperty(k).trim()) : def; }
        catch (NumberFormatException e) { return def; }
    }
    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
    private static double odd(double v) {
        return clamp(v, 0.0, RollEngine.MAX_ADVANCE);
    }
}
