package com.spookybirch.reforge;

import java.util.List;
import java.util.Random;

/**
 * The cinematic state machine for one reforge gamble. Purely visual: it owns no
 * game state and never touches the item. Built once the real result is known,
 * then advanced by wall-clock time so it stays smooth regardless of tick rate.
 *
 * <p>Sequence: intro → 3 hammer hits (each swaps the shown reforge) → the third
 * lands on the <b>real</b> reforge → anvil cracks → breaks → pause → reveal.
 * The first two shown reforges are believable fakes from the item's category
 * pool; only the third is real.
 *
 * <p>All particles live in fixed pre-allocated pools, so a running animation
 * allocates nothing per frame.
 */
public final class ReforgeAnimation {

    public enum Phase {
        INTRO,
        HIT1_RAISE, HIT1_DROP, HIT1_REACT,
        HIT2_RAISE, HIT2_DROP, HIT2_REACT,
        HIT3_RAISE, HIT3_DROP, HIT3_IMPACT,
        CRACK, BREAK, PAUSE,
        REVEAL_IN, REVEAL_HOLD, REVEAL_FADE,
        DONE
    }

    // --- config-derived ---
    private final boolean reducedMotion;

    // --- content ---
    private final String originalName;      // reforge before the reroll (may be "")
    private final String[] intermediate = new String[2];
    private final String finalName;         // the REAL reforge (authoritative)

    // --- timeline ---
    private final Phase[] order;
    private final long[] dur;                // ms per phase (already speed/motion scaled)
    private final long total;
    private final long startMs;
    private long lastUpdateMs;

    // --- live frame values (read by the renderer) ---
    private Phase phase = Phase.INTRO;
    private double phaseT;                   // 0..1 within the current phase
    private String shownName;                // reforge label to display right now
    private int shownStage;                  // 0=original,1=A,2=B,3=final
    private double hammerAngleDeg;           // rotation of the hammer about its pivot
    private double crackLevel;               // 0..1 how cracked the anvil looks
    private double breakProgress;            // 0..1 anvil fragmenting
    private double revealScale = 1.0;        // reveal name pop
    private double revealAlpha;              // 0..1 reveal opacity
    private double shake;                    // current screen-shake magnitude (px)

    // --- one-shot events for the frame just computed (for sounds) ---
    private int hitLandedThisFrame;          // 1,2,3 or 0
    private boolean breakStartedThisFrame;
    private boolean revealStartedThisFrame;
    private int impactsFired;                // 0..3 already-fired hammer impacts
    private boolean breakFired;
    private boolean revealFired;

    // --- particles ---
    private final Spark[] sparks = new Spark[96];
    private final Frag[] frags = new Frag[18];
    private final Random rng = new Random();

    public ReforgeAnimation(String originalDisplay, String finalDisplay,
                            List<String> pool, double speed, boolean reducedMotion) {
        this.reducedMotion = reducedMotion;
        this.originalName = originalDisplay == null ? "" : originalDisplay;
        this.finalName = finalDisplay == null ? "" : finalDisplay;
        // Particle pools must exist before we (potentially) spawn into them.
        for (int i = 0; i < sparks.length; i++) sparks[i] = new Spark();
        for (int i = 0; i < frags.length; i++) frags[i] = new Frag();
        pickIntermediates(pool);

        double sp = speed <= 0 ? 1.0 : speed;
        double m = reducedMotion ? 0.55 : 1.0;
        // Base durations (ms) at speed 1.0, full motion.
        this.order = Phase.values();
        this.dur = new long[order.length];
        set(Phase.INTRO,       450, sp, m);
        set(Phase.HIT1_RAISE,  320, sp, m);
        set(Phase.HIT1_DROP,   110, sp, m);
        set(Phase.HIT1_REACT,  430, sp, m);
        set(Phase.HIT2_RAISE,  300, sp, m);
        set(Phase.HIT2_DROP,   105, sp, m);
        set(Phase.HIT2_REACT,  430, sp, m);
        set(Phase.HIT3_RAISE,  540, sp, m);   // wind up higher for the finale
        set(Phase.HIT3_DROP,    95, sp, m);
        set(Phase.HIT3_IMPACT, 260, sp, m);
        set(Phase.CRACK,       360, sp, m);
        set(Phase.BREAK,       660, sp, m);
        set(Phase.PAUSE,       220, sp, m);
        set(Phase.REVEAL_IN,   360, sp, m);
        // Reveal hold is deliberately not sped up much — it's the payoff.
        this.dur[Phase.REVEAL_HOLD.ordinal()] = reducedMotion ? 800 : 1400;
        set(Phase.REVEAL_FADE, 380, sp, m);
        this.dur[Phase.DONE.ordinal()] = 0;

        long t = 0;
        for (long d : dur) t += d;
        this.total = t;

        long now = System.currentTimeMillis();
        this.startMs = now;
        this.lastUpdateMs = now;

        this.shownName = originalName;
        this.shownStage = 0;
    }

