package com.treegifts.core;

/**
 * The SkyBlock rarity ladder, with each tier's canonical chat colour.
 *
 * On Hypixel, the §-colour of an item's name in chat <b>is</b> its rarity, so the
 * Tree Gift parser reads the rarity straight from the chat formatting via
 * {@link #fromColorCode(char)} — it never guesses from an item database.
 *
 * No Minecraft dependency: colours are plain ARGB ints and the legacy code is a
 * plain char, so the whole thing is unit-testable.
 */
public enum Rarity {
    COMMON   ("Common",    'f', 0xFFFFFFFF),
    UNCOMMON ("Uncommon",  'a', 0xFF55FF55),
    RARE     ("Rare",      '9', 0xFF5555FF),
    EPIC     ("Epic",      '5', 0xFFAA00FF),
    LEGENDARY("Legendary", '6', 0xFFFFAA00),
    MYTHIC   ("Mythic",    'd', 0xFFFF55FF),
    SPECIAL  ("Special",   'c', 0xFFFF5555);

    public final String display;
    /** The Minecraft legacy colour code (the char after §) this rarity uses. */
    public final char code;
    /** Packed ARGB (0xAARRGGBB), ready for a text/fill call. */
    public final int color;

    Rarity(String display, char code, int color) {
        this.display = display;
        this.code = code;
        this.color = color;
    }

    /** The next rarity up, or {@code this} at the top. (Legacy demo roller only.) */
    public Rarity next() {
        Rarity[] all = values();
        int i = ordinal();
        return i + 1 < all.length ? all[i + 1] : this;
    }

    /** Legacy demo helper: Legendary is the top of the old random ladder. */
    public boolean isMax() {
        return this == LEGENDARY;
    }

    /**
     * Map a Minecraft legacy colour code (the char after §) to the rarity it
     * represents on Hypixel. Gray/unknown colours fall back to COMMON so an
     * unrecognised drop still animates as a modest reveal rather than crashing.
     */
    public static Rarity fromColorCode(char c) {
        switch (Character.toLowerCase(c)) {
            case 'f': return COMMON;
            case 'a': return UNCOMMON;
            case '9': return RARE;
            case '5': return EPIC;
            case '6': return LEGENDARY;
            case 'd': return MYTHIC;
            case 'c': return SPECIAL;
            default:  return COMMON;
        }
    }

    /** How dramatic this rarity's reveal should be, 0.0 (Common) → 1.0 (Special). */
    public float drama() {
        return ordinal() / (float) (values().length - 1);
    }

    /** The display name prefixed with this rarity's legacy colour code. */
    public String colored() {
        return "§" + code + display;
    }

    /** Parse a rarity from a name/code (case-insensitive); null if unrecognised. */
    public static Rarity fromName(String s) {
        if (s == null) return null;
        String t = s.trim();
        for (Rarity r : values()) {
            if (r.name().equalsIgnoreCase(t) || r.display.equalsIgnoreCase(t)) return r;
        }
        if (t.length() == 1) return fromColorCode(t.charAt(0));
        return null;
    }
}
