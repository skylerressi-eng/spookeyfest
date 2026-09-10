package com.spookybirch.core;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

/**
 * Persistent settings, stored in config/spookybirch.cfg. Controls which HUD
 * lines show and where the HUD sits on screen.
 */
public final class SpookyConfig {

    public static final SpookyConfig INSTANCE = new SpookyConfig();

    private Configuration cfg;

    public boolean hudEnabled = true;
    public boolean showMana = true;
    public boolean showCandy = true;
    public boolean showEvent = true;
    public boolean showNearbyMobs = true;
    public boolean showBestMobTip = true;
    public boolean showScore = true;   // candy score + session rate
    public boolean showEta = true;     // ETA to the candy goal
    public boolean onlyDuringFestival = false; // if true, hide HUD outside the fest

    // Score tuning: how much one purple candy is worth in "score", and an
    // optional session goal the tracker estimates an ETA toward (0 = no goal).
    public double purpleWeight = 8.0;
    public int candyGoal = 0;

    public int hudX = 5;
    public int hudY = 5;
    public float hudScale = 1.0f;

    // --- Dragon Altar Roulette (cosmetic simulation) ---
    public double dragonAnimSpeed = 1.0;        // spin speed multiplier
    public double dragonParticleIntensity = 1.0; // reveal particle amount
    public boolean dragonSound = true;          // reveal sound effects
    public boolean dragonRareEffects = true;    // extra glow/pulse for rare results
    public double dragonWheelScale = 1.0;       // wheel size multiplier
    public boolean dragonShowWeights = true;    // show Dragon Weight requirements
    public boolean dragonShowPercents = true;   // show drop-note text
    public boolean dragonReducedMotion = false; // shorter, calmer animation

    public void load(File dir) {
        File file = new File(dir, "spookybirch.cfg");
        cfg = new Configuration(file);
        read();
    }

    private void read() {
        cfg.load();
        hudEnabled       = cfg.getBoolean("hudEnabled", "hud", true, "Master toggle for the overlay");
        showMana         = cfg.getBoolean("showMana", "hud", true, "Show mana line");
        showCandy        = cfg.getBoolean("showCandy", "hud", true, "Show candy counters");
        showEvent        = cfg.getBoolean("showEvent", "hud", true, "Show festival status/time");
        showNearbyMobs   = cfg.getBoolean("showNearbyMobs", "hud", true, "Show nearby spooky mob count");
        showBestMobTip   = cfg.getBoolean("showBestMobTip", "hud", true, "Show best-candy-mob tip line");
        showScore        = cfg.getBoolean("showScore", "hud", true, "Show candy score + rate line");
        showEta          = cfg.getBoolean("showEta", "hud", true, "Show ETA to the candy goal");
        onlyDuringFestival = cfg.getBoolean("onlyDuringFestival", "hud", false, "Only show HUD while the fest is live");
        purpleWeight = cfg.get("score", "purpleWeight", 8.0D, "Score value of one purple candy (green = 1)", 1.0D, 100.0D).getDouble();
        candyGoal    = cfg.getInt("candyGoal", "score", 0, 0, 10_000_000, "Candy score goal for ETA (0 = off)");
        hudX     = cfg.getInt("hudX", "position", 5, 0, 10000, "HUD x pixel");
        hudY     = cfg.getInt("hudY", "position", 5, 0, 10000, "HUD y pixel");
        hudScale = (float) cfg.get("position", "hudScale", 1.0D, "HUD scale", 0.5D, 2.5D).getDouble();

        dragonAnimSpeed        = cfg.get("dragon", "animSpeed", 1.0D, "Roulette spin speed multiplier", 0.25D, 3.0D).getDouble();
        dragonParticleIntensity= cfg.get("dragon", "particleIntensity", 1.0D, "Reveal particle amount", 0.0D, 2.0D).getDouble();
        dragonSound            = cfg.getBoolean("dragonSound", "dragon", true, "Play the result reveal sound");
        dragonRareEffects      = cfg.getBoolean("dragonRareEffects", "dragon", true, "Extra glow/pulse for rare results");
        dragonWheelScale       = cfg.get("dragon", "wheelScale", 1.0D, "Roulette wheel size multiplier", 0.5D, 2.0D).getDouble();
        dragonShowWeights      = cfg.getBoolean("dragonShowWeights", "dragon", true, "Show Dragon Weight requirements");
        dragonShowPercents     = cfg.getBoolean("dragonShowPercents", "dragon", true, "Show drop-note text");
        dragonReducedMotion    = cfg.getBoolean("dragonReducedMotion", "dragon", false, "Shorter, calmer animation");
        if (cfg.hasChanged()) cfg.save();
        applyToTracker();
    }

    /** Push score-affecting settings into the (Forge-free) tracker. */
    public void applyToTracker() {
        CandyTracker.INSTANCE.setPurpleWeight(purpleWeight);
        CandyTracker.INSTANCE.setGoal(candyGoal);
    }

    public void save() {
        if (cfg == null) return;
        cfg.get("hud", "hudEnabled", true).set(hudEnabled);
        cfg.get("hud", "showMana", true).set(showMana);
        cfg.get("hud", "showCandy", true).set(showCandy);
        cfg.get("hud", "showEvent", true).set(showEvent);
        cfg.get("hud", "showNearbyMobs", true).set(showNearbyMobs);
        cfg.get("hud", "showBestMobTip", true).set(showBestMobTip);
        cfg.get("hud", "showScore", true).set(showScore);
        cfg.get("hud", "showEta", true).set(showEta);
        cfg.get("hud", "onlyDuringFestival", false).set(onlyDuringFestival);
        cfg.get("score", "purpleWeight", 8.0D).set(purpleWeight);
        cfg.get("score", "candyGoal", 0).set(candyGoal);
        applyToTracker();
        cfg.get("position", "hudX", 5).set(hudX);
        cfg.get("position", "hudY", 5).set(hudY);
        cfg.get("position", "hudScale", 1.0D).set((double) hudScale);
        cfg.get("dragon", "animSpeed", 1.0D).set(dragonAnimSpeed);
        cfg.get("dragon", "particleIntensity", 1.0D).set(dragonParticleIntensity);
        cfg.get("dragon", "dragonSound", true).set(dragonSound);
        cfg.get("dragon", "dragonRareEffects", true).set(dragonRareEffects);
        cfg.get("dragon", "wheelScale", 1.0D).set(dragonWheelScale);
        cfg.get("dragon", "dragonShowWeights", true).set(dragonShowWeights);
        cfg.get("dragon", "dragonShowPercents", true).set(dragonShowPercents);
        cfg.get("dragon", "dragonReducedMotion", false).set(dragonReducedMotion);
        cfg.save();
    }

    private SpookyConfig() {}
}
