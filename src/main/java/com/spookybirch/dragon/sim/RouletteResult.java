package com.spookybirch.dragon.sim;

import com.spookybirch.dragon.data.DragonType;
import com.spookybirch.dragon.data.LootEntry;

/**
 * The outcome of one spin: which segment the ball landed on, for which dragon.
 * Cosmetic only — it is explicitly a visual simulation, never a real reward.
 */
public final class RouletteResult {

    public final DragonType dragon;
    public final LootEntry entry;
    public final int segmentIndex;
    /** The seed that produced this result (for reproducibility / display). */
    public final long seed;

    public RouletteResult(DragonType dragon, LootEntry entry, int segmentIndex, long seed) {
        this.dragon = dragon;
        this.entry = entry;
        this.segmentIndex = segmentIndex;
        this.seed = seed;
    }
}
