package com.spookybirch.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.LongSupplier;

/**
 * The candy score tracker.
 *
 * Fed the current inventory candy totals each tick, it accumulates only the
 * POSITIVE changes as "collected" — so trading candy away, or picking up a
 * candy that stacks then splits, never makes the counters go backwards.
 *
 * From that it derives:
 *   - a weighted CANDY SCORE (purple counts as {@link #purpleWeight}),
 *   - an overall rate (score per hour over the whole session),
 *   - a live recent rate (score per hour over the last few minutes),
 *   - a best-recent-rate high score,
 *   - an ETA to a candy-score goal.
 *
 * This class has NO Minecraft/Forge dependency on purpose: SpookyConfig pushes
 * the weight and goal into it, and the clock is injectable — so the whole thing
 * can be unit- and stress-tested with plain Java.
 */
public final class CandyTracker {

    public static final CandyTracker INSTANCE = new CandyTracker();

    /** How long the "recent rate" window looks back, in ms. */
    private static final long WINDOW_MS = 5 * 60 * 1000L;

    /**
     * The recent window must span at least this long before it counts toward the
     * best-rate high score. Without this, grabbing one stack in the first couple
     * of seconds would report an absurd rate (e.g. 180k/hr) as your "best".
     */
    private static final long MIN_BEST_SPAN_MS = 60 * 1000L;

    /** Hard cap on stored samples — belt-and-braces against unbounded growth. */
    private static final int MAX_SAMPLES = 20_000;

    // Tunables, pushed in from config (kept local so this class stays pure).
    private double purpleWeight = 8.0;
    private int goal = 0;

    // Injectable clock (defaults to wall time; overridden in tests).
    private LongSupplier clock = System::currentTimeMillis;

    // Collected totals this session (monotonic — only ever go up).
    private int collectedGreen = 0;
    private int collectedPurple = 0;

    // Last inventory totals we saw, to compute deltas.
    private int lastGreen = -1;
    private int lastPurple = -1;

    private long sessionStartMs = 0L;
    private long lastGainMs = 0L;
    private double bestRecentRate = 0.0;
    private boolean started = false;

    // Sliding window of (timestamp, score) samples for the recent rate.
    private final Deque<long[]> samples = new ArrayDeque<long[]>(); // {timeMs, scoreMilli}

    /** Feed the tracker the current inventory candy totals. */
    public void update(int green, int purple) {
        // Defend against nonsense inputs.
        if (green < 0) green = 0;
        if (purple < 0) purple = 0;

        long now = clock.getAsLong();
        if (!started) {
            started = true;
            sessionStartMs = now;
            setLast(green, purple);
            pushSample(now);
            return;
        }
        // Ignore a clock that appears to run backwards (shouldn't happen, but a
        // negative dt would corrupt every rate).
        if (now < sessionStartMs) now = sessionStartMs;

        boolean gained = false;
        if (lastGreen >= 0 && green > lastGreen) { collectedGreen += green - lastGreen; gained = true; }
        if (lastPurple >= 0 && purple > lastPurple) { collectedPurple += purple - lastPurple; gained = true; }
        lastGreen = green;
        lastPurple = purple;
        if (gained) lastGainMs = now;

        pushSample(now);
        // Only let a stable, long-enough window set the best-rate high score.
        if (windowSpanMs() >= MIN_BEST_SPAN_MS) {
            double rr = recentRatePerHour();
            if (rr > bestRecentRate) bestRecentRate = rr;
        }
    }

    /** Time span currently covered by the recent-rate window, in ms. */
    private long windowSpanMs() {
        if (samples.size() < 2) return 0L;
        return samples.peekLast()[0] - samples.peekFirst()[0];
    }

    private void setLast(int green, int purple) {
        this.lastGreen = green;
        this.lastPurple = purple;
    }

    private void pushSample(long now) {
        // score in "milli" (score * 1000) so we can store it as a long.
        samples.addLast(new long[] { now, Math.round(score() * 1000.0) });
        while (!samples.isEmpty() && now - samples.peekFirst()[0] > WINDOW_MS) {
            samples.pollFirst();
        }
        // Safety valve: if updates ever come faster than expected, never let the
        // window grow without bound.
        while (samples.size() > MAX_SAMPLES) {
            samples.pollFirst();
        }
    }

    /** Weighted candy score: green + purple × purpleWeight. */
    public double score() {
        return collectedGreen + collectedPurple * purpleWeight;
    }

    public int collectedGreen()  { return collectedGreen; }
    public int collectedPurple() { return collectedPurple; }

    public long elapsedMs() {
        if (!started) return 0L;
        long ms = clock.getAsLong() - sessionStartMs;
        return ms < 0L ? 0L : ms;
    }

    /** Whole-session score per hour. */
    public double overallRatePerHour() {
        long ms = elapsedMs();
        if (ms < 1000L) return 0.0;
        return score() / (ms / 3_600_000.0);
    }

    /** Score per hour over the last {@link #WINDOW_MS}. */
    public double recentRatePerHour() {
        if (samples.size() < 2) return 0.0;
        long[] first = samples.peekFirst();
        long[] last = samples.peekLast();
        long dt = last[0] - first[0];
        if (dt < 1000L) return 0.0;
        double dScore = (last[1] - first[1]) / 1000.0;
        return dScore / (dt / 3_600_000.0);
    }

    public double bestRecentRate() { return bestRecentRate; }

    /** Seconds since the last candy gain — used to show an "idle" hint. */
    public long secondsSinceGain() {
        if (lastGainMs == 0L) return -1L;
        long s = (clock.getAsLong() - lastGainMs) / 1000L;
        return s < 0L ? 0L : s;
    }

    /**
     * ETA in seconds to reach the candy-score goal at the recent rate, or -1 if
     * we can't estimate (no goal, or no rate yet). Returns 0 when already hit.
     */
    public long etaSecondsToGoal() {
        if (goal <= 0) return -1L;
        double remaining = goal - score();
        if (remaining <= 0) return 0L;
        double rate = recentRatePerHour();
        if (rate <= 0.01) return -1L;
        return Math.round(remaining / rate * 3600.0);
    }

    public void reset() {
        collectedGreen = 0;
        collectedPurple = 0;
        sessionStartMs = clock.getAsLong();
        lastGainMs = 0L;
        bestRecentRate = 0.0;
        samples.clear();
        samples.addLast(new long[] { sessionStartMs, 0L });
        // keep lastGreen/lastPurple so the next tick doesn't count existing
        // inventory as a fresh gain.
        started = true;
    }

    // --- config plumbing (SpookyConfig pushes these in) ---

    public void setPurpleWeight(double w) {
        this.purpleWeight = w > 0 ? w : 1.0;
    }

    public void setGoal(int g) {
        this.goal = g < 0 ? 0 : g;
    }

    public double purpleWeight() { return purpleWeight; }
    public int goal() { return goal; }

    // --- test seam: swap the clock and start from a clean slate ---

    /** Package-visible so the stress-test harness can drive a fake clock. */
    void setClockForTest(LongSupplier c) {
        this.clock = c;
        this.collectedGreen = 0;
        this.collectedPurple = 0;
        this.lastGreen = -1;
        this.lastPurple = -1;
        this.sessionStartMs = 0L;
        this.lastGainMs = 0L;
        this.bestRecentRate = 0.0;
        this.started = false;
        this.samples.clear();
    }

    int sampleCount() { return samples.size(); }

    private CandyTracker() {}
}
