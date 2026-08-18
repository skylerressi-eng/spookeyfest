package com.spookybirch.core;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The candy score tracker.
 *
 * Fed the current inventory candy totals each tick, it accumulates only the
 * POSITIVE changes as "collected" — so trading candy away, or picking up a
 * candy that stacks then splits, never makes the counters go backwards.
 *
 * From that it derives:
 *   - a weighted CANDY SCORE (purple counts as {@link SpookyConfig#purpleWeight}),
 *   - an overall rate (score per hour over the whole session),
 *   - a live recent rate (score per hour over the last few minutes),
 *   - a best-recent-rate high score,
 *   - an ETA to a configurable candy-score goal.
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
        long now = System.currentTimeMillis();
        if (!started) {
            started = true;
            sessionStartMs = now;
            setLast(green, purple);
            pushSample(now);
            return;
        }
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
    }

    /** Weighted candy score: green + purple × purpleWeight. */
    public double score() {
        return collectedGreen + collectedPurple * SpookyConfig.INSTANCE.purpleWeight;
    }

    public int collectedGreen()  { return collectedGreen; }
    public int collectedPurple() { return collectedPurple; }

    public long elapsedMs() {
        if (!started) return 0L;
        return System.currentTimeMillis() - sessionStartMs;
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
        return (System.currentTimeMillis() - lastGainMs) / 1000L;
    }

    /**
     * ETA in seconds to reach the configured candy-score goal at the recent
     * rate, or -1 if we can't estimate (no goal, already hit, or no rate yet).
     */
    public long etaSecondsToGoal() {
        int goal = SpookyConfig.INSTANCE.candyGoal;
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
        sessionStartMs = System.currentTimeMillis();
        lastGainMs = 0L;
        bestRecentRate = 0.0;
        samples.clear();
        samples.addLast(new long[] { sessionStartMs, 0L });
        // keep lastGreen/lastPurple so the next tick doesn't count existing
        // inventory as a fresh gain.
        started = true;
    }

    private CandyTracker() {}
}
