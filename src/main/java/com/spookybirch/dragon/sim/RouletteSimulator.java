package com.spookybirch.dragon.sim;

import java.util.Random;

import com.spookybirch.dragon.data.DragonLoot;

/**
 * Picks which wheel segment a spin lands on.
 *
 * <h3>Honesty note</h3>
 * This is a <b>fair, uniform</b> spin over the segments on the wheel — each
 * visible segment is equally likely. It is deliberately <i>not</i> weighted by
 * the game's real drop chances, because those chances are governed by the Dragon
 * Weight / quality system and are not published as simple per-item percentages.
 * Inventing such percentages would be faking accuracy, so we don't: the landing
 * is a transparent visual draw, and the result is always labelled a simulation.
 *
 * Deterministic when a seed is supplied; random otherwise.
 */
public final class RouletteSimulator {

    /**
     * Choose a landing segment index in [0, loot.size()).
     *
     * @param loot the pool being spun
     * @param seed an explicit seed, or a fresh random one if {@code null}
     * @return the chosen segment index
     */
    public static int pick(DragonLoot loot, Long seed) {
        int n = loot.size();
        if (n <= 0) return 0;
        Random rng = (seed != null) ? new Random(seed) : new Random();
        return rng.nextInt(n);
    }

    /** A fresh non-deterministic seed, so a "random" spin is still reproducible after the fact. */
    public static long freshSeed() {
        return new Random().nextLong();
    }

    private RouletteSimulator() {}
}
