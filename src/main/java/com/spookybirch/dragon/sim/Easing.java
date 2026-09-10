package com.spookybirch.dragon.sim;

/**
 * Small collection of easing functions for the roulette animation. All take and
 * return a normalised progress t in [0, 1] (except {@link #dampedBounce}). Kept
 * allocation-free so they can be called every frame.
 */
public final class Easing {

    public static double clamp01(double t) {
        return t < 0 ? 0 : (t > 1 ? 1 : t);
    }

    public static double easeInQuad(double t) {
        t = clamp01(t);
        return t * t;
    }

    public static double easeOutCubic(double t) {
        t = clamp01(t);
        double u = 1 - t;
        return 1 - u * u * u;
    }

    public static double easeInOutQuad(double t) {
        t = clamp01(t);
        return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
    }

    /** Overshooting ease used for the result panel "pop". */
    public static double easeOutBack(double t) {
        t = clamp01(t);
        final double c1 = 1.70158;
        final double c3 = c1 + 1;
        double u = t - 1;
        return 1 + c3 * u * u * u + c1 * u * u;
    }

    /**
     * The spin trajectory: monotonic 0-&gt;1 that accelerates for the first
     * {@code accelFrac} of time (quadratic ease-in) then decelerates for the
     * remainder (cubic ease-out). Reaching exactly 1 at t=1 is what lets the
     * ball land precisely on its target segment with no snap.
     *
     * @param t         normalised time over accel+spin+decel
     * @param accelFrac fraction of time spent accelerating
     * @param accelDist fraction of total distance covered during accel
     */
    public static double spinProfile(double t, double accelFrac, double accelDist) {
        t = clamp01(t);
        if (t < accelFrac) {
            double l = t / accelFrac;
            return accelDist * l * l;
        }
        double l = (t - accelFrac) / (1 - accelFrac);
        double u = 1 - l;
        return accelDist + (1 - accelDist) * (1 - u * u * u);
    }

    /**
     * A decaying oscillation for the ball settling into its pocket. Returns an
     * offset that starts near {@code amplitude} and decays to ~0 as t-&gt;1.
     *
     * @param t         normalised time over the settle window
     * @param amplitude peak offset (e.g. degrees)
     * @param waves     roughly how many bounces
     */
    public static double dampedBounce(double t, double amplitude, double waves) {
        t = clamp01(t);
        double decay = Math.exp(-5.0 * t);
        return amplitude * decay * Math.cos(waves * Math.PI * t);
    }

    private Easing() {}
}
