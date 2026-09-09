package com.dragonloot.core;

import com.dragonloot.data.DragonReward;
import com.dragonloot.data.RewardData;

import java.util.List;
import java.util.Random;

/**
 * Plain-Java tests for DragonLoot's dependency-free core: the roll engine,
 * easing math, reward tables (including the "legendary = only 3 items" rule),
 * lifetime stats, and a million-roll stress run. No Minecraft needed.
 */
public class DragonTest {

    static int passed = 0, failed = 0;

    public static void main(String[] args) {
        testProbabilitiesSumToOne();
        testStagesWellFormed();
        testDistributionMatchesOdds();
        testLuck();
        testClamping();
        testEasing();
        testLootTable();
        testRewards();
        testStats();
        testHugeStress();

        System.out.println("\n==================================");
        System.out.println("DragonLoot  PASSED: " + passed + "   FAILED: " + failed);
        System.out.println("==================================");
        if (failed > 0) System.exit(1);
    }

    static RollEngine defaultEngine() {
        RollEngine e = RollEngine.INSTANCE;
        e.setAdvanceChance(Rarity.UNCOMMON, 0.50);
        e.setAdvanceChance(Rarity.RARE, 0.30);
        e.setAdvanceChance(Rarity.EPIC, 0.15);
        e.setLuck(1.0);
        return e;
    }
    static void ok(boolean cond, String name) {
        if (cond) { passed++; System.out.println("  ok  - " + name); }
        else { failed++; System.out.println("  FAIL- " + name); }
    }
    static void near(double a, double b, double tol, String name) {
        ok(Math.abs(a - b) <= tol, name + " (got " + a + ", want ~" + b + ")");
    }

    static void testProbabilitiesSumToOne() {
        System.out.println("[Final probabilities]");
        RollEngine e = defaultEngine();
        double[] p = e.finalProbabilities();
        double sum = 0; for (double x : p) sum += x;
        near(sum, 1.0, 1e-9, "probabilities sum to 1");
        near(p[Rarity.UNCOMMON.ordinal()], 0.50, 1e-9, "P(uncommon)");
        near(p[Rarity.RARE.ordinal()], 0.35, 1e-9, "P(rare)");
        near(p[Rarity.EPIC.ordinal()], 0.1275, 1e-9, "P(epic)");
        near(p[Rarity.LEGENDARY.ordinal()], 0.0225, 1e-9, "P(legendary)");
        ok(p[0] > p[1] && p[1] > p[2] && p[2] > p[3], "rarer tiers are progressively rarer");
    }

    static void testStagesWellFormed() {
        System.out.println("[Stages well-formed]");
        RollEngine e = defaultEngine();
        Random r = new Random(1);
        boolean allGood = true;
        for (int i = 0; i < 50_000; i++) {
            RollResult res = e.roll(r);
            List<Rarity> st = res.stages();
            if (st.isEmpty() || st.get(0) != Rarity.UNCOMMON) allGood = false;
            for (int k = 1; k < st.size(); k++) if (st.get(k) != st.get(k - 1).next()) allGood = false;
            if (res.finalRarity() != st.get(st.size() - 1)) allGood = false;
        }
        ok(allGood, "every roll starts at UNCOMMON and escalates one tier at a time");
    }

    static void testDistributionMatchesOdds() {
        System.out.println("[Distribution ~ odds]");
        RollEngine e = defaultEngine();
        double[] expected = e.finalProbabilities();
        Random r = new Random(12345);
        long n = 400_000;
        long[] counts = new long[Rarity.values().length];
        for (long i = 0; i < n; i++) counts[e.roll(r).finalRarity().ordinal()]++;
        for (Rarity t : Rarity.values()) {
            double emp = (double) counts[t.ordinal()] / n;
            near(emp, expected[t.ordinal()], 0.01, "empirical P(" + t.displayName + ") ~ theory");
        }
    }

    static void testLuck() {
        System.out.println("[Luck]");
        RollEngine e = defaultEngine();
        double baseLeg = e.finalProbabilities()[Rarity.LEGENDARY.ordinal()];
        e.setLuck(3.0);
        double luckyLeg = e.finalProbabilities()[Rarity.LEGENDARY.ordinal()];
        ok(luckyLeg > baseLeg, "higher luck raises legendary chance (" + baseLeg + " -> " + luckyLeg + ")");
        e.setLuck(0.0);
        near(e.finalProbabilities()[Rarity.UNCOMMON.ordinal()], 1.0, 1e-9, "zero luck => always uncommon");
        defaultEngine();
    }

    static void testClamping() {
        System.out.println("[Clamping]");
        RollEngine e = defaultEngine();
        e.setAdvanceChance(Rarity.UNCOMMON, 5.0);
        near(e.getAdvanceChance(Rarity.UNCOMMON), RollEngine.MAX_ADVANCE, 1e-9, "advance clamps to MAX_ADVANCE");
        e.setAdvanceChance(Rarity.UNCOMMON, -1.0);
        near(e.getAdvanceChance(Rarity.UNCOMMON), 0.0, 1e-9, "advance clamps to 0");
        e.setLuck(999);
        near(e.getLuck(), 10.0, 1e-9, "luck clamps to 10");
        ok(e.effectiveAdvance(Rarity.LEGENDARY) == 0.0, "top tier never advances");
        defaultEngine();
    }

