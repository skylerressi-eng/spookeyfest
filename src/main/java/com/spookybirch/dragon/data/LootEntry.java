package com.spookybirch.dragon.data;

/**
 * One item that can appear on a dragon's roulette wheel.
 *
 * This is a pure data holder — it describes an item for display and carries the
 * <i>verified</i> Dragon Weight requirement and drop-note text where those are
 * known. Nothing here is a fabricated probability: {@link #dropInfo} is the
 * game's own wording ("0-24%", "30%", "Dungeons"…) and {@link #weightReq} uses
 * {@link #UNKNOWN} when a value could not be verified.
 */
public class LootEntry {

    /** Sentinel for a weight requirement that is not applicable / not gated. */
    public static final int NONE = 0;
    /** Sentinel for a weight requirement we could not verify. */
    public static final int UNKNOWN = -1;

    /** Broad category, used to pick the wheel glyph and grouping. */
    public enum Category { ARMOR, WEAPON, PET, FRAGMENT, SPECIAL, COMMON }

    public final String name;       // full display name
    public final String shortName;  // abbreviated label for the wheel segment
    public final LootRarity rarity;
    public final Category category;
    /** Dragon Weight needed to be eligible; NONE, UNKNOWN or a verified value. */
    public final int weightReq;
    /** Verbatim drop-note from the source (e.g. "0-24%", "30%", "Dungeons"). */
    public final String dropInfo;

    public LootEntry(String name, String shortName, LootRarity rarity, Category category,
                     int weightReq, String dropInfo) {
        this.name = name;
        this.shortName = shortName;
        this.rarity = rarity;
        this.category = category;
        this.weightReq = weightReq;
        this.dropInfo = dropInfo;
    }

    /** Human-readable weight requirement for the info panels. */
    public String weightLabel() {
        if (weightReq == UNKNOWN) return "UNKNOWN";
        if (weightReq == NONE) return "—";
        return Integer.toString(weightReq);
    }
}
