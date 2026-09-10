package com.treegifts.core;

import java.util.List;

/**
 * Tests the Tree Gift chat parser against Hypixel's REAL message format.
 *
 * The §-formatted example lines below are taken verbatim from SkyHanni's
 * ForagingTracker regex tests (the authoritative parser), so passing this means
 * we match what Hypixel actually sends — including reading rarity from the item
 * name's chat colour. No Minecraft needed.
 */
public class TreeGiftChatParserTest {

    static int passed = 0, failed = 0;

    // 64 ▬ in dark-green bold — the real Tree Gift border.
    static final String BORDER = "§2§l" + repeat('▬', 64);

    public static void main(String[] args) {
        testRealGiftBlock();
        testRarityFromColor();
        testNoFalsePositives();
        testBorderWithoutHeaderIgnored();
        testDecodeAndStrip();

        System.out.println("\n==================================");
        System.out.println("PASSED: " + passed + "   FAILED: " + failed);
        System.out.println("==================================");
        if (failed > 0) System.exit(1);
    }

    static void ok(boolean c, String n) {
        if (c) { passed++; System.out.println("  ok  - " + n); }
        else { failed++; System.out.println("  FAIL- " + n); }
    }
    static void eq(Object a, Object b, String n) { ok(a == null ? b == null : a.equals(b), n + " (got " + a + ", want " + b + ")"); }

    static TreeGiftResult find(List<TreeGiftResult> l, String name) {
        for (TreeGiftResult r : l) if (r.itemName.equals(name)) return r;
        return null;
    }

    /** Feed a full, realistic Tree Gift block and check every notable drop. */
    static void testRealGiftBlock() {
        System.out.println("[Real Tree Gift block]");
        TreeGiftChatParser p = new TreeGiftChatParser();
        // These lines are SkyHanni's verbatim REGEX-TEST strings.
        feed(p, BORDER);
        feed(p, "                                §r§9§lTREE GIFT");
        feed(p, "                 §r§7You helped cut §r§a100% §r§7of the §r§aFig Tree§r§7.");
        feed(p, "                            §r§e+5 rewards gained! §8(hover)");
        feed(p, "§2Forest Essence§r§8 x4");
        feed(p, "§3Foraging Experience §r§8x1,000");
        feed(p, "                                §r§d§lBONUS GIFT");
        feed(p, "                          §r§7§r§aStretching Sticks §r§8(§r§a20%§r§8)");
        feed(p, "          §r§7§r§aEnchanted Book (§r§d§lFirst Impression I§r§a) §r§8(§r§a0.4%§r§8)");
        feed(p, "                           §r§7§r§fSweep Booster §r§8(§r§a1%§r§8)");
        feed(p, "                          §r§7§r§cTree the Fish §r§8(§r§a0.05%§r§8)");
        feed(p, "§r§7A §r§dPhanpyre §r§7fell from the Tree!");
        List<TreeGiftResult> out = p.feedLine(BORDER); // closing border -> results

        ok(out.size() == 5, "5 notable drops parsed (got " + out.size() + ": " + out + ")");

        TreeGiftResult sticks = find(out, "Stretching Sticks");
        ok(sticks != null && sticks.rarity == Rarity.UNCOMMON, "Stretching Sticks = Uncommon (§a)");
        ok(sticks != null && Math.abs(sticks.dropChancePercent - 20.0) < 1e-6, "Stretching Sticks 20%");
        ok(sticks != null && "Fig".equals(sticks.treeType), "tree type = Fig");

        TreeGiftResult fish = find(out, "Tree the Fish");
        ok(fish != null && fish.rarity == Rarity.SPECIAL, "Tree the Fish = Special (§c)");
        ok(fish != null && Math.abs(fish.dropChancePercent - 0.05) < 1e-6, "Tree the Fish 0.05%");

        TreeGiftResult sweep = find(out, "Sweep Booster");
        ok(sweep != null && sweep.rarity == Rarity.COMMON, "Sweep Booster = Common (§f)");

        TreeGiftResult book = find(out, "Enchanted Book (First Impression I)");
        ok(book != null && book.rarity == Rarity.UNCOMMON, "Enchanted Book parsed with nested parens");

        TreeGiftResult phan = find(out, "Phanpyre");
        ok(phan != null && phan.phantom, "Phanpyre parsed as phantom");
        ok(phan != null && phan.rarity == Rarity.MYTHIC, "Phanpyre = Mythic (§d)");

        // Icons resolve (render stand-ins, presentation only).
        ok(fish != null && fish.iconItemId.contains("cod"), "Tree the Fish icon = cod");
        ok(phan != null && phan.iconItemId.contains("phantom_membrane"), "Phanpyre icon = phantom_membrane");
    }

