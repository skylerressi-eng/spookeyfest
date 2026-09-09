package com.dragonloot.core;

import java.util.Collections;
import java.util.List;

/**
 * The outcome of one egg gamble: the ordered list of tiers the egg cracked
 * through (always starting at {@link Rarity#UNCOMMON}) and the final tier it
 * broke open at. The reveal animation uses the final tier to know where the
 * slot reel lands and how far the egg cracks.
 */
public final class RollResult {

    private final List<Rarity> stages;
    private final Rarity finalRarity;

    public RollResult(List<Rarity> stages, Rarity finalRarity) {
        this.stages = Collections.unmodifiableList(stages);
        this.finalRarity = finalRarity;
    }

    public List<Rarity> stages() {
        return stages;
    }

    public Rarity finalRarity() {
        return finalRarity;
    }

    public int stageCount() {
        return stages.size();
    }

    @Override
    public String toString() {
        return "RollResult" + stages + " -> " + finalRarity;
    }
}
