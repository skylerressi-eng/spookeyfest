package com.dragonloot.data;

import com.dragonloot.core.Rarity;

/** One flavor reward the egg can hatch, tied to a rarity tier. */
public final class DragonReward {
    public final Rarity rarity;
    public final String name;
    public final String note;

    public DragonReward(Rarity rarity, String name, String note) {
        this.rarity = rarity;
        this.name = name;
        this.note = note;
    }
}
