package com.dragonloot.client;

import com.dragonloot.core.DragonConfig;
import com.dragonloot.core.DragonStats;
import com.dragonloot.core.RollEngine;
import com.dragonloot.core.RollResult;
import com.dragonloot.data.DragonReward;
import com.dragonloot.data.RewardData;
import com.dragonloot.gui.EggRevealScreen;

import net.minecraft.client.Minecraft;

import java.util.Random;

/**
 * One place to kick off a gamble: roll the egg, bank the result into lifetime
 * stats, pick a flavor reward, and open the reveal screen on the client thread.
 * The command, keybind and chat trigger all route through here.
 */
public final class DragonReveal {

    private static final Random RNG = new Random();

    private DragonReveal() {}

    public static void rollAndShow() {
        RollResult result = RollEngine.INSTANCE.roll(RNG);
        DragonStats.INSTANCE.record(result.finalRarity());
        DragonConfig.INSTANCE.saveStats();
        DragonReward reward = RewardData.pick(result.finalRarity(), RNG);

        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.setScreen(new EggRevealScreen(result, reward)));
    }
}