    private void set(Phase p, long baseMs, double speed, double motion) {
        dur[p.ordinal()] = Math.max(1, Math.round(baseMs * motion / speed));
    }

    // ------------------------------------------------------------------ update

    /** Advance to wall-clock {@code now}. Call once per rendered frame. */
    public void update(long now) {
        double dt = Math.min(0.1, (now - lastUpdateMs) / 1000.0); // clamp lag spikes
        lastUpdateMs = now;

        hitLandedThisFrame = 0;
        breakStartedThisFrame = false;
        revealStartedThisFrame = false;

        long elapsed = now - startMs;
        // Locate current phase.
        long acc = 0;
        Phase cur = Phase.DONE;
        double localT = 1.0;
        for (Phase p : order) {
            long d = dur[p.ordinal()];
            if (elapsed < acc + d || p == Phase.DONE) {
                cur = p;
                localT = d <= 0 ? 1.0 : (elapsed - acc) / (double) d;
                break;
            }
            acc += d;
        }
        if (elapsed >= total) { cur = Phase.DONE; localT = 1.0; }
        phase = cur;
        phaseT = clamp01(localT);

        computeHammer();
        computeShownReforge();
        computeCrackBreakReveal();
        fireOneShots();
        updateParticles(dt);
    }

    private void computeHammer() {
        // Angles in degrees about the hammer's pivot. Rest ~ -22 (cocked back a
        // touch), raised ~ -95 (up & back), impact ~ +12 (struck down onto anvil).
        final double REST = -22, RAISED = -95, STRUCK = 12;
        switch (phase) {
            case INTRO:      hammerAngleDeg = lerp(REST, REST - 6, easeInOut(phaseT)); break;
            case HIT1_RAISE:
            case HIT2_RAISE: hammerAngleDeg = lerp(REST, RAISED, easeOutQuad(phaseT)); break;
            case HIT3_RAISE: hammerAngleDeg = lerp(REST, RAISED - 12, easeOutQuad(phaseT)); break;
            case HIT1_DROP:
            case HIT2_DROP:
            case HIT3_DROP:  hammerAngleDeg = lerp(RAISED, STRUCK, easeInQuad(phaseT)); break;
            case HIT1_REACT:
            case HIT2_REACT:
            case HIT3_IMPACT:
                // Bounce back toward rest with a small damped overshoot.
                hammerAngleDeg = STRUCK + (REST - STRUCK) * easeOutBack(phaseT);
                break;
            default:         hammerAngleDeg = REST; break;
        }
    }

    private void computeShownReforge() {
        // The label swaps at the *instant* each hit lands (start of the react).
        switch (phase) {
            case INTRO: case HIT1_RAISE: case HIT1_DROP:
                shownName = originalName; shownStage = 0; break;
            case HIT1_REACT: case HIT2_RAISE: case HIT2_DROP:
                shownName = intermediate[0]; shownStage = 1; break;
            case HIT2_REACT: case HIT3_RAISE: case HIT3_DROP:
                shownName = intermediate[1]; shownStage = 2; break;
            default:
                shownName = finalName; shownStage = 3; break;
        }
    }

