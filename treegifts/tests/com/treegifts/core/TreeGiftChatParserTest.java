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
        testGuaranteedOnlyPlays();
        testRewardCountFallback();
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
        List<TreeGiftResult> out = p.feedLine(BORDER); // closing border -> headline

        // One headline per gift: the rarest drop. Tree the Fish is Special (§c),
        // which outranks Phanpyre (Mythic) and everything else in this block.
        ok(out.size() == 1, "one headline per gift (got " + out.size() + ": " + out + ")");
        TreeGiftResult h = out.isEmpty() ? null : out.get(0);
        ok(h != null && h.itemName.equals("Tree the Fish"), "headline = Tree the Fish (rarest)");
        ok(h != null && h.rarity == Rarity.SPECIAL, "headline rarity = Special (§c)");
        ok(h != null && "Fig".equals(h.treeType), "tree type = Fig");
        ok(h != null && h.iconItemId.contains("cod"), "Tree the Fish icon = cod");
    }

    /** A plain tree (guaranteed items only, from the hover) must still reveal. */
    static void testGuaranteedOnlyPlays() {
        System.out.println("[Plain tree reveals from guaranteed items]");
        TreeGiftChatParser p = new TreeGiftChatParser();
        p.feedLine(BORDER);
        p.feedLine("                                §r§9§lTREE GIFT");
        p.feedLine("                 §r§7You helped cut §r§a100% §r§7of the §r§aHelix Tree§r§7.");
        p.feedLine("                            §r§e+3 rewards gained! §8(hover)");
        // These arrive from the hover in-game; fed as lines here.
        p.feedLine("§2Forest Essence§r§8 x4");
        p.feedLine("§3Foraging Experience §r§8x1,000");
        p.feedLine("§aTender Wood §r§8x0-2");
        List<TreeGiftResult> out = p.feedLine(BORDER);
        ok(out.size() == 1, "plain tree yields exactly one headline (got " + out + ")");
        TreeGiftResult h = out.isEmpty() ? null : out.get(0);
        ok(h != null && h.itemName.equals("Foraging Experience"), "headline = biggest guaranteed item");
        ok(h != null && h.amount == 1000, "headline amount parsed (1,000)");
        ok(h != null && "Helix".equals(h.treeType), "tree type = Helix");
    }

    /** Even with no parsable items, the reward-count line still triggers a reveal. */
    static void testRewardCountFallback() {
        System.out.println("[Reward-count fallback]");
        TreeGiftChatParser p = new TreeGiftChatParser();
        p.feedLine(BORDER);
        p.feedLine("        §r§9§lTREE GIFT");
        p.feedLine("        §r§7You helped cut §r§a50% §r§7of the §r§aFig Tree§r§7.");
        p.feedLine("        §r§e+7 rewards gained!");
        List<TreeGiftResult> out = p.feedLine(BORDER);
        ok(out.size() == 1, "reward-count fallback yields a headline");
        TreeGiftResult h = out.isEmpty() ? null : out.get(0);
        ok(h != null && h.itemName.equals("Tree Gift") && h.amount == 7, "generic 'Tree Gift' x7 headline");
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
