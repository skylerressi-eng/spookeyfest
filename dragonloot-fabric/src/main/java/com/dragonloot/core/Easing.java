package com.dragonloot.core;

/**
 * Easing curves for the reveal animation — the same functions slot machines,
 * gacha reveals and UI toolkits use to make motion feel weighty and smooth
 * instead of linear and cheap. All take a normalized time t in [0,1] and return
 * an eased value (usually in [0,1], a couple overshoot on purpose). Pure math,
 * no dependencies, so it's unit-tested alongside the gambling core.
 */
public final class Easing {

    private Easing() {}

    public static double clamp01(double t) {
        return t < 0 ? 0 : (t > 1 ? 1 : t);
    }

    /** Linear. */
    public static double linear(double t) {
        return clamp01(t);
    }

    /** Decelerate hard at the end — the classic "reel coasting to a stop". */
    public static double outCubic(double t) {
        t = clamp01(t);
        double f = 1 - t;
        return 1 - f * f * f;
    }

    /** Even harder deceleration — long, suspenseful tail. */
    public static double outQuint(double t) {
        t = clamp01(t);
        double f = 1 - t;
        return 1 - f * f * f * f * f;
    }

    /** Smooth S-curve, slow-in slow-out. */
    public static double inOutCubic(double t) {
        t = clamp01(t);
        return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    }

    /** Overshoots past 1 then settles — a satisfying "pop" for the reward card. */
    public static double outBack(double t) {
        t = clamp01(t);
        double c1 = 1.70158;
        double c3 = c1 + 1;
        double f = t - 1;
        return 1 + c3 * f * f * f + c1 * f * f;
    }

    /** Springy bounce-settle — for a jackpot flourish. */
    public static double outElastic(double t) {
        t = clamp01(t);
        if (t == 0 || t == 1) return t;
        double c4 = (2 * Math.PI) / 3;
        return Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * c4) + 1;
    }

    /** Interpolate between a and b by eased amount. */
    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /** Per-channel ARGB color blend, a -> b by t. Keeps full alpha. */
    public static int mixColor(int a, int b, double t) {
        t = clamp01(t);
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return 0xFF000000
                | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8)
                | (int) (ab + (bb - ab) * t);
    }
}