    /** The rarity must come from the colour, not the item name. */
    static void testRarityFromColor() {
        System.out.println("[Rarity from colour]");
        // Same item name, different colour -> different rarity.
        eq(rarityOf("          §r§7§r§6Mystery Thing §r§8(§r§a1%§r§8)"), Rarity.LEGENDARY, "§6 -> Legendary");
        eq(rarityOf("          §r§7§r§9Mystery Thing §r§8(§r§a1%§r§8)"), Rarity.RARE, "§9 -> Rare");
        eq(rarityOf("          §r§7§r§5Mystery Thing §r§8(§r§a1%§r§8)"), Rarity.EPIC, "§5 -> Epic");
    }

    static Rarity rarityOf(String bonusLine) {
        TreeGiftChatParser p = new TreeGiftChatParser();
        p.feedLine(BORDER);
        p.feedLine("        §r§9§lTREE GIFT");
        p.feedLine(bonusLine);
        List<TreeGiftResult> out = p.feedLine(BORDER);
        return out.isEmpty() ? null : out.get(0).rarity;
    }

    /** Unrelated chat must never trigger. */
    static void testNoFalsePositives() {
        System.out.println("[No false positives]");
        TreeGiftChatParser p = new TreeGiftChatParser();
        ok(p.feedLine("§2Guild > Steve§f: got a drop lol (50%)").isEmpty(), "guild chat ignored");
        ok(p.feedLine("§r§aSteve§f: Tree the Fish (0.05%)").isEmpty(), "player chat ignored (no border)");
        ok(p.feedLine("§eYou found a §6Legendary§e item! (0.1%)").isEmpty(), "random loot msg ignored");
        ok(p.feedLine("An Enderman fell from the Tree!").isEmpty(), "stray line without a gift block ignored");
    }

    /** A bordered block that is NOT a Tree Gift must be ignored. */
    static void testBorderWithoutHeaderIgnored() {
        System.out.println("[Border without header]");
        TreeGiftChatParser p = new TreeGiftChatParser();
        p.feedLine(BORDER);
        p.feedLine("        §r§6§lSOMETHING ELSE");
        p.feedLine("          §r§7§r§cTree the Fish §r§8(§r§a0.05%§r§8)");
        List<TreeGiftResult> out = p.feedLine(BORDER);
        ok(out.isEmpty(), "no header 'TREE GIFT' -> block ignored (no false positive)");
    }

    static void testDecodeAndStrip() {
        System.out.println("[decode / strip]");
        eq(TreeGiftChatParser.strip("§aHello §lWorld"), "Hello World", "strip removes § codes");
        TreeGiftChatParser.Decoded d = TreeGiftChatParser.decode("§r§7§r§cTree");
        eq(d.plain, "Tree", "decode plain");
        ok(d.colors.length == 4 && d.colors[0] == 'c', "decode colour at 'T' is c");
    }

    static void feed(TreeGiftChatParser p, String line) {
        List<TreeGiftResult> r = p.feedLine(line);
        if (!r.isEmpty()) System.out.println("    (unexpected mid-block emit: " + r + ")");
    }

    static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }
}
