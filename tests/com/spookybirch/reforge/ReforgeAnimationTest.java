package com.spookybirch.reforge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Forge-free checks for the reforge cinematic's core invariants. The single most
 * important one: <b>the animation always ends on the exact real result it was
 * given</b> and never on an intermediate fake. Run via scripts/run-tests.sh.
 */
public class ReforgeAnimationTest {

    static int passed = 0, failed = 0;

    public static void main(String[] args) {
        List<String> swordPool = Arrays.asList(
                "Gentle", "Odd", "Fast", "Fair", "Epic", "Sharp", "Heroic", "Spicy", "Legendary");

        finalAlwaysRealResult(swordPool);
        stagesAdvanceMonotonically(swordPool);
        eachHitFiresExactlyOnce(swordPool);
        intermediatesComeFromPool(swordPool);
        degeneratePoolsDoNotHang();
        reducedMotionStillReachesResult(swordPool);

        System.out.println("\nPASSED: " + passed + "   FAILED: " + failed);
        if (failed > 0) System.exit(1);
    }

    /** Over 300 random runs the reveal must be the given real result, exactly. */
    static void finalAlwaysRealResult(List<String> pool) {
        for (int i = 0; i < 300; i++) {
            String real = pool.get(i % pool.size());
            String original = pool.get((i + 3) % pool.size());
            ReforgeAnimation a = new ReforgeAnimation(original, real, pool, 3.0, false);
            String lastShown = drive(a);
            check("final reveal == real result", real.equals(lastShown));
            check("stage locked to final", a.shownStage() == 3);
            check("finalName preserved", real.equals(a.finalName()));
        }
    }

    static void stagesAdvanceMonotonically(List<String> pool) {
        ReforgeAnimation a = new ReforgeAnimation("Sharp", "Spicy", pool, 1.0, false);
        long base = System.currentTimeMillis();
        int prev = 0;
        boolean ok = true;
        Set<Integer> seen = new HashSet<Integer>();
        for (int t = 0; t <= 12000; t += 16) {
            a.update(base + t);
            int st = a.shownStage();
            if (st < prev) ok = false;   // never regresses
            prev = st;
            seen.add(st);
            if (a.isDone()) break;
        }
        check("stage never regresses", ok);
        check("all four stages shown", seen.containsAll(Arrays.asList(0, 1, 2, 3)));
    }

    static void eachHitFiresExactlyOnce(List<String> pool) {
        ReforgeAnimation a = new ReforgeAnimation("Fair", "Legendary", pool, 1.0, false);
        long base = System.currentTimeMillis();
        int[] hits = new int[4];
        int breaks = 0, reveals = 0;
        for (int t = 0; t <= 12000; t += 8) {
            a.update(base + t);
            int h = a.hitLandedThisFrame();
            if (h >= 1 && h <= 3) hits[h]++;
            if (a.breakStartedThisFrame()) breaks++;
            if (a.revealStartedThisFrame()) reveals++;
            if (a.isDone()) break;
        }
        check("hit 1 fires once", hits[1] == 1);
        check("hit 2 fires once", hits[2] == 1);
        check("hit 3 fires once", hits[3] == 1);
        check("break fires once", breaks == 1);
        check("reveal fires once", reveals == 1);
    }

    static void intermediatesComeFromPool(List<String> pool) {
        Set<String> poolSet = new HashSet<String>(pool);
        for (int i = 0; i < 200; i++) {
            ReforgeAnimation a = new ReforgeAnimation("Sharp", "Spicy", pool, 2.0, false);
            long base = System.currentTimeMillis();
            Set<String> intermediates = new HashSet<String>();
            for (int t = 0; t <= 12000; t += 16) {
                a.update(base + t);
                if (a.shownStage() == 1 || a.shownStage() == 2) intermediates.add(a.shownName());
                if (a.isDone()) break;
            }
            for (String s : intermediates) {
                check("intermediate from pool: " + s, poolSet.contains(s));
            }
        }
    }

    /** A single-element or empty pool must not spin forever picking intermediates. */
    static void degeneratePoolsDoNotHang() {
        ReforgeAnimation single = new ReforgeAnimation("", "Spicy",
                Collections.singletonList("Spicy"), 3.0, false);
        check("single-element pool reveals result", "Spicy".equals(drive(single)));

        ReforgeAnimation empty = new ReforgeAnimation("", "Spicy",
                new ArrayList<String>(), 3.0, false);
        check("empty pool still reveals result", "Spicy".equals(drive(empty)));
    }

    static void reducedMotionStillReachesResult(List<String> pool) {
        ReforgeAnimation a = new ReforgeAnimation("Odd", "Heroic", pool, 1.0, true);
        check("reduced motion ends on real result", "Heroic".equals(drive(a)));
    }

    /** Runs an animation to completion; returns the last shown reforge name. */
    static String drive(ReforgeAnimation a) {
        long base = System.currentTimeMillis();
        String last = a.shownName();
        for (int t = 0; t <= 20000; t += 16) {
            a.update(base + t);
            last = a.shownName();
            if (a.isDone()) break;
        }
        return last;
    }

    static void check(String name, boolean cond) {
        if (cond) { passed++; }
        else { failed++; System.out.println("FAIL: " + name); }
    }
}