    private void computeCrackBreakReveal() {
        crackLevel = 0; breakProgress = 0; revealAlpha = 0; revealScale = 1.0;
        switch (phase) {
            case HIT3_IMPACT: crackLevel = 0.35 * phaseT; break;
            case CRACK:       crackLevel = 0.35 + 0.65 * easeInQuad(phaseT); break;
            case BREAK:       crackLevel = 1.0; breakProgress = easeInQuad(phaseT); break;
            case PAUSE:       crackLevel = 1.0; breakProgress = 1.0; break;
            case REVEAL_IN:
                crackLevel = 1.0; breakProgress = 1.0;
                revealAlpha = easeOutQuad(phaseT);
                revealScale = 0.4 + 0.6 * easeOutBack(phaseT); // pop up
                break;
            case REVEAL_HOLD:
                crackLevel = 1.0; breakProgress = 1.0;
                revealAlpha = 1.0;
                revealScale = 1.0 + 0.02 * Math.sin((lastUpdateMs % 1600) / 1600.0 * Math.PI * 2);
                break;
            case REVEAL_FADE:
                crackLevel = 1.0; breakProgress = 1.0;
                revealAlpha = 1.0 - easeInQuad(phaseT);
                revealScale = 1.0;
                break;
            default: break;
        }

        // Screen shake: strong right after each impact, decaying quickly.
        double s = 0;
        if (phase == Phase.HIT1_REACT || phase == Phase.HIT2_REACT) {
            s = (reducedMotion ? 1.4 : 3.0) * Math.exp(-6.5 * phaseT);
        } else if (phase == Phase.HIT3_IMPACT) {
            s = (reducedMotion ? 2.4 : 6.5) * Math.exp(-4.5 * phaseT);
        } else if (phase == Phase.BREAK) {
            s = (reducedMotion ? 1.6 : 4.0) * Math.exp(-3.0 * phaseT);
        }
        shake = s;
    }

    private void fireOneShots() {
        // Fire an impact once, at the boundary where the hammer lands (the start
        // of each react/impact phase). Ordinal ordering makes "have we reached
        // phase X yet" a simple comparison.
        if (impactsFired < 1 && reached(Phase.HIT1_REACT)) { impactsFired = 1; hitLandedThisFrame = 1; spawnSparks(28, 1.0); }
        if (impactsFired < 2 && reached(Phase.HIT2_REACT)) { impactsFired = 2; hitLandedThisFrame = 2; spawnSparks(34, 1.2); }
        if (impactsFired < 3 && reached(Phase.HIT3_IMPACT)) { impactsFired = 3; hitLandedThisFrame = 3; spawnSparks(64, 2.1); }
        if (!breakFired && reached(Phase.BREAK)) { breakFired = true; breakStartedThisFrame = true; spawnFragments(); spawnSparks(40, 1.6); }
        if (!revealFired && reached(Phase.REVEAL_IN)) { revealFired = true; revealStartedThisFrame = true; spawnSparks(30, 1.3); }
    }

    private boolean reached(Phase p) {
        return phase.ordinal() >= p.ordinal();
    }

    // --------------------------------------------------------------- particles

    private void spawnSparks(int count, double power) {
        if (reducedMotion) count = Math.max(4, count / 3);
        for (int i = 0; i < count; i++) {
            Spark s = freeSpark();
            if (s == null) return;
            double ang = -Math.PI / 2 + (rng.nextDouble() - 0.5) * Math.PI * 1.1; // upward fan
            double speed = (0.6 + rng.nextDouble() * 1.6) * power;
            s.active = true;
            s.x = 0; s.y = 0;
            s.vx = Math.cos(ang) * speed * 60;
            s.vy = Math.sin(ang) * speed * 60;
            s.life = s.maxLife = 0.35 + rng.nextDouble() * 0.5;
            s.size = 1 + rng.nextInt(2);
            // Ember palette: white-hot -> orange -> red.
            double h = rng.nextDouble();
            s.color = h > 0.7 ? 0xFFFFF3C0 : (h > 0.35 ? 0xFFFFB030 : 0xFFFF6020);
        }
    }