    static void testEasing() {
        System.out.println("[Easing]");
        near(Easing.outCubic(0), 0, 1e-9, "outCubic(0)=0");
        near(Easing.outCubic(1), 1, 1e-9, "outCubic(1)=1");
        ok(Easing.outCubic(0.5) > 0.5, "outCubic front-loaded (fast then slow)");
        near(Easing.outQuint(1), 1, 1e-9, "outQuint(1)=1");
        near(Easing.inOutCubic(0.5), 0.5, 1e-9, "inOutCubic symmetric at mid");
        ok(Easing.outBack(0.8) > 1.0, "outBack overshoots past 1");
        near(Easing.clamp01(2.0), 1.0, 1e-9, "clamp01 caps at 1");
        ok(Easing.mixColor(0xFF000000, 0xFFFFFFFF, 0.5) == 0xFF7F7F7F, "mixColor midpoint grey");
    }

    static void testLootTable() {
        System.out.println("[Loot table rule]");
        List<DragonReward> leg = RewardData.forRarity(Rarity.LEGENDARY);
        ok(leg.size() == 3, "exactly 3 legendary items (got " + leg.size() + ")");
        boolean hasDragonPet = false, hasEnderPet = false, hasDye = false;
        for (DragonReward d : leg) {
            if (d.name.equals("Ender Dragon Pet")) hasDragonPet = true;
            if (d.name.equals("Enderman Pet")) hasEnderPet = true;
            if (d.name.equals("Pearlescent Dye")) hasDye = true;
        }
        ok(hasDragonPet && hasEnderPet && hasDye, "legendary = 2 ender pets + Pearlescent Dye");
        // Those three must NOT appear in any lower tier.
        boolean leakedDown = false;
        for (Rarity r : new Rarity[]{Rarity.UNCOMMON, Rarity.RARE, Rarity.EPIC}) {
            for (DragonReward d : RewardData.forRarity(r)) {
                if (d.name.equals("Ender Dragon Pet") || d.name.equals("Enderman Pet") || d.name.equals("Pearlescent Dye")) {
                    leakedDown = true;
                }
            }
        }
        ok(!leakedDown, "the three legendaries never appear in a lower tier");
        ok(!RewardData.forRarity(Rarity.EPIC).isEmpty()
                && !RewardData.forRarity(Rarity.RARE).isEmpty()
                && !RewardData.forRarity(Rarity.UNCOMMON).isEmpty(), "the rest are sorted across lower tiers");
    }

    static void testRewards() {
        System.out.println("[Rewards]");
        ok(!RewardData.all().isEmpty(), "reward table non-empty");
        Random r = new Random(7);
        boolean allValid = true;
        for (Rarity t : Rarity.values()) {
            for (int i = 0; i < 100; i++) {
                DragonReward d = RewardData.pick(t, r);
                if (d == null || d.rarity != t || d.name == null || d.name.isEmpty()) allValid = false;
            }
        }
        ok(allValid, "pick() returns a matching, well-formed reward for every tier");
    }

    static void testStats() {
        System.out.println("[Stats]");
        DragonStats s = DragonStats.INSTANCE;
        s.reset();
        ok(s.total() == 0 && s.best() == null, "reset zeroes stats");
        s.record(Rarity.UNCOMMON);
        s.record(Rarity.RARE);
        boolean newBest = s.record(Rarity.EPIC);
        ok(newBest, "record returns true on a new best");
        ok(!s.record(Rarity.UNCOMMON), "record returns false when not a new best");
        ok(s.total() == 4, "total counts every roll");
        ok(s.count(Rarity.UNCOMMON) == 2, "per-rarity count correct");
        ok(s.best() == Rarity.EPIC, "best tracks the highest tier");
        near(s.share(Rarity.UNCOMMON), 0.5, 1e-9, "share = count/total");
        for (int i = 0; i < 100; i++) s.record(Rarity.UNCOMMON);
        ok(s.historySize() <= 20, "history bounded to 20 (got " + s.historySize() + ")");
        long[] snap = s.countsSnapshot();
        long tot = s.total();
        int bo = s.bestOrdinal();
        s.loadCounts(snap, tot, bo);
        ok(s.total() == tot && s.bestOrdinal() == bo, "loadCounts round-trips totals + best");
        s.reset();
    }

    static void testHugeStress() {
        System.out.println("[Huge stress: 1,000,000 rolls]");
        RollEngine e = defaultEngine();
        DragonStats s = DragonStats.INSTANCE;
        s.reset();
        Random r = new Random(2024);
        long n = 1_000_000;
        long[] counts = new long[Rarity.values().length];
        boolean crashed = false;
        try {
            for (long i = 0; i < n; i++) {
                RollResult res = e.roll(r);
                counts[res.finalRarity().ordinal()]++;
                s.record(res.finalRarity());
            }
        } catch (Throwable t) {
            crashed = true;
            System.out.println("    exception: " + t);
        }
        ok(!crashed, "no exception over 1M rolls");
        long sum = 0; for (long c : counts) sum += c;
        ok(sum == n, "counts sum to roll count");
        ok(s.total() == n, "stats total matches");
        ok(s.historySize() <= 20, "history stayed bounded");
        ok(counts[Rarity.UNCOMMON.ordinal()] > counts[Rarity.LEGENDARY.ordinal()], "uncommon far outnumbers legendary");
        ok(counts[Rarity.LEGENDARY.ordinal()] > 0, "legendary still happens sometimes");
        System.out.println("    uncommon=" + counts[0] + " rare=" + counts[1]
                + " epic=" + counts[2] + " legendary=" + counts[3]);
        s.reset();
    }
}
