package com.dragonloot.event;

import com.dragonloot.core.DragonLootConfig;
import com.dragonloot.core.DragonStats;
import com.dragonloot.core.RollEngine;
import com.dragonloot.core.RollResult;
import com.dragonloot.data.DragonReward;
import com.dragonloot.data.RewardData;
import com.dragonloot.gui.EggRevealScreen;

import net.minecraft.client.Minecraft;

import java.util.Random;

/**
 * One place to kick off a gamble: roll the egg, bank the result into the
 * lifetime stats, pick a flavor reward, and open the reveal screen on the
 * client thread. Both the chat watcher and the {@code /dragonloot} command
 * route through here so behaviour stays identical.
 */
public final class DragonReveal {

    private static final Random RNG = new Random();

    private DragonReveal() {}

    public static void rollAndShow() {
        RollResult result = RollEngine.INSTANCE.roll(RNG);
        DragonStats.INSTANCE.record(result.finalRarity());
        DragonLootConfig.INSTANCE.saveStats();
        final DragonReward reward = RewardData.pick(result.finalRarity(), RNG);

        final Minecraft mc = Minecraft.getMinecraft();
        final RollResult res = result;
        // GUI must open on the client thread (chat events can arrive off-thread).
        mc.addScheduledTask(new Runnable() {
            public void run() { mc.displayGuiScreen(new EggRevealScreen(res, reward)); }
        });
    }
}
