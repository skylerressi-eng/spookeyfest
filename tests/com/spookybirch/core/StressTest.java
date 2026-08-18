package com.spookybirch.core;

import com.spookybirch.data.CandyData;
import com.spookybirch.data.CandyMob;
import com.spookybirch.data.FishingData;
import com.spookybirch.data.SeaCreature;
import com.spookybirch.util.Fmt;
import com.spookybirch.util.TextUtil;

import java.util.List;
import java.util.Random;

/**
 * Plain-Java stress test + big check for the mod's Forge-free logic.
 * Run: javac (pure sources + this) then `java com.spookybirch.core.StressTest`.
 */
public class StressTest {

    static int passed = 0, failed = 0;

    // A fake clock the tracker reads instead of wall time.
    static long clockMs = 0L;

    public static void main(String[] args) {
        testFmt();
        testTextUtil();
        testTrackerBasics();
        testTradingNeverReduces();
        testOverallRate();
        testRecentRateAndEta();
        testBestRateSpikeGuard();
        testResetKeepsBaseline();
        testDataTables();
        testHugeStress();

        System.out.println("\n==================================");
        System.out.println("PASSED: " + passed + "   FAILED: " + failed);
        System.out.println("==================================");
        if (failed > 0) System.exit(1);
    }

    // ---- helpers ----
    static CandyTracker freshTracker() {
        CandyTracker t = CandyTracker.INSTANCE;
        t.setClockForTest(() -> clockMs);
        t.setPurpleWeight(8.0);
        t.setGoal(0);
        return t;
    }
    static void ok(boolean cond, String name) {
        if (cond) { passed++; System.out.println("  ok  - " + name); }
        else { failed++; System.out.println("  FAIL- " + name); }
    }
    static void eq(Object a, Object b, String name) { ok(a == null ? b == null : a.equals(b), name + " (got " + a + ", want " + b + ")"); }
    static void near(double a, double b, double tol, String name) { ok(Math.abs(a - b) <= tol, name + " (got " + a + ", want ~" + b + ")"); }

    // ---- Fmt ----
    static void testFmt() {
        System.out.println("[Fmt]");
        eq(Fmt.num(0), "0", "num 0");
        eq(Fmt.num(999), "999", "num 999");
        eq(Fmt.num(1234), "1,234", "num 1234 commas");
        eq(Fmt.num(12345), "12.3k", "num 12345 k");
        eq(Fmt.num(1_500_000), "1.5M", "num 1.5M");
        eq(Fmt.num(2_000_000), "2M", "num 2M trims .0");
        eq(Fmt.rate(12345), "12.3k/hr", "rate");
        eq(Fmt.duration(-1), "--", "duration negative");
        eq(Fmt.duration(45), "45s", "duration 45s");
        eq(Fmt.duration(90), "1m 30s", "duration 90s");
        eq(Fmt.duration(3661), "1h 01m", "duration 1h01m");
    }

    // ---- TextUtil ----
    static void testTextUtil() {
        System.out.println("[TextUtil]");
        eq(TextUtil.stripColor("§aGreen §lCandy"), "Green Candy", "stripColor");
        eq(TextUtil.stripColor(null), "", "stripColor null");
        eq(TextUtil.cleanNumber("1,234 "), "1234", "cleanNumber");
        ok(TextUtil.parseIntSafe("1,234", -1) == 1234, "parseIntSafe commas");
        ok(TextUtil.parseIntSafe("abc", 7) == 7, "parseIntSafe fallback");
    }

    // ---- Tracker basics ----
    static void testTrackerBasics() {
        System.out.println("[Tracker basics]");
        clockMs = 0;
        CandyTracker t = freshTracker();
        t.update(0, 0);            // start; existing inventory not counted
        t.update(5, 0);            // +5 green
        t.update(5, 1);            // +1 purple
        ok(t.collectedGreen() == 5, "collectedGreen == 5");
        ok(t.collectedPurple() == 1, "collectedPurple == 1");
        near(t.score(), 5 + 8, 0.001, "score = green + purple*8");
    }

    static void testTradingNeverReduces() {
        System.out.println("[Trading never reduces]");
        clockMs = 0;
        CandyTracker t = freshTracker();
        t.update(0, 0);
        t.update(10, 0);           // +10
        t.update(3, 0);            // sold 7 -> total drops, collected must NOT drop
        ok(t.collectedGreen() == 10, "collected stays 10 after selling");
        t.update(8, 0);            // +5 from the lower base of 3
        ok(t.collectedGreen() == 15, "collected 15 after regaining");
    }

    static void testOverallRate() {
        System.out.println("[Overall rate]");
        clockMs = 0;
        CandyTracker t = freshTracker();
        t.update(0, 0);
        clockMs = 3_600_000L;      // +1 hour
        t.update(3600, 0);         // 3600 green in an hour
        near(t.overallRatePerHour(), 3600, 1, "overall ~3600/hr");
        ok(t.elapsedMs() == 3_600_000L, "elapsed 1h");
    }

