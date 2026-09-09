package com.dragonloot.core;

import java.util.Collections;
import java.util.List;

/**
 * The outcome of one egg gamble: the ordered list of tiers the egg cracked
 * through (always starting at {@link Rarity#UNCOMMON}) and the final tier it
 * broke open at. The reveal animation walks {@link #stages} one crack at a time.
 */
public final class RollResult {

    private final List<Rarity> stages;
    private final Rarity finalRarity;

    public RollResult(List<Rarity> stages, Rarity finalRarity) {
        this.stages = Collections.unmodifiableList(stages);
        this.finalRarity = finalRarity;
    }

    /** Every tier the egg passed through, e.g. [UNCOMMON, RARE, EPIC]. */
    public List<Rarity> stages() {
        return stages;
    }

    /** The tier the egg broke open at (the last of {@link #stages}). */
    public Rarity finalRarity() {
        return finalRarity;
    }

    /** How many crack steps the animation plays (>= 1). */
    public int stageCount() {
        return stages.size();
    }

    @Override
    public String toString() {
        return "RollResult" + stages + " -> " + finalRarity;
    }
}
