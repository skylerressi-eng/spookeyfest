package com.spookybirch.dragon.sim;

/**
 * The roulette animation state machine, in order:
 *
 * <pre>
 *   IDLE → STARTING → ACCELERATING → SPINNING → DECELERATING
 *        → BALL_SETTLING → RESULT_REVEAL → RESULT_DISPLAY → IDLE
 * </pre>
 *
 * The wheel and ball share this timeline but move on independent trajectories
 * (opposite directions, different rotation counts). {@link RouletteAnimator}
 * owns the transitions; screens read the current state to drive effects.
 */
public enum AnimationState {
    /** At rest; wheel drifts slowly for life. */
    IDLE,
    /** Brief anticipation / wind-up before the spin. */
    STARTING,
    /** Ball speeds up (quadratic ease-in). */
    ACCELERATING,
    /** Ball at cruising speed. */
    SPINNING,
    /** Ball slows toward its target (cubic ease-out). */
    DECELERATING,
    /** Ball bounces and settles into the winning pocket. */
    BALL_SETTLING,
    /** Result panel pops in with rarity-scaled effects. */
    RESULT_REVEAL,
    /** Result held on screen until replay/close. */
    RESULT_DISPLAY;

    /** True once a result has been chosen and is being shown. */
    public boolean showingResult() {
        return this == RESULT_REVEAL || this == RESULT_DISPLAY;
    }

    /** True while the wheel/ball are actively moving through the spin. */
    public boolean spinning() {
        return this == STARTING || this == ACCELERATING || this == SPINNING
                || this == DECELERATING || this == BALL_SETTLING;
    }
}
