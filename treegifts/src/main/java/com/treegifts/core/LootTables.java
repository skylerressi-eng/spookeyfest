package com.treegifts.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * What each rarity of cracked wood pays out — plus the Mango Dye jackpot.
 *
 * A tier's table is picked from uniformly; the "amount" for money-ish rewards
 * scales with rarity so a Legendary crack always feels bigger than a Common one.
 * Every reward's colour is its rarity colour (Mango gets its own warm orange).
 *
 * Pure Java: the tables are just lists, and the RNG is injected, so payouts are
 * deterministic under a seeded {@link Random} in tests.
 */
public final class LootTables {

    /** The Mango Dye's signature warm colour (not a rarity colour). */
    public static final int MANGO_COLOR = 0xFFFFB331;

    private LootTables() {}

    /** Roll one reward for a wood that cracked open at {@code rarity}. */
    public static Loot forRarity(Rarity rarity, Random rng) {
        List<Loot> table = tableFor(rarity);
        return table.get(rng.nextInt(table.size()));
    }

    /** The Mango Dye jackpot — only reachable by spinning a Legendary. */
    public static Loot mango(Random rng) {
        // A tiny bit of amount variety so two mangos don't read identically.
        int amount = 1 + rng.nextInt(2); // 1–2
        return new Loot("Mango Dye", "minecraft:orange_dye", MANGO_COLOR, amount,
                "The one-in-a-thousand spin. Frame it.");
    }

    private static List<Loot> tableFor(Rarity r) {
        List<Loot> t = new ArrayList<Loot>();
        int c = r.color;
        switch (r) {
            case COMMON:
                t.add(new Loot("Hardened Wood", "minecraft:oak_log", c, 16, "Solid start. Chop another."));
                t.add(new Loot("Oak Wood", "minecraft:oak_planks", c, 32, "Bread-and-butter timber."));
                t.add(new Loot("Wheat", "minecraft:wheat", c, 24, "Not wood, but it'll feed the pet."));
                break;
            case UNCOMMON:
                t.add(new Loot("Hardened Wood", "minecraft:stripped_oak_log", c, 48, "A tidy bundle."));
                t.add(new Loot("Enchanted Bread", "minecraft:bread", c, 4, "A little baker's XP on the side."));
                t.add(new Loot("Jungle Sapling", "minecraft:jungle_sapling", c, 8, "Plant it, chop it, repeat."));
                break;
            case RARE:
                t.add(new Loot("Enchanted Wood", "minecraft:enchanted_book", c, 2, "160 wood, compressed."));
                t.add(new Loot("SkyBlock Coins", "minecraft:emerald", c, 5000, "Coin purse says thank you."));
                t.add(new Loot("Sapling (Pet)", "minecraft:oak_sapling", c, 1, "A leafy little companion."));
                break;
            case EPIC:
                t.add(new Loot("Enchanted Wood", "minecraft:enchanted_book", c, 8, "A whole chest of it."));
                t.add(new Loot("Treecapitator", "minecraft:diamond_axe", c, 1, "Fells the whole tree in one swing."));
                t.add(new Loot("SkyBlock Coins", "minecraft:emerald", c, 50000, "Now we're talking."));
                break;
            case LEGENDARY:
            default:
                t.add(new Loot("Jungle Key", "minecraft:tripwire_hook", c, 1, "Opens the good chest."));
                t.add(new Loot("Enchanted Wood x64", "minecraft:enchanted_book", c, 64, "A vault of timber."));
                t.add(new Loot("SkyBlock Coins", "minecraft:gold_ingot", c, 500000, "Half a million. Legendary."));
                t.add(new Loot("Monkey (Pet)", "minecraft:cocoa_beans", c, 1, "+foraging fortune, +vibes."));
                break;
        }
        return t;
    }
}
