package com.spookybirch.dragon.data;

/**
 * The Ender Dragon variants of the Dragon's Nest (The End).
 *
 * IMPORTANT ACCURACY NOTE: Hypixel SkyBlock has only <b>seven summonable</b>
 * dragon variants — Protector, Old, Wise, Young, Strong, Unstable and Superior.
 * There is <b>no Holy Dragon</b>: Holy Dragon Armour is dropped by Lost
 * Adventurers in the Catacombs (Dungeons), not from a dragon fight. Holy is kept
 * here only as a labelled reference pool so the UI can show its gear, and is
 * flagged {@link #summonable} = false so the simulation never treats it as a
 * real fight.
 *
 * Accent colours are cosmetic, chosen to fit the mod's purple/blue/gold theme.
 */
public enum DragonType {

    PROTECTOR("Protector", 0xFF3AC7A8, true,
            "Defensive — grants bonus true defence."),
    OLD("Old", 0xFFC9B458, true,
            "Old Blood boosts core enchantment strength."),
    WISE("Wise", 0xFF4FC3F7, true,
            "Wise Blood — abilities cost 33% less mana."),
    YOUNG("Young", 0xFF7CFF5A, true,
            "Young Blood — +70% speed above 50% HP."),
    STRONG("Strong", 0xFFFF6B4A, true,
            "Strong Blood adds crit damage & strength."),
    UNSTABLE("Unstable", 0xFFD16BFF, true,
            "Unstable Blood — chance for extra crit damage."),
    SUPERIOR("Superior", 0xFFFFC531, true,
            "Superior Blood — +5% to all stats."),
    HOLY("Holy", 0xFFF5E9B8, false,
            "Not a fight drop — Holy gear comes from the Catacombs.");

    public final String display;
    /** Cosmetic accent ARGB for this dragon's UI theme. */
    public final int accent;
    /** True for the seven real Ender Dragon variants; false for Holy. */
    public final boolean summonable;
    /** Short flavour line for the armour set's blood ability. */
    public final String blurb;

    DragonType(String display, int accent, boolean summonable, String blurb) {
        this.display = display;
        this.accent = accent;
        this.summonable = summonable;
        this.blurb = blurb;
    }

    /** The seven summonable variants, in a stable display order. */
    public static DragonType[] summonables() {
        return new DragonType[] {
                PROTECTOR, OLD, WISE, YOUNG, STRONG, UNSTABLE, SUPERIOR
        };
    }
}
