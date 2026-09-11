package com.treegifts.core;

/**
 * Lifetime Tree Gift stats: how many of each rarity you've seen, the best rarity,
 * and the single rarest find (lowest drop %). Pure Java (no Minecraft) so it's
 * unit-testable; persistence is handled by {@link TreeGiftConfig}.
 */
public final class TreeGiftStats {

    public static final TreeGiftStats INSTANCE = new TreeGiftStats();

    private final long[] counts = new long[Rarity.values().length];
    private long total = 0L;
    private int bestOrdinal = -1;

    // The rarest single item ever revealed (smallest drop %).
    private String rarestName = "";
    private double rarestDrop = Double.MAX_VALUE;

    private TreeGiftStats() {}

    /** Record one revealed gift. */
    public void record(TreeGiftResult r) {
        if (r == null) return;
        int o = r.rarity.ordinal();
        counts[o]++;
        total++;
        if (o > bestOrdinal) bestOrdinal = o;
        if (r.hasDropChance() && r.dropChancePercent < rarestDrop) {
            rarestDrop = r.dropChancePercent;
            rarestName = r.itemName;
        }
    }

    public long total() { return total; }
    public long count(Rarity r) { return counts[r.ordinal()]; }
    public double share(Rarity r) { return total == 0 ? 0.0 : count(r) / (double) total; }
    public int bestOrdinal() { return bestOrdinal; }
    public Rarity best() { return bestOrdinal < 0 ? null : Rarity.values()[bestOrdinal]; }
    public String rarestName() { return rarestName; }
    public double rarestDrop() { return rarestDrop == Double.MAX_VALUE ? -1 : rarestDrop; }

    public long[] countsSnapshot() { return counts.clone(); }

    public void loadCounts(long[] loaded, long totalIn, int bestOrdinalIn,
                           String rarestNameIn, double rarestDropIn) {
        java.util.Arrays.fill(counts, 0L);
        if (loaded != null) {
            for (int i = 0; i < counts.length && i < loaded.length; i++) counts[i] = Math.max(0L, loaded[i]);
        }
        this.total = Math.max(0L, totalIn);
        this.bestOrdinal = bestOrdinalIn;
        this.rarestName = rarestNameIn == null ? "" : rarestNameIn;
        this.rarestDrop = rarestDropIn > 0 ? rarestDropIn : Double.MAX_VALUE;
    }

    public void reset() {
        java.util.Arrays.fill(counts, 0L);
        total = 0L;
        bestOrdinal = -1;
        rarestName = "";
        rarestDrop = Double.MAX_VALUE;
    }
}
