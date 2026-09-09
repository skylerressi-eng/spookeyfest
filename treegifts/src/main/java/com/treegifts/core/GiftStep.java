package com.treegifts.core;

/**
 * One beat of a tree gift's reveal — one axe throw and what it did.
 *
 * The roll engine produces an ordered list of these up front, and the animation
 * simply plays them out. Modelling the roll as data (not as something the
 * renderer decides on the fly) is what keeps the outcome deterministic and
 * unit-testable.
 */
public final class GiftStep {

    public enum Type {
        /** First axe: the wrapped wood is revealed as Common. */
        REVEAL,
        /** An axe bumped the wood up to a higher rarity. */
        UPGRADE,
        /** An axe cracked the wood open — this is the payout beat. */
        CRACK,
        /** A Legendary wood span and transformed into a Mango Dye. */
        MANGO
    }

    public final Type type;
    /** The rarity the wood is showing after this beat. */
    public final Rarity rarity;

    private GiftStep(Type type, Rarity rarity) {
        this.type = type;
        this.rarity = rarity;
    }

    public static GiftStep reveal(Rarity r)  { return new GiftStep(Type.REVEAL, r); }
    public static GiftStep upgrade(Rarity r) { return new GiftStep(Type.UPGRADE, r); }
    public static GiftStep crack(Rarity r)   { return new GiftStep(Type.CRACK, r); }
    public static GiftStep mango()           { return new GiftStep(Type.MANGO, Rarity.LEGENDARY); }

    /** True if this beat ends the gift (a crack payout or a mango transform). */
    public boolean isTerminal() {
        return type == Type.CRACK || type == Type.MANGO;
    }

    @Override
    public String toString() {
        return type + "(" + rarity + ")";
    }
}
