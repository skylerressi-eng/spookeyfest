package com.treegifts.core;

/**
 * The REAL, server-decided Tree Gift drop, parsed from Hypixel's chat message.
 *
 * This is immutable and authoritative: the reveal animation may only *present*
 * this — it must never invent, re-roll, or override the item or rarity. There is
 * intentionally no randomness anywhere in this class.
 *
 * Pure data, no Minecraft dependency (so the parser + this can be unit-tested).
 */
public final class TreeGiftResult {

    /** Plain display name, e.g. "Stretching Sticks" or "Tree the Fish". */
    public final String itemName;
    /** Rarity, taken from the item name's chat colour (never guessed). */
    public final Rarity rarity;
    /** Quantity if the message stated one (e.g. Forest Essence x4), else -1. */
    public final int amount;
    /** Drop chance the message showed (e.g. 0.05 for 0.05%), or -1 if none. */
    public final double dropChancePercent;
    /** Tree pool this came from ("Fig"/"Mangrove"/"Helix"), or "" if unknown. */
    public final String treeType;
    /** True for the "A X fell from the Tree!" phantom drops. */
    public final boolean phantom;
    /** Vanilla item id used purely to render an icon in the reveal. */
    public final String iconItemId;

    public TreeGiftResult(String itemName, Rarity rarity, int amount, double dropChancePercent,
                          String treeType, boolean phantom, String iconItemId) {
        this.itemName = itemName;
        this.rarity = rarity;
        this.amount = amount;
        this.dropChancePercent = dropChancePercent;
        this.treeType = treeType == null ? "" : treeType;
        this.phantom = phantom;
        this.iconItemId = iconItemId;
    }

    public boolean hasAmount() { return amount > 0; }
    public boolean hasDropChance() { return dropChancePercent > 0; }

    @Override
    public String toString() {
        return "TreeGiftResult{" + itemName + " [" + rarity + "]"
                + (hasAmount() ? " x" + amount : "")
                + (hasDropChance() ? " (" + dropChancePercent + "%)" : "")
                + (treeType.isEmpty() ? "" : " from " + treeType)
                + (phantom ? " phantom" : "") + "}";
    }
}
