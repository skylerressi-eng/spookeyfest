package com.treegifts.core;

import java.util.List;
import java.util.Random;

/**
 * Plain-Java test + big statistical check for the tree-gift roll engine.
 * No Minecraft needed — run via ../scripts/run-tests.sh.
 *
 * Proves the fun stuff actually behaves: the ladder only ever climbs, a gift
 * always pays out, mango only happens off a Legendary, seeded rolls are
 * reproducible, and the published odds match reality over a million rolls.
 */
public class TreeGiftRollerTest {

    static int passed = 0, failed = 0;

    public static void main(String[] args) {
        testRarityLadder();
        testConfigClamping();
        testRollShape();
        testDeterminism();
        testForcedPaths();
        testLootTables();
        testDistributionOneMillion();

        System.out.println("\n==================================");
        System.out.println("PASSED: " + passed + "   FAILED: " + failed);
        System.out.println("==================================");
        if (failed > 0) System.exit(1);
    }

    // ---- helpers ----
    static void ok(boolean cond, String name) {
        if (cond) { passed++; System.out.println("  ok  - " + name); }
        else { failed++; System.out.println("  FAIL- " + name); }
    }
    static void eq(Object a, Object b, String name) {
        ok(a == null ? b == null : a.equals(b), name + " (got " + a + ", want " + b + ")");
    }
    static void near(double a, double b, double tol, String name) {
        ok(Math.abs(a - b) <= tol, name + " (got " + fmt(a) + ", want ~" + fmt(b) + " ±" + fmt(tol) + ")");
    }
    static String fmt(double d) { return String.format("%.5f", d); }

    // ---- Rarity ----
    static void testRarityLadder() {
        System.out.println("[Rarity ladder]");
        eq(Rarity.COMMON.next(), Rarity.UNCOMMON, "common -> uncommon");
        eq(Rarity.UNCOMMON.next(), Rarity.RARE, "uncommon -> rare");
        eq(Rarity.RARE.next(), Rarity.EPIC, "rare -> epic");
        eq(Rarity.EPIC.next(), Rarity.LEGENDARY, "epic -> legendary");
        eq(Rarity.LEGENDARY.next(), Rarity.MYTHIC, "legendary -> mythic (ladder extended for real drops)");
        eq(Rarity.SPECIAL.next(), Rarity.SPECIAL, "special caps at the top");
        ok(Rarity.LEGENDARY.isMax(), "legendary isMax (legacy demo roller stops here)");
        ok(!Rarity.COMMON.isMax(), "common !isMax");
        boolean colored = true;
        for (Rarity r : Rarity.values()) if ((r.color & 0xFF000000) == 0) colored = false;
        ok(colored, "every rarity colour is opaque (alpha set)");
    }

    // ---- Config ----
    static void testConfigClamping() {
        System.out.println("[Config clamping]");
        GiftConfig c = GiftConfig.defaults();
        near(c.upgradeChance(Rarity.COMMON), 0.70, 1e-9, "common upgrade 0.70");
        near(c.upgradeChance(Rarity.EPIC), 0.20, 1e-9, "epic upgrade 0.20");
        near(c.upgradeChance(Rarity.LEGENDARY), 0.0, 1e-9, "legendary can't upgrade");
        c.commonUpgrade = 5.0;   // nonsense high
        c.epicUpgrade = -3.0;    // nonsense low
        near(c.upgradeChance(Rarity.COMMON), 1.0, 1e-9, "clamped to 1.0");
        near(c.upgradeChance(Rarity.EPIC), 0.0, 1e-9, "clamped to 0.0");
    }

