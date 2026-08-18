package com.spookybirch.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Spooky Festival fishing reference. During the event, fishing anywhere can hook
 * spooky sea creatures. Chances are community-sourced approximations — tune here.
 */
public final class FishingData {

    private static final List<SeaCreature> CREATURES = new ArrayList<SeaCreature>();

    static {
        // name              chance  drops                                  note
        add("Scarecrow",     0.300,  "Green Candy, string",                 "Most common spooky catch");
        add("Nightmare",     0.220,  "Green Candy, Nightmare pet chance",   "Rideable mount, rare pet drop");
        add("Werewolf",      0.200,  "Green Candy, leather",                "Higher HP, watch your rod");
        add("Phantom Fisher",0.150, "Green/Purple Candy, Fishing XP", "Good candy per catch");
        add("Grim Reaper",   0.130,  "Purple Candy, Reaper Mask chance",    "Rarest — best candy + gear");
    }

    private static void add(String name, double chance, String drops, String note) {
        CREATURES.add(new SeaCreature(name, chance, drops, note));
    }

    public static List<SeaCreature> all() {
        return Collections.unmodifiableList(CREATURES);
    }

    private FishingData() {}
}
