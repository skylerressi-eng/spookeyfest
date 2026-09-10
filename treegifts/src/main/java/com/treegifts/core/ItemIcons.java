package com.treegifts.core;

import java.util.Locale;

/**
 * Maps a real Tree Gift item's display name to a vanilla Minecraft item id used
 * ONLY to render an icon in the reveal (these SkyBlock items have no client-side
 * item, so we pick the closest-looking vanilla stand-in).
 *
 * This never affects the result — the name + rarity come from chat and are
 * authoritative. This is presentation only. Centralised on purpose (see the
 * project spec: keep the loot/render data in one place).
 *
 * The catalogue reflects the current (post-Foraging-rework) Tree Gift loot table
 * on Galatea: guaranteed items (Forest Essence, Foraging/HotF XP, Whispers,
 * Tender Wood / Vinesap) plus the rare "bonus gift" and phantom drops
 * (Stretching Sticks, Phanpyre/Phanflare/Dreadwing, Tree the Fish, boosters,
 * enchanted books, runes, pets, shards, ...). Unknown names fall back to a log.
 *
 * No Minecraft dependency — plain Strings, so it's testable.
 */
public final class ItemIcons {

    /** Fallback icon (also the Hardened Wood target stand-in). */
    public static final String HARDENED_WOOD = "minecraft:oak_log";

    private ItemIcons() {}

    /** Best vanilla icon id for a Tree Gift item display name. */
    public static String iconFor(String displayName) {
        if (displayName == null) return HARDENED_WOOD;
        String n = displayName.toLowerCase(Locale.ROOT);

        // Guaranteed / currency-ish
        if (n.contains("forest essence"))        return "minecraft:lime_dye";
        if (n.contains("desert whisper"))         return "minecraft:orange_dye";
        if (n.contains("whisper"))                return "minecraft:cyan_dye";
        if (n.contains("foraging experience") || n.contains("foraging xp")) return "minecraft:experience_bottle";
        if (n.contains("hotf") || n.contains("heart of the forest")) return "minecraft:nether_star";
        if (n.contains("tender wood"))            return "minecraft:stripped_oak_log";
        if (n.contains("vinesap"))                return "minecraft:slime_ball";
        if (n.contains("deep root"))              return "minecraft:hanging_roots";

        // Rare drops
        if (n.contains("stretching stick"))       return "minecraft:stick";
        if (n.contains("enchanted book"))         return "minecraft:enchanted_book";
        if (n.contains("sweep booster") || n.contains("wisdom booster") || n.contains("exp boost")
                || n.contains("booster"))         return "minecraft:paper";
        if (n.contains("signal enhancer"))        return "minecraft:redstone";
        if (n.contains("forest's favor") || n.contains("forests favor")) return "minecraft:golden_apple";

        // Phantoms / dragons
        if (n.contains("phanpyre") || n.contains("phanflare") || n.contains("dreadwing")) return "minecraft:phantom_membrane";

        // Fish / pets / shards
        if (n.contains("tree the fish") || n.contains("fish")) return "minecraft:cod";
        if (n.contains("shard"))                  return "minecraft:prismarine_shard";
        if (n.contains("chameleon"))              return "minecraft:lime_dye";
        if (n.contains("hummingbird"))            return "minecraft:feather";
        if (n.contains("grizzly") || n.contains("bear")) return "minecraft:honey_bottle";
        if (n.contains("firefox"))                return "minecraft:blaze_powder";
        if (n.contains("groundhog"))              return "minecraft:dirt";
        if (n.contains("drybark"))                return "minecraft:dead_bush";
        if (n.contains("puck"))                   return "minecraft:snowball";
        if (n.contains("rune"))                   return "minecraft:nether_star";
        if (n.contains("pet"))                    return "minecraft:bone";

        // Logs
        if (n.contains("enchanted") && n.contains("log")) return "minecraft:enchanted_book";
        if (n.contains("log") || n.contains("wood")) return "minecraft:oak_log";

        return HARDENED_WOOD;
    }
}
