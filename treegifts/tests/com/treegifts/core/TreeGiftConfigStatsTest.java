package com.treegifts.core;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Pure-Java tests for the Tree Gift lifetime stats and the .properties config
 * (settings + stat persistence round-trip). No Minecraft needed.
 */
public class TreeGiftConfigStatsTest {

    static int passed = 0, failed = 0;

    public static void main(String[] args) throws Exception {
        testStats();
        testConfigRoundTrip();

        System.out.println("\n==================================");
        System.out.println("PASSED: " + passed + "   FAILED: " + failed);
        System.out.println("==================================");
        if (failed > 0) System.exit(1);
    }

    static void ok(boolean c, String n) {
        if (c) { passed++; System.out.println("  ok  - " + n); }
        else { failed++; System.out.println("  FAIL- " + n); }
    }

    static TreeGiftResult drop(String name, Rarity r, double pct) {
        return new TreeGiftResult(name, r, -1, pct, "Fig", false, ItemIcons.iconFor(name));
    }

    static void testStats() {
        System.out.println("[Stats]");
        TreeGiftStats s = TreeGiftStats.INSTANCE;
        s.reset();
        s.record(drop("Forest Essence", Rarity.COMMON, -1));
        s.record(drop("Stretching Sticks", Rarity.UNCOMMON, 20));
        s.record(drop("Tree the Fish", Rarity.SPECIAL, 0.05));
        s.record(drop("Phanpyre", Rarity.MYTHIC, 0.3));
        ok(s.total() == 4, "total counted");
        ok(s.count(Rarity.COMMON) == 1 && s.count(Rarity.SPECIAL) == 1, "per-rarity counts");
        ok(s.best() == Rarity.SPECIAL, "best rarity = Special");
        ok(Math.abs(s.share(Rarity.UNCOMMON) - 0.25) < 1e-9, "share = 25%");
        ok(s.rarestName().equals("Tree the Fish") && Math.abs(s.rarestDrop() - 0.05) < 1e-9, "rarest find tracked");
        s.reset();
        ok(s.total() == 0 && s.best() == null, "reset clears");
    }

    static void testConfigRoundTrip() throws Exception {
        System.out.println("[Config round-trip]");
        Path dir = Files.createTempDirectory("treegifts-test");
        TreeGiftConfig cfg = TreeGiftConfig.INSTANCE;

        cfg.load(dir);
        cfg.autoReveal = false;
        cfg.minRarity = Rarity.RARE.ordinal();
        cfg.playSounds = false;
        cfg.particleScale = 1.5;
        cfg.animationSpeed = 1.75;
        cfg.save();

        TreeGiftStats.INSTANCE.reset();
        TreeGiftStats.INSTANCE.record(drop("Dreadwing", Rarity.LEGENDARY, 1.2));
        TreeGiftStats.INSTANCE.record(drop("Tree the Fish", Rarity.SPECIAL, 0.05));
        cfg.saveStats();

        // Wipe live state, then reload from disk.
        TreeGiftStats.INSTANCE.reset();
        cfg.autoReveal = true; cfg.minRarity = 0; cfg.playSounds = true;
        cfg.load(dir);

        ok(!cfg.autoReveal, "autoReveal persisted (false)");
        ok(cfg.minRarity == Rarity.RARE.ordinal(), "minRarity persisted (Rare)");
        ok(!cfg.playSounds, "playSounds persisted (false)");
        ok(Math.abs(cfg.particleScale - 1.5) < 1e-9, "particleScale persisted");
        ok(Math.abs(cfg.animationSpeed - 1.75) < 1e-9, "animationSpeed persisted");
        ok(cfg.minRarityValue() == Rarity.RARE, "minRarityValue resolves");

        ok(TreeGiftStats.INSTANCE.total() == 2, "stats total persisted");
        ok(TreeGiftStats.INSTANCE.count(Rarity.SPECIAL) == 1, "stats per-rarity persisted");
        ok(TreeGiftStats.INSTANCE.best() == Rarity.SPECIAL, "stats best persisted");

        // clamp check
        cfg.animationSpeed = 99; cfg.particleScale = -5; cfg.minRarity = 99; cfg.save(); cfg.load(dir);
        ok(cfg.animationSpeed <= 2.0 && cfg.particleScale >= 0.0, "values clamped on reload");
        ok(cfg.minRarity <= Rarity.values().length - 1, "minRarity clamped");
    }
}
