package com.dragonloot.core;

/**
 * The four rarity tiers an egg can gamble up to, in ascending order. The egg
 * always starts cracking at {@link #UNCOMMON} and can escalate one tier at a
 * time: UNCOMMON -> RARE -> EPIC -> LEGENDARY.
 *
 * Deliberately dependency-free: it holds only a display name, a Minecraft "§x"
 * chat color, an ARGB color for the GUI, and how many cracks the egg shows at
 * this tier. That keeps the whole gambling engine unit-testable without a game.
 */
public enum Rarity {
    UNCOMMON ("Uncommon",  "§a", 0xFF55FF55, 2),
    RARE     ("Rare",      "§9", 0xFF5599FF, 4),
    EPIC     ("Epic",      "§5", 0xFFAA33FF, 7),
    LEGENDARY("Legendary", "§6", 0xFFFFAA00, 11);

    public final String displayName;
    public final String colorCode;
    /** ARGB color for GUI rendering (0xAARRGGBB). */
    public final int argb;
    /** How many cracks show on the egg once it reaches this tier. */
    public final int cracks;

    Rarity(String displayName, String colorCode, int argb, int cracks) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.argb = argb;
        this.cracks = cracks;
    }

    public Rarity next() {
        return this == LEGENDARY ? null : values()[ordinal() + 1];
    }

    public boolean isTop() {
        return this == LEGENDARY;
    }

    /** "§6Legendary" — the name painted in its own color, for chat. */
    public String colored() {
        return colorCode + displayName;
    }
}
