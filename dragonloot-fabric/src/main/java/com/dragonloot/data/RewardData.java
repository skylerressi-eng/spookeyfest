package com.dragonloot.data;

import com.dragonloot.core.Rarity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The cosmetic loot the egg reveals when it breaks open. Purely flavor — the mod
 * never touches your real inventory — but it makes the gamble feel like it paid
 * out.
 *
 * Loot placement (per request):
 *   LEGENDARY  = ONLY the two ender pets + the Pearlescent Dye — the top prizes.
 *   EPIC/RARE/UNCOMMON = everything else, sorted by roughly how good it is.
 *
 * Edit a row and the reveal screen, /dragonloot rewards and the guide all update.
 */
public final class RewardData {

    private static final List<DragonReward> ALL = new ArrayList<DragonReward>();

    static {
        // Uncommon — the small consolation drops.
        add(Rarity.UNCOMMON, "Dragon Horn",             "a little souvenir");
        add(Rarity.UNCOMMON, "Dragon Scale",            "worth a few coins");
        add(Rarity.UNCOMMON, "Handful of Ender Pearls", "always handy");
        add(Rarity.UNCOMMON, "Dragon Fragment",         "chip toward a summon");

        // Rare — solid pulls.
        add(Rarity.RARE, "Aspect of the Dragons", "the classic sword");
        add(Rarity.RARE, "Dragon Claw",           "a tidy chunk of coins");
        add(Rarity.RARE, "Strong Dragon Armor",   "a dragon set piece");

        // Epic — the really good stuff.
        add(Rarity.EPIC, "Aspect of the Void",     "the upgraded blade");
        add(Rarity.EPIC, "Superior Dragon Armor",  "best dragon set piece");
        add(Rarity.EPIC, "Ender Relic",            "shiny and tradeable");

        // Legendary — ONLY these three. The jackpots.
        add(Rarity.LEGENDARY, "Ender Dragon Pet",  "the dream companion");
        add(Rarity.LEGENDARY, "Enderman Pet",      "a loyal ender friend");
        add(Rarity.LEGENDARY, "Pearlescent Dye",   "ultra-rare cosmetic");
    }

    private static void add(Rarity r, String name, String note) {
        ALL.add(new DragonReward(r, name, note));
    }

    public static List<DragonReward> all() {
        return Collections.unmodifiableList(ALL);
    }

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
