package com.spookybirch.dragon;

import com.spookybirch.dragon.data.DragonLoot;
import com.spookybirch.dragon.data.DragonLootData;
import com.spookybirch.dragon.data.DragonType;
import com.spookybirch.dragon.data.DragonWeight;
import com.spookybirch.dragon.data.LootEntry;
import com.spookybirch.dragon.sim.Easing;
import com.spookybirch.dragon.sim.RouletteAnimator;
import com.spookybirch.dragon.sim.RouletteResult;
import com.spookybirch.dragon.sim.RouletteSimulator;

/**
 * Forge-free verification of the Dragon Altar Roulette core: loot data
 * integrity, the Dragon Weight formula, the deterministic simulator, the easing
 * maths and — most importantly — that the animation state machine lands the ball
 * <b>exactly</b> on the chosen segment under the top pointer.
 *
 * Run: javac these + the data/sim sources, then {@code java com...DragonSimTest}.
 */
public class DragonSimTest {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        testLootDataIntegrity();
        testVerifiedWeightRequirements();
        testWeightFormula();
        testSimulatorDeterminism();
        testSimulatorRangeAndSpread();
        testEasing();
        testExactLanding(false);
        testExactLanding(true);
        testReplayIndependence();

        System.out.println();
        System.out.println("DragonSimTest: " + (checks - failures) + "/" + checks + " checks passed.");
        if (failures > 0) {
            System.out.println("FAILURES: " + failures);
            System.exit(1);
        }
        System.out.println("ALL DRAGON SIM TESTS PASSED");
    }

    // --- data --------------------------------------------------------------

    private static void testLootDataIntegrity() {
        for (DragonType t : DragonType.values()) {
            DragonLoot pool = DragonLootData.pool(t);
            check(pool != null, "pool exists for " + t);
            check(pool.size() >= 5, t + " pool has segments (" + pool.size() + ")");
            for (LootEntry e : pool.entries()) {
                check(e.name != null && !e.name.isEmpty(), t + " entry has a name");
                check(e.shortName != null && !e.shortName.isEmpty(), t + " entry has a short name");
                check(e.rarity != null, t + " entry has a rarity");
                check(e.dropInfo != null, t + " entry has drop info (no silent gaps)");
                // Weight is a verified value, NONE, or UNKNOWN — never a random number.
                check(e.weightReq == LootEntry.NONE || e.weightReq == LootEntry.UNKNOWN
                        || (e.weightReq >= 250 && e.weightReq <= 450), t + " weight req is sane: " + e.weightReq);
            }
        }
        check(!DragonType.HOLY.summonable, "Holy is NOT summonable (dungeon armour, not a fight)");
        for (DragonType t : DragonType.summonables()) {
            check(t.summonable, t + " is summonable");
        }
    }

    private static void testVerifiedWeightRequirements() {
        DragonLoot sup = DragonLootData.pool(DragonType.SUPERIOR);
        check(findWeight(sup, "Superior Dragon Helmet") == 325, "Helmet weight = 325");
        check(findWeight(sup, "Superior Dragon Chestplate") == 400, "Chestplate weight = 400");
        check(findWeight(sup, "Superior Dragon Leggings") == 350, "Leggings weight = 350");
        check(findWeight(sup, "Superior Dragon Boots") == 300, "Boots weight = 300");
        check(findWeight(sup, "Aspect of the Dragons") == 450, "AOTD weight = 450");
        check(findWeight(sup, "Dragon Claw") == 450, "Dragon Claw weight = 450");
        check(findWeight(sup, "Dragon Horn") == 450, "Dragon Horn weight = 450 (Superior)");

        DragonLoot uns = DragonLootData.pool(DragonType.UNSTABLE);
        check(findWeight(uns, "Travel Scroll (Dragon's Nest)") == 250, "Travel Scroll weight = 250 (Unstable)");
        DragonLoot yng = DragonLootData.pool(DragonType.YOUNG);
        check(findWeight(yng, "Dragon Scale") == 450, "Dragon Scale weight = 450 (Young)");

        // Specials are exclusive to their dragon.
        check(findEntry(DragonLootData.pool(DragonType.STRONG), "Dragon Horn") == null, "Strong has no Dragon Horn");
        check(findEntry(DragonLootData.pool(DragonType.PROTECTOR), "Dragon Scale") == null, "Protector has no Scale");
    }

    // --- weight ------------------------------------------------------------

    private static void testWeightFormula() {
        DragonWeight w = new DragonWeight();
        w.setEyes(8); w.setDamageRank(1); w.setFinalBlow(true);
        check(w.eyeWeight() == 800, "8 eyes = 800");
        check(w.rankWeight() == 300, "rank 1 = 300");
        check(w.finalBlowWeight() == 50, "final blow = 50");
        check(w.total() == 1150, "total 8/1st/blow = 1150");

        w.setDamageRank(2); check(w.rankWeight() == 250, "rank 2 = 250");
        w.setDamageRank(3); check(w.rankWeight() == 200, "rank 3 = 200");
        w.setDamageRank(4); check(w.rankWeight() == 125, "rank 4 = 125");
        w.setDamageRank(10); check(w.rankWeight() == 125, "rank 10 = 125");
        w.setDamageRank(11); check(w.rankWeight() == 100, "rank 11 = 100");
        w.setDamageRank(15); check(w.rankWeight() == 100, "rank 15 = 100");
        w.setDamageRank(16); check(w.rankWeight() == 75, "rank 16 = 75");

        // Clamping.
        w.setEyes(99); check(w.eyes() == DragonWeight.MAX_EYES, "eyes clamp to 8");
        w.setEyes(-5); check(w.eyes() == 0, "eyes clamp to 0");
        w.setDamageRank(0); check(w.damageRank() == 1, "rank floors at 1");

        // Eligibility uses the real requirement; UNKNOWN never claims eligible.
        w.setEyes(4); w.setDamageRank(1); w.setFinalBlow(false); // 400+300 = 700
        LootEntry aotd = findEntry(DragonLootData.pool(DragonType.WISE), "Aspect of the Dragons");
        check(w.eligibleFor(aotd), "700 weight is eligible for AOTD (450)");
        w.setEyes(0); w.setDamageRank(16); w.setFinalBlow(false); // 0+75 = 75
        check(!w.eligibleFor(aotd), "75 weight not eligible for AOTD");
        LootEntry dye = findEntry(DragonLootData.pool(DragonType.WISE), "Pearlescent Dye");
        check(dye.weightReq == LootEntry.UNKNOWN && !w.eligibleFor(dye),
                "UNKNOWN-weight item never reports eligible");
    }

    // --- simulator ---------------------------------------------------------

    private static void testSimulatorDeterminism() {
        DragonLoot pool = DragonLootData.pool(DragonType.STRONG);
        int a = RouletteSimulator.pick(pool, 12345L);
        int b = RouletteSimulator.pick(pool, 12345L);
        check(a == b, "same seed => same segment (deterministic)");
        boolean anyDiff = false;
        for (long s = 0; s < 20; s++) {
            if (RouletteSimulator.pick(pool, s) != a) { anyDiff = true; break; }
        }
        check(anyDiff, "different seeds can give different segments");
    }

    private static void testSimulatorRangeAndSpread() {
        DragonLoot pool = DragonLootData.pool(DragonType.YOUNG);
        int n = pool.size();
        int[] hits = new int[n];
        for (int i = 0; i < 20000; i++) {
            int idx = RouletteSimulator.pick(pool, (long) i * 2654435761L);
            check(idx >= 0 && idx < n, "index in range", i == 0); // only log first
            hits[idx]++;
        }
        // Fair spin: every segment should be reachable.
        int empty = 0;
        for (int h : hits) if (h == 0) empty++;
        check(empty == 0, "every segment is reachable (uniform spin)");
    }

    // --- easing ------------------------------------------------------------

    private static void testEasing() {
        check(approx(Easing.spinProfile(0, 0.2, 0.22), 0, 1e-9), "spinProfile(0)=0");
        check(approx(Easing.spinProfile(1, 0.2, 0.22), 1, 1e-9), "spinProfile(1)=1");
        double prev = -1;
        boolean monotonic = true;
        for (int i = 0; i <= 100; i++) {
            double v = Easing.spinProfile(i / 100.0, 0.2, 0.22);
            if (v < prev - 1e-9) { monotonic = false; break; }
            prev = v;
        }
        check(monotonic, "spinProfile is monotonic increasing");
        check(approx(Easing.easeOutCubic(0), 0, 1e-9) && approx(Easing.easeOutCubic(1), 1, 1e-9),
                "easeOutCubic endpoints");
        check(Easing.clamp01(2) == 1 && Easing.clamp01(-1) == 0, "clamp01 bounds");
    }

    // --- the critical landing test ----------------------------------------

    private static void testExactLanding(boolean reducedMotion) {
        DragonLoot pool = DragonLootData.pool(DragonType.SUPERIOR);
        int n = pool.size();
        double seg = 360.0 / n;

        for (long seed = 0; seed < 60; seed++) {
            RouletteAnimator anim = new RouletteAnimator();
            anim.setReducedMotion(reducedMotion);
            anim.setAnimSpeed(1.0);
            anim.startSpin(pool, seed);
            RouletteResult res = anim.result();
            check(res != null, "spin produced a result", seed != 0);

            // Drive the clock well past the full timeline (~5.3s; reduced ~2.9s).
            long base = System.nanoTime();
            for (int ms = 0; ms <= 9000; ms += 16) {
                anim.update(base + ms * 1_000_000L);
            }

            // 1) Ball rests at the top pointer (angle ≡ 0 mod 360).
            double ballMod = norm360(anim.ballAngle());
            double ballErr = Math.min(ballMod, 360 - ballMod);
            check(ballErr < 0.5, "ball settles at pointer (err=" + fmt(ballErr) + ", seed " + seed + ")",
                    seed != 0 || reducedMotion);

            // 2) The chosen segment's centre sits under the pointer (screen angle ≈ 0).
            double segCenter = (res.segmentIndex + 0.5) * seg;
            double atPointer = norm360(segCenter + anim.wheelAngle());
            double segErr = Math.min(atPointer, 360 - atPointer);
            check(segErr < 0.5, "winning segment aligns to pointer (err=" + fmt(segErr) + ", seed " + seed + ")",
                    seed != 0 || reducedMotion);

            // 3) The segment computed under the pointer equals the result index.
            int under = (int) Math.floor(norm360(-anim.wheelAngle()) / seg) % n;
            check(under == res.segmentIndex,
                    "pointer segment == result index (" + under + " vs " + res.segmentIndex + ")",
                    seed != 0 || reducedMotion);

            // 4) No NaN leaked into the render angles.
            check(!Double.isNaN(anim.wheelAngle()) && !Double.isNaN(anim.ballAngle()),
                    "angles are finite", seed != 0 || reducedMotion);
        }
    }

    private static void testReplayIndependence() {
        DragonLoot pool = DragonLootData.pool(DragonType.OLD);
        RouletteAnimator anim = new RouletteAnimator();
        anim.startSpin(pool, 7L);
        int first = anim.result().segmentIndex;
        long base = System.nanoTime();
        for (int ms = 0; ms <= 9000; ms += 32) anim.update(base + ms * 1_000_000L);
        anim.reset();
        check(anim.result() == null, "reset clears the result");
        check(anim.idle(), "reset returns to IDLE");
        anim.startSpin(pool, 7L);
        check(anim.result().segmentIndex == first, "replay with same seed reproduces the result");
    }

    // --- helpers -----------------------------------------------------------

    private static int findWeight(DragonLoot pool, String name) {
        LootEntry e = findEntry(pool, name);
        return e == null ? Integer.MIN_VALUE : e.weightReq;
    }

    private static LootEntry findEntry(DragonLoot pool, String name) {
        for (LootEntry e : pool.entries()) if (e.name.equals(name)) return e;
        return null;
    }

    private static double norm360(double a) {
        double r = a % 360.0;
        return r < 0 ? r + 360.0 : r;
    }

    private static boolean approx(double a, double b, double eps) {
        return Math.abs(a - b) <= eps;
    }

    private static String fmt(double d) {
        return String.format("%.4f", d);
    }

    private static void check(boolean cond, String label) {
        check(cond, label, true);
    }

    /** {@code log} = whether to print this individual check (kept quiet for bulk loops). */
    private static void check(boolean cond, String label, boolean log) {
        checks++;
        if (!cond) {
            failures++;
            System.out.println("  FAIL: " + label);
        } else if (log) {
            System.out.println("  ok:   " + label);
        }
    }
}
