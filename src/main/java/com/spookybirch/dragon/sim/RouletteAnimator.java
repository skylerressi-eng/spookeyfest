package com.spookybirch.dragon.sim;

import com.spookybirch.dragon.data.DragonLoot;

/**
 * Drives the roulette animation on a wall-clock timeline so motion is smooth and
 * frame-rate independent. The wheel and ball move on independent trajectories
 * (opposite directions, different rotation counts) but share one state machine
 * (see {@link AnimationState}).
 *
 * <h3>Exact landing</h3>
 * The target segment is chosen up front, then the wheel's final angle is solved
 * so that segment sits under the top pointer, and the ball's final angle is
 * solved so it rests at the pointer. The whole accelerate → cruise → decelerate
 * arc is a single eased interpolation toward those finals ({@link
 * Easing#spinProfile}), which guarantees a precise, snap-free landing. A damped
 * bounce then settles the ball into the pocket.
 *
 * All state lives in primitive fields; {@link #update} allocates nothing.
 */
public final class RouletteAnimator {

    // Angle convention: degrees, 0 = top (12 o'clock), clockwise positive.

    // Base phase durations in ms (before anim-speed / reduced-motion scaling).
    private static final double D_STARTING = 250;
    private static final double D_ACCEL = 800;
    private static final double D_SPIN = 900;
    private static final double D_DECEL = 2200;
    private static final double D_SETTLE = 650;
    private static final double D_REVEAL = 500;

    private static final int WHEEL_TURNS = 5;   // clockwise full turns
    private static final int BALL_TURNS = 8;    // counter-clockwise full turns
    private static final double WINDUP = 10.0;  // degrees of anticipation
    private static final double BOUNCE_AMP = 7.0;
    private static final double IDLE_DRIFT = 6.0; // deg/sec

    // Easing shape for the spin arc: ~20% of time accelerating, covering ~22%
    // of the distance, then a long cubic ease-out.
    private static final double ACCEL_FRAC = 0.20;
    private static final double ACCEL_DIST = 0.22;

    private AnimationState state = AnimationState.IDLE;

    private double animSpeed = 1.0;
    private boolean reducedMotion = false;

    // Current render angles.
    private double wheelAngle = 0;
    private double ballAngle = 0;

    // Trajectory endpoints for the active spin.
    private double wheelStart, wheelWindup, wheelFinal;
    private double ballStart, ballWindup, ballFinal;

    private long spinStartNanos;
    private long lastNanos;

    private DragonLoot loot;
    private RouletteResult result;

    public AnimationState state() { return state; }
    public double wheelAngle() { return wheelAngle; }
    public double ballAngle() { return ballAngle; }
    public RouletteResult result() { return result; }
    public boolean idle() { return state == AnimationState.IDLE; }

    public void setAnimSpeed(double s) {
        animSpeed = s < 0.25 ? 0.25 : (s > 3.0 ? 3.0 : s);
    }

    public void setReducedMotion(boolean r) { reducedMotion = r; }

    /** Scaled phase duration (reduced motion shortens everything). */
    private double dur(double base) {
        double d = base / animSpeed;
        if (reducedMotion) d *= 0.55;
        return d;
    }

    /**
     * Begin a spin. Picks the target segment (deterministic if {@code seed} is
     * non-null) and solves the landing geometry.
     */
    public void startSpin(DragonLoot loot, Long seed) {
        if (loot == null || loot.size() == 0) return;
        this.loot = loot;

        long usedSeed = (seed != null) ? seed : RouletteSimulator.freshSeed();
        int target = RouletteSimulator.pick(loot, usedSeed);
        this.result = new RouletteResult(loot.type, loot.get(target), target, usedSeed);

        int n = loot.size();
        double segAngle = 360.0 / n;
        double segCenter = (target + 0.5) * segAngle; // base angle of target centre

        // Wheel: spin clockwise so the target centre ends under the top pointer
        // (screen angle 0). finalWheel ≡ -segCenter (mod 360).
        wheelStart = wheelAngle;
        wheelWindup = wheelStart - WINDUP * 0.5;
        int wheelTurns = reducedMotion ? Math.max(2, WHEEL_TURNS - 2) : WHEEL_TURNS;
        double afterTurns = wheelWindup + wheelTurns * 360.0;
        double targetMod = norm360(-segCenter);
        double delta = targetMod - norm360(afterTurns);
        if (delta < 0) delta += 360.0;
        wheelFinal = afterTurns + delta;

        // Ball: orbit counter-clockwise and come to rest at the pointer
        // (finalBall ≡ 0 mod 360).
        ballStart = ballAngle;
        ballWindup = ballStart + WINDUP;
        int ballTurns = reducedMotion ? Math.max(3, BALL_TURNS - 3) : BALL_TURNS;
        double ballBase = ballWindup - ballTurns * 360.0;
        ballFinal = ballBase - norm360(ballBase); // snap down to ≡ 0 (mod 360)

        spinStartNanos = System.nanoTime();
        state = AnimationState.STARTING;
    }