    private void spawnFragments() {
        for (int i = 0; i < frags.length; i++) {
            Frag f = frags[i];
            f.active = true;
            f.x = (i % 3 - 1) * 10 + (rng.nextDouble() - 0.5) * 6;
            f.y = -(i / 3) * 6 - rng.nextDouble() * 4;
            f.vx = (rng.nextDouble() - 0.5) * 120;
            f.vy = -(40 + rng.nextDouble() * 90);
            f.vr = (rng.nextDouble() - 0.5) * 720;
            f.rot = rng.nextDouble() * 360;
            f.size = 4 + rng.nextInt(6);
            f.life = f.maxLife = 0.7 + rng.nextDouble() * 0.4;
        }
    }

    private void updateParticles(double dt) {
        for (Spark s : sparks) {
            if (!s.active) continue;
            s.life -= dt;
            if (s.life <= 0) { s.active = false; continue; }
            s.vy += 260 * dt;            // gravity
            s.x += s.vx * dt;
            s.y += s.vy * dt;
            s.vx *= 0.98;
        }
        for (Frag f : frags) {
            if (!f.active) continue;
            f.life -= dt;
            if (f.life <= 0) { f.active = false; continue; }
            f.vy += 320 * dt;
            f.x += f.vx * dt;
            f.y += f.vy * dt;
            f.rot += f.vr * dt;
        }
    }

    private Spark freeSpark() {
        for (Spark s : sparks) if (!s.active) return s;
        return null;
    }

    private void pickIntermediates(List<String> pool) {
        // Two believable, different-from-each-other and (where possible)
        // different-from-final reforges from the item's real pool.
        String a = pick(pool, finalName, null);
        String b = pick(pool, finalName, a);
        intermediate[0] = a;
        intermediate[1] = b;
    }

    private String pick(List<String> pool, String avoid1, String avoid2) {
        if (pool.isEmpty()) return finalName;
        for (int tries = 0; tries < 12; tries++) {
            String c = pool.get(rng.nextInt(pool.size()));
            if (c.equalsIgnoreCase(avoid1)) continue;
            if (avoid2 != null && c.equalsIgnoreCase(avoid2)) continue;
            return c;
        }
        return pool.get(rng.nextInt(pool.size()));
    }

    // ------------------------------------------------------------------ getters

    public boolean isDone() { return phase == Phase.DONE; }
    public Phase phase() { return phase; }
    public double phaseT() { return phaseT; }
    public String shownName() { return shownName; }
    public int shownStage() { return shownStage; }
    public String finalName() { return finalName; }
    public double hammerAngleDeg() { return hammerAngleDeg; }
    public double crackLevel() { return crackLevel; }
    public double breakProgress() { return breakProgress; }
    public double revealScale() { return revealScale; }
    public double revealAlpha() { return revealAlpha; }
    public double shake() { return shake; }
    public Spark[] sparks() { return sparks; }
    public Frag[] frags() { return frags; }

    /** Which hit (1/2/3) landed on the frame just computed, else 0. */
    public int hitLandedThisFrame() { return hitLandedThisFrame; }
    public boolean breakStartedThisFrame() { return breakStartedThisFrame; }
    public boolean revealStartedThisFrame() { return revealStartedThisFrame; }

    /** True once the reveal has begun — used to lock in the real result. */
    public boolean pastPointOfNoReturn() { return phase.ordinal() >= Phase.CRACK.ordinal(); }

    // ------------------------------------------------------------------ helpers

    public static double clamp01(double v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }
    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
    private static double easeOutQuad(double t) { return 1 - (1 - t) * (1 - t); }
    private static double easeInQuad(double t) { return t * t; }
    private static double easeInOut(double t) { return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2; }
    private static double easeOutBack(double t) {
        double c1 = 1.70158, c3 = c1 + 1;
        return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
    }

    /** A flying ember. Coordinates are relative to the anvil impact point. */
    public static final class Spark {
        public boolean active;
        public double x, y, vx, vy, life, maxLife;
        public int size, color;
        public float alpha() { return (float) clamp01(life / maxLife); }
    }

    /** A chunk of shattered anvil. */
    public static final class Frag {
        public boolean active;
        public double x, y, vx, vy, rot, vr, life, maxLife;
        public int size;
        public float alpha() { return (float) clamp01(life / maxLife); }
    }
}