    // ---- Shape of every roll ----
    static void testRollShape() {
        System.out.println("[Roll shape]");
        Random r = new Random(1);
        TreeGiftRoller roller = new TreeGiftRoller(GiftConfig.defaults(), r);
        boolean allStartReveal = true, allEndTerminal = true, allMonotonic = true;
        boolean mangoOnlyLegendary = true, lootAlways = true, atLeastTwoBeats = true;
        boolean revealIsCommon = true, oneTerminalOnly = true;

        for (int i = 0; i < 200_000; i++) {
            GiftResult g = roller.roll();
            List<GiftStep> s = g.steps;

            if (s.get(0).type != GiftStep.Type.REVEAL) allStartReveal = false;
            if (s.get(0).rarity != Rarity.COMMON) revealIsCommon = false;
            if (!g.last().isTerminal()) allEndTerminal = false;
            if (g.axeCount() < 2) atLeastTwoBeats = false;
            if (g.loot == null) lootAlways = false;

            // rarity never goes down across beats
            for (int k = 1; k < s.size(); k++) {
                if (s.get(k).rarity.ordinal() < s.get(k - 1).rarity.ordinal()) allMonotonic = false;
            }
            // exactly one terminal beat, and it's the last one
            int terminals = 0;
            for (GiftStep step : s) if (step.isTerminal()) terminals++;
            if (terminals != 1 || !s.get(s.size() - 1).isTerminal()) oneTerminalOnly = false;

            if (g.mango) {
                if (g.finalRarity != Rarity.LEGENDARY) mangoOnlyLegendary = false;
                if (g.last().type != GiftStep.Type.MANGO) mangoOnlyLegendary = false;
                if (g.loot.color != LootTables.MANGO_COLOR) mangoOnlyLegendary = false;
            } else {
                // non-mango loot wears its rarity's colour
                if (g.loot.color != g.finalRarity.color) lootAlways = false;
            }
        }
        ok(allStartReveal, "every roll starts with a REVEAL");
        ok(revealIsCommon, "the reveal is always Common");
        ok(allEndTerminal, "every roll ends terminal (crack or mango)");
        ok(oneTerminalOnly, "exactly one terminal beat, and it's last");
        ok(atLeastTwoBeats, "every roll is >= 2 beats (reveal + payout)");
        ok(allMonotonic, "rarity never decreases across beats");
        ok(mangoOnlyLegendary, "mango only ever off a Legendary, with mango colour");
        ok(lootAlways, "loot always present and colour matches outcome");
    }

    // ---- Determinism ----
    static void testDeterminism() {
        System.out.println("[Determinism]");
        GiftResult a = new TreeGiftRoller(GiftConfig.defaults(), new Random(12345)).roll();
        GiftResult b = new TreeGiftRoller(GiftConfig.defaults(), new Random(12345)).roll();
        eq(a.steps.toString(), b.steps.toString(), "same seed -> same beats");
        eq(a.finalRarity, b.finalRarity, "same seed -> same rarity");
        eq(a.loot.name, b.loot.name, "same seed -> same loot");

        GiftResult c = new TreeGiftRoller(GiftConfig.defaults(), new Random(999)).roll();
        // Not a hard guarantee, but with different seeds these virtually never all match.
        ok(!(a.steps.toString().equals(c.steps.toString()) && a.loot.name.equals(c.loot.name)),
                "different seed -> (almost surely) different roll");
    }

    // ---- Forced edge configs ----
    static void testForcedPaths() {
        System.out.println("[Forced paths]");

        // Everything cracks immediately -> always Common.
        GiftConfig neverUp = GiftConfig.defaults();
        neverUp.commonUpgrade = neverUp.uncommonUpgrade = neverUp.rareUpgrade = neverUp.epicUpgrade = 0.0;
        boolean allCommon = true;
        Random r1 = new Random(7);
        for (int i = 0; i < 5000; i++) {
            GiftResult g = new TreeGiftRoller(neverUp, r1).roll();
            if (g.finalRarity != Rarity.COMMON || g.axeCount() != 2 || g.mango) allCommon = false;
        }
        ok(allCommon, "upgrade=0 -> always Common in exactly 2 beats");

        // Always climb, always mango at the top -> always Mango Dye.
        GiftConfig alwaysMango = GiftConfig.defaults();
        alwaysMango.commonUpgrade = alwaysMango.uncommonUpgrade = 1.0;
        alwaysMango.rareUpgrade = alwaysMango.epicUpgrade = 1.0;
        alwaysMango.mangoChance = 1.0;
        boolean allMango = true;
        Random r2 = new Random(8);
        for (int i = 0; i < 5000; i++) {
            GiftResult g = new TreeGiftRoller(alwaysMango, r2).roll();
            // reveal C, up U, up R, up E, up L, mango = 6 beats
            if (!g.mango || g.finalRarity != Rarity.LEGENDARY || g.axeCount() != 6) allMango = false;
            if (!g.loot.name.equals("Mango Dye")) allMango = false;
        }
        ok(allMango, "upgrade=1 & mango=1 -> always Mango Dye in 6 beats");

        // Always climb, never mango -> always Legendary loot.
        GiftConfig alwaysLegend = GiftConfig.defaults();
        alwaysLegend.commonUpgrade = alwaysLegend.uncommonUpgrade = 1.0;
        alwaysLegend.rareUpgrade = alwaysLegend.epicUpgrade = 1.0;
        alwaysLegend.mangoChance = 0.0;
        boolean allLegend = true;
        Random r3 = new Random(9);
        for (int i = 0; i < 5000; i++) {
            GiftResult g = new TreeGiftRoller(alwaysLegend, r3).roll();
            if (g.mango || g.finalRarity != Rarity.LEGENDARY || g.last().type != GiftStep.Type.CRACK) allLegend = false;
        }
        ok(allLegend, "upgrade=1 & mango=0 -> always Legendary crack");
    }

