package com.spookybirch.dragon.data;

/**
 * A <b>simulation-only</b> model of Dragon Weight — the mechanic that decides
 * which major loot a player is eligible to roll for after a dragon fight.
 *
 * This never reads the real game state. It is a small data object the UI feeds
 * with made-up "what-if" inputs (eyes placed, damage rank, final blow) so it can
 * show the player how weight and eligibility work. It grants nothing in-game.
 *
 * <h3>Verified formula (Sept 2026 wiki)</h3>
 * <ul>
 *   <li>+100 weight per Summoning Eye placed.</li>
 *   <li>Damage rank: 1st +300, 2nd +250, 3rd +200, 4th-10th +125,
 *       11th-15th +100, 16th+ +75.</li>
 *   <li>Final blow: +50.</li>
 * </ul>
 * Note on discrepancy: an older Hypixel forum guide lists the damage brackets as
 * 4th-7th +125 and 8th-15th +100. This class uses the maintained wiki's current
 * brackets and flags the difference here rather than silently picking one.
 */
public final class DragonWeight {

    public static final int MAX_EYES = 8;

    private int eyes;       // 0..8
    private int damageRank; // 1..N (1 = top damage)
    private boolean finalBlow;

    public DragonWeight() {
        this.eyes = 4;
        this.damageRank = 1;
        this.finalBlow = false;
    }

    public int eyes() { return eyes; }
    public int damageRank() { return damageRank; }
    public boolean finalBlow() { return finalBlow; }

    public void setEyes(int v) { eyes = clamp(v, 0, MAX_EYES); }
    public void setDamageRank(int v) { damageRank = Math.max(1, v); }
    public void setFinalBlow(boolean v) { finalBlow = v; }

    public void addEyes(int delta) { setEyes(eyes + delta); }
    public void addRank(int delta) { setDamageRank(damageRank + delta); }
    public void toggleFinalBlow() { finalBlow = !finalBlow; }

    /** Weight contributed by placed Summoning Eyes. */
    public int eyeWeight() {
        return eyes * 100;
    }

    /** Weight contributed by the finishing damage rank. */
    public int rankWeight() {
        if (damageRank <= 1) return 300;
        if (damageRank == 2) return 250;
        if (damageRank == 3) return 200;
        if (damageRank <= 10) return 125;
        if (damageRank <= 15) return 100;
        return 75;
    }

    /** Weight from landing the killing blow. */
    public int finalBlowWeight() {
        return finalBlow ? 50 : 0;
    }

    /** Total simulated Dragon Weight. */
    public int total() {
        return eyeWeight() + rankWeight() + finalBlowWeight();
    }

    /** True if the given loot entry's weight requirement is met by this weight. */
    public boolean eligibleFor(LootEntry entry) {
        if (entry.weightReq == LootEntry.UNKNOWN) return false; // can't claim eligibility we can't verify
        return total() >= entry.weightReq;
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
