package com.dragonloot.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Lifetime record of your egg gambles: how many you've cracked, how many landed
 * at each rarity, your best pull, and a short rolling history. Dependency-free
 * singleton; {@link DragonConfig} loads/saves the counts. The history deque is
 * memory-bounded.
 */
public final class DragonStats {

    public static final DragonStats INSTANCE = new DragonStats();

    private static final int HISTORY_CAP = 20;

    private final long[] counts = new long[Rarity.values().length];
    private long total = 0L;
    private Rarity best = null;
    private final Deque<Rarity> history = new ArrayDeque<Rarity>();

    /** Record a completed roll. Returns true if it set a new personal best. */
    public boolean record(Rarity r) {
        if (r == null) return false;
        counts[r.ordinal()]++;
        total++;
        history.addLast(r);
        while (history.size() > HISTORY_CAP) history.removeFirst();
        if (best == null || r.ordinal() > best.ordinal()) {
            best = r;
            return true;
        }
        return false;
    }

    public long total() { return total; }

    public long count(Rarity r) { return r == null ? 0L : counts[r.ordinal()]; }

    public double share(Rarity r) {
        if (total == 0L || r == null) return 0.0;
        return (double) counts[r.ordinal()] / (double) total;
    }

    public Rarity best() { return best; }

    public List<Rarity> history() { return new ArrayList<Rarity>(history); }

    public int historySize() { return history.size(); }

    public void reset() {
        for (int i = 0; i < counts.length; i++) counts[i] = 0L;
        total = 0L;
        best = null;
        history.clear();
    }

    // ---- persistence hooks ----

    public void loadCounts(long[] savedCounts, long savedTotal, int bestOrdinal) {
        reset();
        if (savedCounts != null) {
            for (int i = 0; i < counts.length && i < savedCounts.length; i++) {
                counts[i] = Math.max(0L, savedCounts[i]);
            }
        }
        total = Math.max(0L, savedTotal);
        best = (bestOrdinal >= 0 && bestOrdinal < Rarity.values().length)
                ? Rarity.values()[bestOrdinal] : null;
    }

    public long[] countsSnapshot() { return counts.clone(); }

    public int bestOrdinal() { return best == null ? -1 : best.ordinal(); }
}
