package com.treegifts.core;

/**
 * One reward a cracked (or spun) tree gift pays out.
 *
 * {@link #itemId} is a <i>vanilla</i> item identifier used only as a render
 * stand-in in the reveal animation (SkyBlock items don't exist client-side), while
 * {@link #name} / {@link #color} / {@link #flavor} carry the SkyBlock flavour.
 *
 * Pure data holder — no Minecraft dependency.
 */
public final class Loot {

    public final String name;    // e.g. "Enchanted Wood"
    public final String itemId;  // vanilla render stand-in, e.g. "minecraft:emerald"
    public final int color;      // packed ARGB for the label (usually the rarity colour)
    public final int amount;     // stack / quantity to show ("x64")
    public final String flavor;  // one-line description shown under the reward

    public Loot(String name, String itemId, int color, int amount, String flavor) {
        this.name = name;
        this.itemId = itemId;
        this.color = color;
        this.amount = amount;
        this.flavor = flavor;
    }
}
