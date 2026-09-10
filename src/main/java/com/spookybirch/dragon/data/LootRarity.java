package com.spookybirch.dragon.data;

/**
 * Item rarity tiers, matching Hypixel SkyBlock's colour language. Each tier
 * carries a base ARGB colour (used for text and segment tint) and a brighter
 * "glow" colour used for reveal effects, plus a {@link #intensity} 0-1 that the
 * result reveal uses to scale glow size, particles and sound drama.
 *
 * These are cosmetic display values only — they do not represent real drop
 * chances.
 */
public enum LootRarity {

    COMMON("Common", 0xFFBFBFBF, 0xFFFFFFFF, 0.15f),
    UNCOMMON("Uncommon", 0xFF55FF55, 0xFFB6FFB6, 0.30f),
    RARE("Rare", 0xFF5568FF, 0xFFA9B6FF, 0.50f),
    EPIC("Epic", 0xFFB24BFF, 0xFFE0B6FF, 0.72f),
    LEGENDARY("Legendary", 0xFFFFB114, 0xFFFFE39A, 0.90f),
    MYTHIC("Mythic", 0xFFFF64F0, 0xFFFFC4FA, 1.00f);

    public final String display;
    /** Solid colour for text / segment accent. */
    public final int color;
    /** Lighter colour for glow / highlight passes. */
    public final int glow;
    /** 0-1 drama factor for the reveal (glow radius, particles, sound). */
    public final float intensity;

    LootRarity(String display, int color, int glow, float intensity) {
        this.display = display;
        this.color = color;
        this.glow = glow;
        this.intensity = intensity;
    }
}
