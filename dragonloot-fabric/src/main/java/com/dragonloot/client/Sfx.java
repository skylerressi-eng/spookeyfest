package com.dragonloot.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * All of the reveal's sound effects in one place. Uses vanilla sounds only (no
 * assets to ship). If a sound constant is named differently on your exact
 * Minecraft version, this is the single file to adjust.
 */
public final class Sfx {

    private Sfx() {}

    private static void play(Holder<SoundEvent> sound, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getSoundManager() == null) return;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch));
    }

    /** A slot-reel click. Pitch rises as the reel climbs toward the payout. */
    public static void tick(float pitch) {
        play(SoundEvents.UI_BUTTON_CLICK, clampPitch(pitch));
    }

    /** The egg cracking a stage further. */
    public static void crack(float pitch) {
        play(SoundEvents.EXPERIENCE_ORB_PICKUP, clampPitch(pitch));
    }

    /** The egg splitting open. */
    public static void breakOpen() {
        play(SoundEvents.AMETHYST_BLOCK_BREAK, 0.9f);
    }

    /** The payout chime, brighter for lower tiers, deeper + growl for jackpots. */
    public static void payout(boolean legendary) {
        play(SoundEvents.PLAYER_LEVELUP, legendary ? 0.9f : 1.3f);
        if (legendary) {
            play(SoundEvents.ENDER_DRAGON_GROWL, 1.0f);
            play(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f);
        }
    }

    private static float clampPitch(float p) {
        if (p < 0.5f) return 0.5f;
        if (p > 2.0f) return 2.0f;
        return p;
    }
}
