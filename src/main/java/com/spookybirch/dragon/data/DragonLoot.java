package com.spookybirch.dragon.data;

import java.util.Collections;
import java.util.List;

/**
 * The immutable loot pool for a single {@link DragonType} — the set of items
 * shown around its roulette wheel. Built once by {@link DragonLootData} and
 * cached, so screens never rebuild it per frame.
 */
public final class DragonLoot {

    public final DragonType type;
    private final List<LootEntry> entries;

    DragonLoot(DragonType type, List<LootEntry> entries) {
        this.type = type;
        this.entries = Collections.unmodifiableList(entries);
    }

    /** The wheel segments, in wheel order. */
    public List<LootEntry> entries() {
        return entries;
    }

    public int size() {
        return entries.size();
    }

    public LootEntry get(int index) {
        return entries.get(index);
    }
}
