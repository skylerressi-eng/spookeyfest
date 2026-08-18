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
    public boolean onlyDuringFestival = false; // if true, hide HUD outside the fest

    public int hudX = 5;
    public int hudY = 5;
    public float hudScale = 1.0f;

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
        onlyDuringFestival = cfg.getBoolean("onlyDuringFestival", "hud", false, "Only show HUD while the fest is live");
        hudX     = cfg.getInt("hudX", "position", 5, 0, 10000, "HUD x pixel");
        hudY     = cfg.getInt("hudY", "position", 5, 0, 10000, "HUD y pixel");
        hudScale = (float) cfg.get("position", "hudScale", 1.0D, "HUD scale", 0.5D, 2.5D).getDouble();
        if (cfg.hasChanged()) cfg.save();
    }

    public void save() {
        if (cfg == null) return;
        cfg.get("hud", "hudEnabled", true).set(hudEnabled);
        cfg.get("hud", "showMana", true).set(showMana);
        cfg.get("hud", "showCandy", true).set(showCandy);
        cfg.get("hud", "showEvent", true).set(showEvent);
        cfg.get("hud", "showNearbyMobs", true).set(showNearbyMobs);
        cfg.get("hud", "showBestMobTip", true).set(showBestMobTip);
        cfg.get("hud", "onlyDuringFestival", false).set(onlyDuringFestival);
        cfg.get("position", "hudX", 5).set(hudX);
        cfg.get("position", "hudY", 5).set(hudY);
        cfg.get("position", "hudScale", 1.0D).set((double) hudScale);
        cfg.save();
    }

    private SpookyConfig() {}
}
