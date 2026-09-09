package com.dragonloot.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The gambling engine. Given the current odds it rolls an egg up the rarity
 * ladder: the egg always starts cracking at {@link Rarity#UNCOMMON}, then at
 * each tier it either <b>cracks more</b> (advances one tier) or <b>breaks
 * open</b> (locks in at the current tier). {@link Rarity#LEGENDARY} is terminal.
 *
 * Forge-free and deterministic given a seeded {@link Random}, so it's fully
 * unit-testable (see {@code tests/.../DragonTest.java}). Config pushes the odds
 * in via {@link com.dragonloot.core.DragonLootConfig}, mirroring how
 * {@code SpookyConfig} feeds the candy tracker.
 */
public final class RollEngine {

    public static final RollEngine INSTANCE = new RollEngine();

    /** Hard ceiling on any advance chance so LEGENDARY can never be guaranteed. */
    public static final double MAX_ADVANCE = 0.98;

    // advance[i] = base P(advance from tier i to i+1), for i in UNCOMMON..EPIC.
    // Index by Rarity.ordinal(); LEGENDARY (last) has no advance slot.
    private final double[] advance = {0.50, 0.30, 0.15};

    // A global multiplier on the advance chances ("luck"). 1.0 = default odds;
    // higher makes big pulls more likely (clamped per step by MAX_ADVANCE).
    private double luck = 1.0;

    /** Set the base advance chance out of one tier. Clamped to [0, MAX_ADVANCE]. */
    public void setAdvanceChance(Rarity from, double chance) {
        if (from == null || from.isTop()) return;
        advance[from.ordinal()] = clamp(chance, 0.0, MAX_ADVANCE);
    }

    public double getAdvanceChance(Rarity from) {
        if (from == null || from.isTop()) return 0.0;
        return advance[from.ordinal()];
    }

    /** Luck multiplier applied on top of the base odds. Clamped to [0, 10]. */
    public void setLuck(double luck) {
        this.luck = clamp(luck, 0.0, 10.0);
    }

    public double getLuck() {
        return luck;
    }

    /** Effective (post-luck, post-clamp) chance to advance out of a tier. */
    public double effectiveAdvance(Rarity from) {
        if (from == null || from.isTop()) return 0.0;
        return clamp(advance[from.ordinal()] * luck, 0.0, MAX_ADVANCE);
    }

    /** Roll one egg. The result's stages always begin at UNCOMMON. */
    public RollResult roll(Random rng) {
        List<Rarity> stages = new ArrayList<Rarity>();
        Rarity cur = Rarity.UNCOMMON;
        stages.add(cur);
        while (!cur.isTop()) {
            if (rng.nextDouble() < effectiveAdvance(cur)) {
                cur = cur.next();      // crack more -> escalate a tier
                stages.add(cur);
            } else {
                break;                 // breaks open here
            }
        }
        return new RollResult(stages, cur);
    }

    /**
     * The theoretical probability of ending at each rarity under the current
     * odds, indexed by {@link Rarity#ordinal()}. Used by the odds GUI so players
     * can see what they're gambling against. Sums to 1.0.
     */
    public double[] finalProbabilities() {
        Rarity[] tiers = Rarity.values();
        double[] p = new double[tiers.length];
        double reached = 1.0; // probability of reaching the current tier
        for (int i = 0; i < tiers.length; i++) {
            Rarity t = tiers[i];
            if (t.isTop()) {
                p[i] = reached;                 // whatever reaches the top ends here
            } else {
                double adv = effectiveAdvance(t);
                p[i] = reached * (1.0 - adv);   // breaks open at this tier
                reached *= adv;                 // the rest crack onward
            }
        }
        return p;
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
