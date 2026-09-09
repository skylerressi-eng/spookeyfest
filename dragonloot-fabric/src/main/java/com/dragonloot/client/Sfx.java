package com.dragonloot.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

/**
 * All of the reveal's sound effects in one place. Uses vanilla sounds only (no
 * assets to ship). If a sound constant is named differently on your exact
 * Minecraft version, this is the single file to adjust.
 */
public final class Sfx {

    private Sfx() {}

    private static void play(RegistryEntry<SoundEvent> sound, float pitch) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getSoundManager() == null) return;
        // master(sound, pitch) is the most version-stable overload.
        mc.getSoundManager().play(PositionedSoundInstance.master(sound, pitch));
    }

    /** A slot-reel click. Pitch rises as the reel climbs toward the payout. */
    public static void tick(float pitch) {
        play(SoundEvents.UI_BUTTON_CLICK, clampPitch(pitch));
    }

    /** The egg cracking a stage further. */
    public static void crack(float pitch) {
        play(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, clampPitch(pitch));
    }

    /** The egg splitting open. */
    public static void breakOpen() {
        play(SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, 0.9f);
    }

    /** The payout chime, brighter for lower tiers, deeper + growl for jackpots. */
    public static void payout(boolean legendary) {
        play(SoundEvents.ENTITY_PLAYER_LEVELUP, legendary ? 0.9f : 1.3f);
        if (legendary) {
            play(SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 1.0f);
            play(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.4f);
        }
    }

    private static float clampPitch(float p) {
        if (p < 0.5f) return 0.5f;
        if (p > 2.0f) return 2.0f;
        return p;
    }
}
