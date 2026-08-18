package com.spookybirch.core;

/**
 * Live game state the HUD renders. Filled in by {@link StateUpdater} each tick.
 * Kept as one simple holder so the renderer never has to touch game internals.
 */
public final class SpookyState {

    public static final SpookyState INSTANCE = new SpookyState();

    // --- Mana (parsed from the SkyBlock action bar) ---
    public int mana = 0;
    public int maxMana = 0;
    public boolean manaKnown = false;

    // --- Candy (counted from inventory) ---
    public int greenCandy = 0;
    public int purpleCandy = 0;
    // Session baselines so we can show "gained this session".
    public int greenBaseline = -1;
    public int purpleBaseline = -1;

    // --- Event status (from scoreboard) ---
    public boolean festivalActive = false;
    public String festivalTimeLeft = "";   // e.g. "12:30" if we can read it
    public boolean inSkyblock = false;

    // --- Nearby spooky mobs (counted from loaded entities) ---
    public int nearbySpookyMobs = 0;

    public int greenGained() {
        return greenBaseline < 0 ? 0 : Math.max(0, greenCandy - greenBaseline);
    }

    public int purpleGained() {
        return purpleBaseline < 0 ? 0 : Math.max(0, purpleCandy - purpleBaseline);
    }

    public void resetSession() {
        greenBaseline = greenCandy;
        purpleBaseline = purpleCandy;
    }

    private SpookyState() {}
}