    // ---- Loot tables ----
    static void testLootTables() {
        System.out.println("[Loot tables]");
        Random r = new Random(3);
        boolean allTiersLoot = true;
        for (Rarity rar : Rarity.values()) {
            for (int i = 0; i < 500; i++) {
                Loot l = LootTables.forRarity(rar, r);
                if (l == null || l.name == null || l.itemId == null || l.amount <= 0) allTiersLoot = false;
                if (l.color != rar.color) allTiersLoot = false;
                if (l.itemId.indexOf(':') < 0) allTiersLoot = false; // looks like a real id
            }
        }
        ok(allTiersLoot, "every rarity yields valid, rarity-coloured loot");

        Loot m = LootTables.mango(r);
        eq(m.name, "Mango Dye", "mango name");
        ok(m.color == LootTables.MANGO_COLOR, "mango wears its own colour");
        ok(m.amount >= 1, "mango amount >= 1");
    }

    // ---- Big statistical check ----
    static void testDistributionOneMillion() {
        System.out.println("[Distribution: 1,000,000 rolls]");
        final int N = 1_000_000;
        Random r = new Random(2024);
        TreeGiftRoller roller = new TreeGiftRoller(GiftConfig.defaults(), r);

        int[] byRarity = new int[Rarity.values().length];
        int mango = 0;
        long beatsTotal = 0;
        int maxBeats = 0;
        boolean crashed = false;
        try {
            for (int i = 0; i < N; i++) {
                GiftResult g = roller.roll();
                if (g.mango) mango++;
                else byRarity[g.finalRarity.ordinal()]++;
                beatsTotal += g.axeCount();
                if (g.axeCount() > maxBeats) maxBeats = g.axeCount();
            }
        } catch (Throwable e) {
            crashed = true;
            System.out.println("    exception: " + e);
        }
        ok(!crashed, "no exception over 1,000,000 rolls");

        double pCommon = byRarity[Rarity.COMMON.ordinal()] / (double) N;
        double pUncommon = byRarity[Rarity.UNCOMMON.ordinal()] / (double) N;
        double pRare = byRarity[Rarity.RARE.ordinal()] / (double) N;
        double pEpic = byRarity[Rarity.EPIC.ordinal()] / (double) N;
        double pLegend = byRarity[Rarity.LEGENDARY.ordinal()] / (double) N;
        double pMango = mango / (double) N;

        System.out.println(String.format(
                "    common=%.4f uncommon=%.4f rare=%.4f epic=%.4f legend=%.4f mango=%.5f  (avg beats=%.2f, max=%d)",
                pCommon, pUncommon, pRare, pEpic, pLegend, pMango, beatsTotal / (double) N, maxBeats));

        // Expected from the default odds (see GiftConfig): each within a small tolerance.
        near(pCommon,   0.30000, 0.005,  "P(Common) ~ 0.300");
        near(pUncommon, 0.31500, 0.005,  "P(Uncommon) ~ 0.315");
        near(pRare,     0.25025, 0.005,  "P(Rare) ~ 0.250");
        near(pEpic,     0.10780, 0.004,  "P(Epic) ~ 0.108");
        near(pLegend,   0.02560, 0.003,  "P(Legendary loot) ~ 0.0256");
        near(pMango,    0.00135, 0.0008, "P(Mango Dye) ~ 0.00135");

        // Probabilities cover the whole space.
        near(pCommon + pUncommon + pRare + pEpic + pLegend + pMango, 1.0, 1e-9, "probabilities sum to 1");
        ok(maxBeats <= 6, "no roll ever exceeds 6 beats (reveal + 4 climbs + payout)");
    }
}
