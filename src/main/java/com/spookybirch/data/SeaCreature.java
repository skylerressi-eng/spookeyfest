package com.spookybirch.data;

/** One spooky-fishing sea creature row for the /spooky fishing guide. */
public class SeaCreature {
    public final String name;
    public final double chance;   // approx catch chance (0-1) while spooky fishing
    public final String drops;    // notable drops
    public final String note;

    public SeaCreature(String name, double chance, String drops, String note) {
        this.name = name;
        this.chance = chance;
        this.drops = drops;
        this.note = note;
    }
}
