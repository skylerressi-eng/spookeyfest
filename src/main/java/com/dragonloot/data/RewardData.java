package com.dragonloot.data;

import com.dragonloot.core.Rarity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The cosmetic loot the egg reveals when it breaks open. Purely flavor — the
 * mod never touches your real inventory — but it makes the gamble feel like it
 * paid out. All Dragon's-Nest-themed so it fits the moment.
 *
 * Edit a row here and the reveal screen, {@code /dragonloot rewards} and the
 * guide GUI all update — nothing else hard-codes these strings. This mirrors
 * how {@code CandyData}/{@code FishingData} centralize their tables.
 */
public final class RewardData {

    private static final List<DragonReward> ALL = new ArrayList<DragonReward>();

    static {
        // Uncommon — small consolation drops.
        add(Rarity.UNCOMMON, "Dragon Horn",        "a little souvenir");
        add(Rarity.UNCOMMON, "Handful of Ender Pearls", "still useful");
        add(Rarity.UNCOMMON, "Dragon Scale",       "worth a few coins");

        // Rare — solid pulls.
        add(Rarity.RARE, "Aspect of the Dragons",  "the classic sword");
        add(Rarity.RARE, "Dragon Claw",            "a tidy chunk of coins");
        add(Rarity.RARE, "Ender Artifact",         "shiny and tradeable");

        // Epic — the good stuff.
        add(Rarity.EPIC, "Ender Dragon Pet (Epic)", "a loyal companion");
        add(Rarity.EPIC, "Dragon Armor Piece",      "part of a full set");
        add(Rarity.EPIC, "Old Dragon Fragment",     "for summoning the Old one");

        // Legendary — jackpot.
        add(Rarity.LEGENDARY, "Ender Dragon Pet (Legendary)", "the dream pull");
        add(Rarity.LEGENDARY, "Superior Dragon Armor",        "best in slot");
        add(Rarity.LEGENDARY, "Aspect of the Void",           "upgraded and glowing");
    }

    private static void add(Rarity r, String name, String note) {
        ALL.add(new DragonReward(r, name, note));
    }

    /** All rewards, in declaration order. */
    public static List<DragonReward> all() {
        return Collections.unmodifiableList(ALL);
    }

    /** Every reward for one rarity tier. */
    public static List<DragonReward> forRarity(Rarity r) {
        List<DragonReward> out = new ArrayList<DragonReward>();
        for (DragonReward d : ALL) if (d.rarity == r) out.add(d);
        return out;
    }

    /** Pick a random reward for the given tier (falls back to any if empty). */
    public static DragonReward pick(Rarity r, Random rng) {
        List<DragonReward> pool = forRarity(r);
        if (pool.isEmpty()) pool = ALL;
        return pool.get(rng.nextInt(pool.size()));
    }
}
