package com.treegifts.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Persistent Tree Gifts settings + lifetime stats, saved as a plain .properties
 * file. JDK-only (no Minecraft/Fabric types) so it stays testable and portable.
 *
 * Settings:
 *  - autoReveal      : reveal automatically on a real Tree Gift
 *  - minRarity       : only auto-reveal drops at least this rare (0=Common … 6=Special)
 *  - playSounds      : play the reveal sounds
 *  - particleScale   : 0..2 multiplier on particle/confetti amount
 *  - animationSpeed  : 0.5..2 speed multiplier for the reveal
 */
public final class TreeGiftConfig {

    public static final TreeGiftConfig INSTANCE = new TreeGiftConfig();

    private Path file;

    public boolean autoReveal    = true;
    public int     minRarity     = 0;    // Rarity ordinal threshold for auto-reveal
    public boolean playSounds    = true;
    public double  particleScale = 1.0;
    public double  animationSpeed = 1.0;

    // Lifetime stats (mirrored to/from TreeGiftStats).
    public long[] statCounts = new long[Rarity.values().length];
    public long   statTotal = 0L;
    public int    statBestOrdinal = -1;
    public String statRarestName = "";
    public double statRarestDrop = -1.0;

    private TreeGiftConfig() {}

    public Rarity minRarityValue() {
        int i = Math.max(0, Math.min(Rarity.values().length - 1, minRarity));
        return Rarity.values()[i];
    }

    public void load(Path configDir) {
        try {
            if (!Files.isDirectory(configDir)) Files.createDirectories(configDir);
            this.file = configDir.resolve("treegifts.properties");
        } catch (IOException e) {
            this.file = null;
        }
        Properties p = new Properties();
        if (file != null && Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) { p.load(in); }
            catch (IOException ignored) { }
        }

        autoReveal     = getBool(p, "autoReveal", true);
        minRarity      = (int) clamp(getLong(p, "minRarity", 0), 0, Rarity.values().length - 1);
        playSounds     = getBool(p, "playSounds", true);
        particleScale  = clamp(getDouble(p, "particleScale", 1.0), 0.0, 2.0);
        animationSpeed = clamp(getDouble(p, "animationSpeed", 1.0), 0.5, 2.0);

        long[] c = new long[Rarity.values().length];
        for (int i = 0; i < c.length; i++) c[i] = getLong(p, "stat_" + Rarity.values()[i].name(), 0);
        statCounts = c;
        statTotal = getLong(p, "statTotal", 0);
        statBestOrdinal = (int) getLong(p, "statBestOrdinal", -1);
        statRarestName = p.getProperty("statRarestName", "");
        statRarestDrop = getDouble(p, "statRarestDrop", -1.0);

        applyToStats();
        save();
    }

    public void applyToStats() {
        TreeGiftStats.INSTANCE.loadCounts(statCounts, statTotal, statBestOrdinal, statRarestName, statRarestDrop);
    }

    /** Pull the live stats back out and persist them. */
    public void saveStats() {
        TreeGiftStats s = TreeGiftStats.INSTANCE;
        statCounts = s.countsSnapshot();
        statTotal = s.total();
        statBestOrdinal = s.bestOrdinal();
        statRarestName = s.rarestName();
        statRarestDrop = s.rarestDrop();
        save();
    }

    public void save() {
        if (file == null) return;
        Properties p = new Properties();
        p.setProperty("autoReveal", Boolean.toString(autoReveal));
        p.setProperty("minRarity", Integer.toString(minRarity));
        p.setProperty("playSounds", Boolean.toString(playSounds));
        p.setProperty("particleScale", Double.toString(particleScale));
        p.setProperty("animationSpeed", Double.toString(animationSpeed));
        for (int i = 0; i < statCounts.length; i++) {
            p.setProperty("stat_" + Rarity.values()[i].name(), Long.toString(statCounts[i]));
        }
        p.setProperty("statTotal", Long.toString(statTotal));
        p.setProperty("statBestOrdinal", Integer.toString(statBestOrdinal));
        p.setProperty("statRarestName", statRarestName == null ? "" : statRarestName);
        p.setProperty("statRarestDrop", Double.toString(statRarestDrop));
        try (OutputStream out = Files.newOutputStream(file)) {
            p.store(out, "Tree Gifts config + lifetime stats");
        } catch (IOException ignored) { }
    }

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
}
