package com.treegifts.core;

/**
 * The SkyBlock-style rarity ladder a tree gift climbs, one axe throw at a time.
 *
 * Each rarity carries its canonical SkyBlock display colour (packed ARGB) so the
 * reveal animation and any chat/HUD text stay consistent — Common white, all the
 * way up to Legendary gold.
 *
 * This enum has NO Minecraft dependency: the colours are plain ints, so the whole
 * roll engine can be unit-tested with pure Java.
 */
public enum Rarity {
    COMMON   ("Common",    0xFFFFFFFF),
    UNCOMMON ("Uncommon",  0xFF55FF55),
    RARE     ("Rare",      0xFF5555FF),
    EPIC     ("Epic",      0xFFAA00FF),
    LEGENDARY("Legendary", 0xFFFFAA00);

    public final String display;
    /** Packed ARGB (0xAARRGGBB), ready to hand straight to a text/fill call. */
    public final int color;

    Rarity(String display, int color) {
        this.display = display;
        this.color = color;
    }

    /** The next rarity up, or {@code this} if already at the top (Legendary). */
    public Rarity next() {
        Rarity[] all = values();
        int i = ordinal();
        return i + 1 < all.length ? all[i + 1] : this;
    }

    public boolean isMax() {
        return this == LEGENDARY;
    }
}