    /** Return to idle so the wheel drifts and a new spin can start. */
    public void reset() {
        state = AnimationState.IDLE;
        ballAngle = 0;
        result = null;
    }

    /** Advance the animation to wall-clock time {@code nowNanos}. */
    public void update(long nowNanos) {
        double dtSec = lastNanos == 0 ? 0 : (nowNanos - lastNanos) / 1_000_000_000.0;
        lastNanos = nowNanos;

        if (state == AnimationState.IDLE) {
            wheelAngle += IDLE_DRIFT * dtSec;
            if (wheelAngle > 1e7) wheelAngle = norm360(wheelAngle); // guard long sessions
            return;
        }

        double e = (nowNanos - spinStartNanos) / 1_000_000.0; // ms since spin start
        double tStart = dur(D_STARTING);
        double tAccel = dur(D_ACCEL);
        double tSpin = dur(D_SPIN);
        double tDecel = dur(D_DECEL);
        double tSettle = dur(D_SETTLE);
        double tReveal = dur(D_REVEAL);

        double arcLen = tAccel + tSpin + tDecel;
        double m0 = tStart;
        double m1 = m0 + arcLen;     // end of decelerate
        double m2 = m1 + tSettle;    // end of settle
        double m3 = m2 + tReveal;    // end of reveal pop

        if (e < m0) {
            // STARTING — wind up.
            double s = Easing.easeOutCubic(e / tStart);
            wheelAngle = wheelStart + (wheelWindup - wheelStart) * s;
            ballAngle = ballStart + (ballWindup - ballStart) * s;
            state = AnimationState.STARTING;
        } else if (e < m1) {
            // The spin arc — one eased interpolation to the finals.
            double arcE = e - m0;
            double p = arcE / arcLen;
            double prof = Easing.spinProfile(p, ACCEL_FRAC, ACCEL_DIST);
            wheelAngle = wheelWindup + (wheelFinal - wheelWindup) * prof;
            ballAngle = ballWindup + (ballFinal - ballWindup) * prof;
            state = arcE < tAccel ? AnimationState.ACCELERATING
                    : (arcE < tAccel + tSpin ? AnimationState.SPINNING : AnimationState.DECELERATING);
        } else if (e < m2) {
            // BALL_SETTLING — wheel locked, ball bounces into the pocket.
            wheelAngle = wheelFinal;
            double st = (e - m1) / tSettle;
            ballAngle = ballFinal + (reducedMotion ? 0 : Easing.dampedBounce(st, BOUNCE_AMP, 3));
            state = AnimationState.BALL_SETTLING;
        } else {
            wheelAngle = wheelFinal;
            ballAngle = ballFinal;
            state = (e < m3) ? AnimationState.RESULT_REVEAL : AnimationState.RESULT_DISPLAY;
        }
    }

    /** 0-1 progress through the current spin arc (for effect intensity). */
    public double spinProgress() {
        if (!state.spinning()) return state.showingResult() ? 1.0 : 0.0;
        double e = (System.nanoTime() - spinStartNanos) / 1_000_000.0;
        double arcLen = dur(D_ACCEL) + dur(D_SPIN) + dur(D_DECEL);
        double arcE = e - dur(D_STARTING);
        return Easing.clamp01(arcE / arcLen);
    }

    /** 0-1 progress of the result reveal pop (1 once fully displayed). */
    public double revealProgress() {
        if (state == AnimationState.RESULT_DISPLAY) return 1.0;
        if (state != AnimationState.RESULT_REVEAL) return 0.0;
        double e = (System.nanoTime() - spinStartNanos) / 1_000_000.0;
        double m2 = dur(D_STARTING) + dur(D_ACCEL) + dur(D_SPIN) + dur(D_DECEL) + dur(D_SETTLE);
        return Easing.clamp01((e - m2) / dur(D_REVEAL));
    }

    private static double norm360(double a) {
        double r = a % 360.0;
        return r < 0 ? r + 360.0 : r;
    }
}
