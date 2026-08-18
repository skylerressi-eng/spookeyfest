package com.spookybirch.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The candy reference table shown in-game (/spooky candy) and used by the HUD.
 *
 * NUMBERS ARE APPROXIMATE, community-sourced averages. Tweak freely — the whole
 * mod reads from this one list, so editing here updates the HUD, the guide GUI
 * and the chat command all at once.
 */
public final class CandyData {

    private static final List<CandyMob> MOBS = new ArrayList<CandyMob>();

    static {
        // name                 where                    green/kill  purple%   note
        add("Wither Gourd",     "Hub (Spooky spawns)",   3.0,        0.020,    "Best value target during the fest");
        add("Scary Jerry",      "Hub (Spooky spawns)",   2.5,        0.015,    "Common, spawns in packs");
        add("Batty Witch",      "Hub (Spooky spawns)",   2.2,        0.012,    "Flies, easy to miss");
        add("Phantom Spirit",   "Hub (Spooky spawns)",   2.0,        0.010,    "Fast mover");
        add("Trick or Treater", "Hub (Spooky spawns)",   1.8,        0.010,    "Drops extra candy on final hit");
        add("Crazy Witch Cat",  "Hub (Spooky spawns)",   1.6,        0.008,    "Small hitbox");
        add("Scaredy Cat",      "Hub (Spooky spawns)",   1.4,        0.007,    "Runs away, AOE helps");
        add("Nightmare",        "Hub (night, rare)",     6.0,        0.060,    "Miniboss-tier, big candy dump");
        add("Werewolf",         "Hub / Spider Den night",4.0,        0.040,    "Only at night, hits hard");
        add("Zombie (event)",   "Any zone",              1.0,        0.004,    "Baseline mob, low but steady");
        add("Skeleton (event)", "Any zone",              1.0,        0.004,    "Baseline mob");
        add("Spider (event)",   "Any zone",              0.9,        0.004,    "Baseline mob");
        add("Enderman (event)", "The End / Hub",         1.2,        0.005,    "Teleports, annoying");
    }

    private static void add(String name, String where, double green, double purple, String note) {
        MOBS.add(new CandyMob(name, where, green, purple, note));
    }

    /** All mobs, unsorted. */
    public static List<CandyMob> all() {
        return Collections.unmodifiableList(MOBS);
    }

    /** Mobs ranked best-candy-first. */
    public static List<CandyMob> ranked() {
        List<CandyMob> copy = new ArrayList<CandyMob>(MOBS);
        Collections.sort(copy, new Comparator<CandyMob>() {
            public int compare(CandyMob a, CandyMob b) {
                return Double.compare(b.score(), a.score());
            }
        });
        return copy;
    }

    /** The single best candy mob, for the HUD tip line. */
    public static CandyMob best() {
        return ranked().get(0);
    }

    private CandyData() {}
}
