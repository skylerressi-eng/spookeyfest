# Blacksmith reforge research (Sept 2026)

Research backing `com.spookybirch.reforge.ReforgeData`. The official Hypixel Wiki
was discontinued in **July 2026**, so this was verified against the current
community wiki fork (`hypixelskyblock.minecraft.wiki`) plus the Hypixel forums,
cross-checked across the per-category reforge pages.

## How the Blacksmith works

- **Basic reforges** are applied at the Blacksmith / a Reforge Anvil via the
  **Apply Reforge** button. The reforge you get is **random** and **all basic
  reforges in the item's pool have an equal chance**.
- The first basic reforge on an item costs **10 Coal**; after that it costs
  **Coins scaled by the item's rarity**.
- **Reforge Stones** are a *separate* system (applied through a Reforge Anvil /
  Malik) and are **never** produced by the random Apply Reforge button.

The mod's cinematic is for the **basic** system only. Reforge-stone reforges are
deliberately excluded from the fake-out pools so an intermediate never shows a
reforge the button can't actually produce. (If the *real* result read back from
the item happens to be a stone reforge, it still reveals correctly — the final
reveal is always whatever the server actually wrote.)

## The item category drives the pool

Possible reforges depend on the item's category — the mod must **not** use one
universal list. Categories and their **current basic Blacksmith pools**:

| Category (in code) | Item types | Basic reforges |
|---|---|---|
| `SWORD` | Swords, Fishing Rods, Wands, Gauntlets (shared pool) | Gentle, Odd, Fast, Fair, Epic, Sharp, Heroic, Spicy, Legendary |
| `BOW` | Bows / ranged weapons | Deadly, Fine, Grand, Hasty, Neat, Rapid, Unreal, Awkward, Rich |
| `ARMOR` | Helmet, Chestplate, Leggings, Boots | Clean, Fierce, Heavy, Light, Mythic, Pure, Smart, Titanic, Wise |
| `EQUIPMENT` | Necklace, Cloak, Belt, Gloves, Bracelet | Stained, Menacing, Hefty, Soft, Honored, Blended, Astute, Colossal, Brilliant |
| `AXE` | Axes | Double-Bit, Lumberjack's, Great, Rugged, Lush |
| `HOE` | Hoes | Green Thumb, Peasant's, Robust, Zooming |
| `PICKAXE` | Pickaxes, Drills, Shovels | Unyielding, Prospector's, Excellent, Sturdy, Fortunate |

Basic reforges apply regardless of the item's **rarity** (rarity only changes the
stat magnitude and the coin cost), so the pool is chosen by category alone.

## Reforge-stone-only reforges (NOT in the random pool)

For completeness, these are stone-only and are excluded from the fake-out pools:

- **Sword:** Dirty, Fabled, Suspicious, Gilded, Warped, Withered, Bulky, Fanged, Coldfused
- **Fishing rod:** Salty, Treacherous, Stiff, Lucky, Pitchin', Chomp
- **Bow:** Precise, Spiritual, Headstrong
- **Armor:** Perfect, Necrotic, Ancient, Spiked, Renowned, Cubic, Hyper, Reinforced, Loving, Ridiculous, Empowered, Giant, Undead, Submerged, Jaded, Bustling, Mossy, Mantid, Festive, Groovy
- **Equipment:** Waxed, Fortified, Strengthened, Glistening, Blooming, Rooted, Snowy, Royal, Blood-Soaked, Bloodshot, Blazing, Marshy
- **Axe:** Moil, Toil
- **Hoe:** Deep Fried, Blessed, Bountiful, Overpriced
- **Pickaxe/Drill:** Magnetic, Fruitful, Refined, Stellar, Mithraic, Auspicious, Fleet, Heated, Ambered, Scraped, Glacial

> Note: the famous old names **Godly** and **Strong** are now **reforge-stone**
> reforges (Dragon Claw / Dragon Horn), which is exactly why the pools above are
> not hard-coded to "Sharp / Spicy / Godly / Strong".

## How the mod reads the authoritative result

Hypixel stores the applied reforge in the item's NBT at
`tag.ExtraAttributes.modifier` (e.g. `"spicy"`, `"sharp"`) — the internal id.
The mod watches the item sitting in the reforge GUI slot and, when that
`modifier` **changes on the same item** (identity keyed on `ExtraAttributes.uuid`,
falling back to `ExtraAttributes.id`), it treats the new value as the real
result and plays the cinematic ending on it. The item's category is read from
its last lore line (e.g. `LEGENDARY SWORD`, `EPIC DRILL 3000`, `MYTHIC NECKLACE`).

**The mod never sends a packet, never applies a reforge, never touches coins or
inventory.** It only observes and re-presents the server's own result.

## Sources

- [Reforging — community Hypixel SkyBlock Wiki fork](https://hypixelskyblock.minecraft.wiki/w/Reforging)
- [Reforging/Sword and Fishing Rod](https://hypixelskyblock.minecraft.wiki/w/Reforging/Sword_and_Fishing_Rod)
- [Reforging/Ranged Weapon](https://hypixelskyblock.minecraft.wiki/w/Reforging/Ranged_Weapon)
- [Reforging/Armor](https://hypixelskyblock.minecraft.wiki/w/Reforging/Armor)
- [Reforging/Tool](https://hypixelskyblock.minecraft.wiki/w/Reforging/Tool)
- [Reforging/Equipment](https://hypixelskyblock.minecraft.wiki/w/Reforging/Equipment)
