package com.treegifts.core;

import java.util.Collections;
import java.util.List;

/**
 * The fully-decided outcome of one tree gift: the ordered beats to animate, the
 * rarity it ended on, whether it spun into a Mango Dye, and the reward.
 *
 * Immutable and Minecraft-free — produced by {@link TreeGiftRoller} and consumed
 * by the reveal screen (and by the tests).
 */
public final class GiftResult {

    public final List<GiftStep> steps;
    public final Rarity finalRarity;
    public final boolean mango;
    public final Loot loot;

    public GiftResult(List<GiftStep> steps, Rarity finalRarity, boolean mango, Loot loot) {
        this.steps = Collections.unmodifiableList(steps);
        this.finalRarity = finalRarity;
        this.mango = mango;
        this.loot = loot;
    }

    /** How many axes get thrown in this reveal (one per beat). */
    public int axeCount() {
        return steps.size();
    }

    /** The last beat — always a CRACK or a MANGO. */
    public GiftStep last() {
        return steps.get(steps.size() - 1);
    }
}