    static void testRecentRateAndEta() {
        System.out.println("[Recent rate + ETA]");
        clockMs = 0;
        CandyTracker t = freshTracker();
        t.setGoal(0);
        t.update(0, 0);
        int green = 0;
        // 1 green/sec for 600s (10 min); window is 5 min.
        for (int i = 0; i < 600; i++) {
            clockMs += 1000L;
            green += 1;
            t.update(green, 0);
        }
        near(t.recentRatePerHour(), 3600, 60, "recent ~3600/hr");
        ok(t.bestRecentRate() > 3000 && t.bestRecentRate() < 4200, "best rate sane (" + (int) t.bestRecentRate() + ")");
        // ETA: goal 300 above current score, at 3600/hr -> 300s.
        t.setGoal((int) t.score() + 3600);
        long eta = t.etaSecondsToGoal();
        near(eta, 3600, 120, "eta ~3600s to goal +3600 at 3600/hr");
    }

    static void testBestRateSpikeGuard() {
        System.out.println("[Best-rate spike guard]");
        clockMs = 0;
        CandyTracker t = freshTracker();
        t.update(0, 0);
        clockMs = 2000L;
        t.update(1000, 0);         // 1000 candy in 2 seconds
        ok(t.bestRecentRate() == 0.0, "burst in <60s does NOT set best rate");
        // now farm steadily for 90s
        int g = 1000;
        for (int i = 0; i < 90; i++) { clockMs += 1000L; g += 1; t.update(g, 0); }
        ok(t.bestRecentRate() > 0 && t.bestRecentRate() < 100000, "best rate becomes sane after >60s window");
    }

    static void testResetKeepsBaseline() {
        System.out.println("[Reset]");
        clockMs = 0;
        CandyTracker t = freshTracker();
        t.update(0, 0);
        t.update(50, 5);
        t.reset();
        ok(t.collectedGreen() == 0 && t.collectedPurple() == 0, "reset zeroes collected");
        t.update(50, 5);           // same inventory -> no phantom gain
        ok(t.collectedGreen() == 0 && t.collectedPurple() == 0, "reset keeps baseline (no phantom gain)");
        t.update(60, 6);           // +10 / +1
        ok(t.collectedGreen() == 10 && t.collectedPurple() == 1, "gains after reset counted");
    }

    static void testDataTables() {
        System.out.println("[Data tables]");
        List<CandyMob> ranked = CandyData.ranked();
        ok(!ranked.isEmpty(), "candy list non-empty");
        boolean sorted = true;
        for (int i = 1; i < ranked.size(); i++) if (ranked.get(i - 1).score() < ranked.get(i).score()) sorted = false;
        ok(sorted, "candy ranked descending by score");
        ok(CandyData.best() == ranked.get(0), "best() == top of ranking");
        boolean chancesValid = true;
        for (CandyMob m : CandyData.all()) if (m.purpleChance < 0 || m.purpleChance > 1 || m.greenPerKill < 0) chancesValid = false;
        ok(chancesValid, "candy chances in [0,1] and green>=0");
        boolean fishValid = true;
        for (SeaCreature c : FishingData.all()) if (c.chance < 0 || c.chance > 1) fishValid = false;
        ok(fishValid && !FishingData.all().isEmpty(), "fishing chances in [0,1]");
    }

    // ---- big stress ----
    static void testHugeStress() {
        System.out.println("[Huge stress: 500k updates]");
        clockMs = 0;
        CandyTracker t = freshTracker();
        t.setPurpleWeight(8.0);
        t.update(0, 0);
        Random r = new Random(42);
        int green = 0, purple = 0;
        long expectedGreen = 0, expectedPurple = 0;
        int maxSamples = 0;
        boolean crashed = false;
        try {
            for (int i = 0; i < 500_000; i++) {
                clockMs += 200L;               // 5 updates/sec, ~27h of play
                int dg = r.nextInt(5) - 1;     // -1..3 (sometimes sell)
                int dp = r.nextInt(20) == 0 ? 1 : 0;
                int ng = Math.max(0, green + dg);
                int np = Math.max(0, purple + dp);
                if (ng > green) expectedGreen += ng - green;
                if (np > purple) expectedPurple += np - purple;
                green = ng; purple = np;
                t.update(green, purple);
                maxSamples = Math.max(maxSamples, t.sampleCount());
                // occasional reads shouldn't blow up
                t.recentRatePerHour(); t.overallRatePerHour(); t.score(); t.etaSecondsToGoal();
            }
        } catch (Throwable e) {
            crashed = true;
            System.out.println("    exception: " + e);
        }
        ok(!crashed, "no exception over 500k updates");
        ok(t.collectedGreen() == expectedGreen, "collectedGreen matches sum of positive deltas");
        ok(t.collectedPurple() == expectedPurple, "collectedPurple matches sum of positive deltas");
        // window is 5 min at 5/sec = ~1500 samples; must stay bounded well under cap.
        ok(maxSamples <= 2000, "sample deque bounded (max=" + maxSamples + ")");
        ok(!Double.isNaN(t.score()) && !Double.isInfinite(t.score()), "score finite");
        ok(t.recentRatePerHour() >= 0, "recent rate non-negative");
        System.out.println("    final score=" + Fmt.num(t.score()) + " green=" + t.collectedGreen() + " purple=" + t.collectedPurple() + " maxSamples=" + maxSamples);
    }
}
