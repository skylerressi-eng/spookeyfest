package com.treegifts.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The heart of the mod: rolls a whole tree-gift outcome up front.
 *
 * <pre>
 *   axe 1        reveal the wrapped wood as COMMON
 *   axe 2..n     each throw either UPGRADEs one rarity, or CRACKs open (payout)
 *   at LEGENDARY the throw either spins into a MANGO dye, or CRACKs for
 *                Legendary loot
 * </pre>
 *
 * The result is a list of {@link GiftStep} beats plus the {@link Loot}. The
 * renderer just plays the beats — all the chance lives here, behind an injected
 * {@link Random}, so a seeded roller is fully deterministic for tests.
 *
 * No Minecraft dependency.
 */
public final class TreeGiftRoller {

    private final GiftConfig config;
    private final Random rng;

    public TreeGiftRoller(GiftConfig config, Random rng) {
        if (config == null) config = GiftConfig.defaults();
        if (rng == null) rng = new Random();
        this.config = config;
        this.rng = rng;
    }

    /** Convenience: default odds with a fresh unseeded RNG. */
    public static TreeGiftRoller withDefaults() {
        return new TreeGiftRoller(GiftConfig.defaults(), new Random());
    }

    /** Roll one complete gift. Never returns null; always ends in a payout. */
    public GiftResult roll() {
        List<GiftStep> steps = new ArrayList<GiftStep>();

        // The first axe always reveals a Common wood.
        Rarity current = Rarity.COMMON;
        steps.add(GiftStep.reveal(current));

        while (true) {
            if (current.isMax()) {
                // Top of the ladder: spin into a mango, or crack for legendary loot.
                if (rng.nextDouble() < config.mango()) {
                    steps.add(GiftStep.mango());
                    return new GiftResult(steps, current, true, LootTables.mango(rng));
                }
                steps.add(GiftStep.crack(current));
                return new GiftResult(steps, current, false, LootTables.forRarity(current, rng));
            }

            if (rng.nextDouble() < config.upgradeChance(current)) {
                current = current.next();
                steps.add(GiftStep.upgrade(current));
            } else {
                steps.add(GiftStep.crack(current));
                return new GiftResult(steps, current, false, LootTables.forRarity(current, rng));
            }
        }
    }
}
