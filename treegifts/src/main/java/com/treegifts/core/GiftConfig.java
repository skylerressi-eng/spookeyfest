package com.treegifts.core;

/**
 * The tunable odds behind a tree gift.
 *
 * The mechanic: the first axe reveals a Common wood. Every axe after that either
 * <b>upgrades</b> the wood one rarity higher, or the wood <b>cracks open</b> right
 * there and pays out at its current rarity. So "upgrade chance" is really "keep
 * climbing" — the complement is your chance to lock in the loot you've got.
 *
 * Once the wood is Legendary it can't climb further: instead there's a small
 * {@link #mangoChance} that the next throw makes it <i>spin</i> and transform into
 * a Mango Dye jackpot; otherwise it cracks for Legendary loot.
 *
 * Defaults are chosen so most gifts land Common–Rare, Epics feel good (~11%),
 * Legendaries are a real moment (~2.7%), and a Mango Dye is a genuine "no way"
 * (~0.13%). See {@code TreeGiftRollerTest} which asserts this whole distribution.
 *
 * Pure Java — no Minecraft dependency — so the odds are testable in isolation.
 */
public final class GiftConfig {

    /** Chance the next axe upgrades the wood instead of cracking it, per tier. */
    public double commonUpgrade   = 0.70;
    public double uncommonUpgrade = 0.55;
    public double rareUpgrade     = 0.35;
    public double epicUpgrade     = 0.20;

    /** Chance a Legendary wood spins into a Mango Dye instead of cracking. */
    public double mangoChance = 0.05;

    public static GiftConfig defaults() {
        return new GiftConfig();
    }

    /**
     * The chance the next throw keeps climbing from {@code from}. Legendary can't
     * climb, so it returns 0 (the mango roll is handled separately by the roller).
     */
    public double upgradeChance(Rarity from) {
        switch (from) {
            case COMMON:   return clamp(commonUpgrade);
            case UNCOMMON: return clamp(uncommonUpgrade);
            case RARE:     return clamp(rareUpgrade);
            case EPIC:     return clamp(epicUpgrade);
            default:       return 0.0; // LEGENDARY — top of the ladder
        }
    }

    public double mango() {
        return clamp(mangoChance);
    }

    private static double clamp(double p) {
        if (p < 0.0) return 0.0;
        if (p > 1.0) return 1.0;
        return p;
    }
}
