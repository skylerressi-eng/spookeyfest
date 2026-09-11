package com.spookybirch.reforge;

import com.spookybirch.util.TextUtil;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The reforge data registry — the single source of truth for the cinematic.
 *
 * <p>It knows the <b>current basic Blacksmith reforge pools</b> per item
 * category (verified Sept 2026 against the community SkyBlock wiki fork, since
 * the official wiki shut down in July 2026). These pools are used <b>only</b> to
 * pick believable <i>intermediate</i> reforges for the fake-out hits — the final
 * reforge shown is always the real one Hypixel wrote onto the item, read back
 * from its NBT ({@code ExtraAttributes.modifier}).
 *
 * <p>Reforge-stone reforges (Godly, Strong, Fabled, Necrotic, …) are
 * deliberately <b>excluded</b> from the pools: they are never produced by the
 * normal "Reforge Item" button, so showing one as an intermediate would look
 * fake. If the real result happens to be a stone reforge (someone used the anvil
 * with a stone), it still reveals correctly — we prettify whatever id we read.
 */
public final class ReforgeData {

    /** The item categories the Blacksmith basic-reforge system distinguishes. */
    public enum Category {
        SWORD, BOW, ARMOR, EQUIPMENT, AXE, HOE, PICKAXE;
    }

    // --- Basic Blacksmith reforge pools, by category (display names) ---------
    // Sword pool is shared with fishing rods / wands / gauntlets in-game.
    private static final List<String> SWORD = ro(
            "Gentle", "Odd", "Fast", "Fair", "Epic", "Sharp", "Heroic", "Spicy", "Legendary");
    private static final List<String> BOW = ro(
            "Deadly", "Fine", "Grand", "Hasty", "Neat", "Rapid", "Unreal", "Awkward", "Rich");
    private static final List<String> ARMOR = ro(
            "Clean", "Fierce", "Heavy", "Light", "Mythic", "Pure", "Smart", "Titanic", "Wise");
    private static final List<String> EQUIPMENT = ro(
            "Stained", "Menacing", "Hefty", "Soft", "Honored", "Blended", "Astute", "Colossal", "Brilliant");
    private static final List<String> AXE = ro(
            "Double-Bit", "Lumberjack's", "Great", "Rugged", "Lush");
    private static final List<String> HOE = ro(
            "Green Thumb", "Peasant's", "Robust", "Zooming");
    private static final List<String> PICKAXE = ro(
            "Unyielding", "Prospector's", "Excellent", "Sturdy", "Fortunate");

    /** id (as stored in {@code ExtraAttributes.modifier}) -> pretty display name. */
    private static final Map<String, String> ID_TO_NAME = buildIdMap();

    private ReforgeData() {}

    /** The believable-intermediate pool for a category. Never empty. */
    public static List<String> pool(Category c) {
        switch (c) {
            case BOW: return BOW;
            case ARMOR: return ARMOR;
            case EQUIPMENT: return EQUIPMENT;
            case AXE: return AXE;
            case HOE: return HOE;
            case PICKAXE: return PICKAXE;
            case SWORD:
            default: return SWORD;
        }
    }

    /**
     * The reforge id Hypixel wrote onto the item, or {@code null} if the stack
     * carries no reforge / isn't a reforgeable SkyBlock item. This is the
     * <b>authoritative</b> result — we never invent it.
     */
    public static String modifierId(ItemStack stack) {
        NBTTagCompound extra = extraAttributes(stack);
        if (extra == null) return null;
        String id = extra.getString("modifier");
        return (id == null || id.isEmpty()) ? null : id;
    }

    /**
     * A stable identity key for the item in the reforge slot, so we only treat a
     * modifier change on the <i>same</i> item as a reforge (not a slot swap).
     * Prefers the SkyBlock item UUID, falls back to the item id + name.
     */
    public static String identity(ItemStack stack) {
        if (stack == null) return null;
        NBTTagCompound extra = extraAttributes(stack);
        if (extra != null) {
            String uuid = extra.getString("uuid");
            if (uuid != null && !uuid.isEmpty()) return "uuid:" + uuid;
            // No per-item uuid (stackable gear): the SkyBlock id is stable across
            // a reforge, whereas the display name is not (its prefix changes), so
            // we key on the id alone to keep the same item's identity constant.
            String skyId = extra.getString("id");
            if (skyId != null && !skyId.isEmpty()) return "id:" + skyId;
        }
        return "name:" + TextUtil.stripColor(stack.getDisplayName());
    }

    /** Pretty display name for a reforge id (handles stone reforges too). */
    public static String displayName(String id) {
        if (id == null || id.isEmpty()) return "";
        String pretty = ID_TO_NAME.get(id.toLowerCase());
        return pretty != null ? pretty : prettify(id);
    }

