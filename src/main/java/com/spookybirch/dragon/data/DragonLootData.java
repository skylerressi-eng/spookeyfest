package com.spookybirch.dragon.data;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.spookybirch.dragon.data.LootEntry.Category;

/**
 * The single source of truth for Dragon's Nest loot pools.
 *
 * <h3>Sourcing &amp; accuracy</h3>
 * Data is transcribed from the community-maintained Hypixel SkyBlock Wiki
 * (hypixelskyblock.minecraft.wiki) as of September 2026. The former official
 * wiki (wiki.hypixel.net) was retired in July 2026, so it can no longer be used
 * as the "current official" source; this mirror is the maintained successor.
 *
 * Every {@link LootEntry#dropInfo} string is the source's own wording — no
 * probability is invented here. {@link LootEntry#weightReq} holds the verified
 * Dragon Weight thresholds (Helmet 325, Leggings 350, Chestplate 400, Boots 300,
 * AOTD / Claw / Horn / Scale / Pet 450, Travel Scroll 250); unverified values
 * use {@link LootEntry#UNKNOWN}, and pool/non-gated items use
 * {@link LootEntry#NONE}.
 *
 * <p>This models <b>only</b> the Ender Dragon <i>fight</i> loot (system A). It
 * deliberately does not model any "sacrifice altar" economy: Hypixel SkyBlock
 * summons dragons by placing Summoning Eyes at the Dragon's Nest — there is no
 * separate Draconic Altar sacrifice reward table in the game, so none is faked
 * here. Holy is included as a non-fight reference pool only.
 */
public final class DragonLootData {

    private static final Map<DragonType, DragonLoot> POOLS = new EnumMap<DragonType, DragonLoot>(DragonType.class);

    static {
        for (DragonType t : DragonType.values()) {
            POOLS.put(t, t == DragonType.HOLY ? buildHoly() : buildFightPool(t));
        }
    }

    /** The cached loot pool for a dragon. Never null. */
    public static DragonLoot pool(DragonType type) {
        return POOLS.get(type);
    }

    /**
     * A standard Ender Dragon fight pool for one of the seven summonable dragons.
     * Entries are ordered to interleave rarities for a pleasant wheel; wheel
     * landing is a fair spin, so order is purely cosmetic.
     */
    private static DragonLoot buildFightPool(DragonType t) {
        String d = t.display + " Dragon ";
        List<LootEntry> e = new ArrayList<LootEntry>();

        // --- Guaranteed / common pool (every eligible player) ---
        // "30%" per-piece is the verified drop note for armour when the weight
        // requirement is met (Strong Dragon Armour wiki; same rate for variants).
        e.add(new LootEntry(d + "Boots", "Boots", LootRarity.LEGENDARY, Category.ARMOR, 300, "30%"));
        e.add(new LootEntry(d + "Fragment", "Frag", LootRarity.EPIC, Category.FRAGMENT, LootEntry.NONE, "Pool"));
        e.add(new LootEntry("Aspect of the Dragons", "AOTD", LootRarity.LEGENDARY, Category.WEAPON, 450, "0-24%"));
        e.add(new LootEntry("Enchanted Ender Pearl", "E.Pearl", LootRarity.COMMON, Category.COMMON, LootEntry.NONE, "Everyone"));
        e.add(new LootEntry(d + "Helmet", "Helm", LootRarity.LEGENDARY, Category.ARMOR, 325, "30%"));
        e.add(new LootEntry("Dragon Claw", "Claw", LootRarity.EPIC, Category.WEAPON, 450, "0-16%"));

        // --- Dragon-specific special, placed mid-wheel where it exists ---
        addSpecial(e, t);

        e.add(new LootEntry(d + "Leggings", "Legs", LootRarity.LEGENDARY, Category.ARMOR, 350, "30%"));
        // Superior fights yield 10 Dragon Essence; the others 5.
        String essence = (t == DragonType.SUPERIOR) ? "x10" : "x5";
        e.add(new LootEntry("Dragon Essence", "Essence", LootRarity.COMMON, Category.COMMON, LootEntry.NONE, essence));
        e.add(new LootEntry("Ender Dragon Pet", "Pet", LootRarity.EPIC, Category.PET, 450, "0-0.4%"));
        e.add(new LootEntry(d + "Chestplate", "Chest", LootRarity.LEGENDARY, Category.ARMOR, 400, "30%"));
        e.add(new LootEntry("Summoning Eye", "Eye", LootRarity.RARE, Category.SPECIAL, LootEntry.UNKNOWN, "Rare"));
        e.add(new LootEntry("Ender Dragon Pet", "Pet+", LootRarity.LEGENDARY, Category.PET, 450, "0-0.08%"));
        e.add(new LootEntry("Pearlescent Dye", "Dye", LootRarity.MYTHIC, Category.SPECIAL, LootEntry.UNKNOWN, "~0.001%"));

        return new DragonLoot(t, e);
    }

    private static void addSpecial(List<LootEntry> e, DragonType t) {
        switch (t) {
            case YOUNG:
                e.add(new LootEntry("Dragon Scale", "Scale", LootRarity.EPIC, Category.SPECIAL, 450, "30%"));
                break;
            case UNSTABLE:
                e.add(new LootEntry("Travel Scroll (Dragon's Nest)", "Scroll", LootRarity.RARE, Category.SPECIAL, 250, "35%"));
                break;
            case SUPERIOR:
                e.add(new LootEntry("Dragon Horn", "Horn", LootRarity.EPIC, Category.SPECIAL, 450, "30%"));
                break;
            default:
                break; // Protector / Old / Wise / Strong have no unique special
        }
    }

    /**
     * Holy reference pool. Holy Dragon Armour is <b>not</b> a fight drop — it is
     * dropped by Lost Adventurers in the Catacombs — so this pool is labelled
     * "Catacombs" and gated by no fight weight (NONE). It exists only so the UI
     * can display Holy gear; the simulator marks it non-summonable.
     */
    private static DragonLoot buildHoly() {
        List<LootEntry> e = new ArrayList<LootEntry>();
        e.add(new LootEntry("Holy Dragon Helmet", "Helm", LootRarity.LEGENDARY, Category.ARMOR, LootEntry.NONE, "Catacombs"));
        e.add(new LootEntry("Holy Dragon Chestplate", "Chest", LootRarity.LEGENDARY, Category.ARMOR, LootEntry.NONE, "Catacombs"));
        e.add(new LootEntry("Holy Dragon Leggings", "Legs", LootRarity.LEGENDARY, Category.ARMOR, LootEntry.NONE, "Catacombs"));
        e.add(new LootEntry("Holy Dragon Boots", "Boots", LootRarity.LEGENDARY, Category.ARMOR, LootEntry.NONE, "Catacombs"));
        e.add(new LootEntry("Holy Dragon Fragment", "Frag", LootRarity.EPIC, Category.FRAGMENT, LootEntry.NONE, "Crafting"));
        return new DragonLoot(DragonType.HOLY, e);
    }

    private DragonLootData() {}
}