    /**
     * Best-effort item category from the stack's lore. Hypixel puts the
     * rarity + category on the last lore line (e.g. "LEGENDARY SWORD",
     * "EPIC DRILL 3000", "MYTHIC NECKLACE"). Order of checks matters — PICKAXE
     * must beat AXE, etc. Defaults to SWORD (its pool is the widest weapon set)
     * so the fake-out hits always have somewhere sensible to pull from.
     */
    public static Category categoryOf(ItemStack stack) {
        // The category sits on the rarity line, usually the last lore line — but a
        // "Co-op Soulbound" note can sit below it, so scan the last few lines and
        // return the first that names a category.
        String[] tail = lastLoreLines(stack, 5);
        for (int i = tail.length - 1; i >= 0; i--) {
            Category c = categoryFromLine(tail[i]);
            if (c != null) return c;
        }
        // WAND / GAUNTLET / fishing rod / anything unrecognised → sword pool.
        return Category.SWORD;
    }

    private static Category categoryFromLine(String line) {
        if (line == null) return null;
        String s = TextUtil.stripColor(line).toUpperCase();
        if (s.contains("BOW")) return Category.BOW;                 // LONGBOW/BOW
        if (s.contains("FISHING ROD") || s.contains("FISHING WEAPON")) return Category.SWORD;
        if (s.contains("PICKAXE") || s.contains("DRILL") || s.contains("SHOVEL")) return Category.PICKAXE;
        if (s.contains("HOE")) return Category.HOE;
        if (s.contains("AXE")) return Category.AXE;                 // after PICKAXE
        if (s.contains("HELMET") || s.contains("CHESTPLATE") || s.contains("LEGGINGS")
                || s.contains("BOOTS") || s.contains("ARMOR")) return Category.ARMOR;
        if (s.contains("NECKLACE") || s.contains("CLOAK") || s.contains("BELT")
                || s.contains("GLOVES") || s.contains("BRACELET") || s.contains("EQUIPMENT")) {
            return Category.EQUIPMENT;
        }
        if (s.contains("SWORD")) return Category.SWORD;
        return null;
    }

    // --- internals -----------------------------------------------------------

    private static NBTTagCompound extraAttributes(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) return null;
        NBTTagCompound tag = stack.getTagCompound();
        if (!tag.hasKey("ExtraAttributes")) return null;
        return tag.getCompoundTag("ExtraAttributes");
    }

    /** The last up-to-{@code n} non-empty lore lines, in original top-to-bottom order. */
    private static String[] lastLoreLines(ItemStack stack, int n) {
        if (stack == null || !stack.hasTagCompound()) return new String[0];
        NBTTagCompound tag = stack.getTagCompound();
        if (!tag.hasKey("display")) return new String[0];
        NBTTagCompound display = tag.getCompoundTag("display");
        if (!display.hasKey("Lore")) return new String[0];
        NBTTagList lore = display.getTagList("Lore", 8); // 8 == string tag
        java.util.ArrayList<String> nonEmpty = new java.util.ArrayList<String>();
        for (int i = 0; i < lore.tagCount(); i++) {
            String line = lore.getStringTagAt(i);
            if (line != null && !TextUtil.stripColor(line).trim().isEmpty()) nonEmpty.add(line);
        }
        int from = Math.max(0, nonEmpty.size() - n);
        return nonEmpty.subList(from, nonEmpty.size()).toArray(new String[0]);
    }

    private static Map<String, String> buildIdMap() {
        Map<String, String> m = new HashMap<String, String>();
        addAll(m, SWORD);
        addAll(m, BOW);
        addAll(m, ARMOR);
        addAll(m, EQUIPMENT);
        addAll(m, AXE);
        addAll(m, HOE);
        addAll(m, PICKAXE);
        // A few id spellings Hypixel uses that don't normalise cleanly.
        m.put("double_bit", "Double-Bit");
        m.put("green_thumb", "Green Thumb");
        return m;
    }

    private static void addAll(Map<String, String> m, List<String> names) {
        for (String name : names) m.put(normalizeId(name), name);
    }

    /** "Green Thumb" -> "green_thumb", "Lumberjack's" -> "lumberjacks". */
    private static String normalizeId(String name) {
        return name.toLowerCase().replace("'", "").replace("-", "_").replace(" ", "_");
    }

    /** Turn a raw id ("blessed", "green_thumb") into a titled label for display. */
    private static String prettify(String id) {
        String[] parts = id.replace('_', ' ').replace('-', ' ').split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) sb.append(p.substring(1));
        }
        return sb.length() == 0 ? id : sb.toString();
    }

    private static List<String> ro(String... items) {
        return Collections.unmodifiableList(Arrays.asList(items));
    }

    // Kept for potential future reflection/debug; keeps LinkedHashMap import used.
    static Map<String, List<String>> allPools() {
        Map<String, List<String>> m = new LinkedHashMap<String, List<String>>();
        m.put("SWORD", SWORD); m.put("BOW", BOW); m.put("ARMOR", ARMOR);
        m.put("EQUIPMENT", EQUIPMENT); m.put("AXE", AXE); m.put("HOE", HOE);
        m.put("PICKAXE", PICKAXE);
        return m;
    }
}
