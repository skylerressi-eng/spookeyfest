package com.dragonloot.event;

import com.dragonloot.core.DragonLootConfig;
import com.spookybirch.util.TextUtil;

import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Watches chat for the SkyBlock dragon fight and fires the egg reveal when the
 * dragon dies. It works as a tiny state machine: seeing a "Summoning Eye placed"
 * or "Dragon has spawned" line arms the trigger, and a dragon death line while
 * armed plays the reveal.
 *
 * The exact Hypixel phrasing changes over time, so the trigger phrases live here
 * as clearly-labelled constants — tune them in one place if Hypixel rewords a
 * message (same philosophy as SpookyBirch's tunable data tables). A short
 * cooldown stops a burst of loot/death lines from firing twice.
 */
public class DragonWatcher {

    // Lines that mean "a dragon fight is starting" -> arm the trigger.
    private static final String[] SUMMON_PHRASES = {
            "summoning eye",         // "☬ [player] placed a Summoning Eye! ..."
            "the dragon has spawned",
            "dragon has spawned",
            "❈ superior dragon",     // superior spawn banner (defensive)
    };

    // Lines that mean "the dragon just died" -> fire the reveal.
    private static final String[] DEATH_PHRASES = {
            "dragon has been defeated",
            "dragon has been slain",
            "dragon is dead",
            "dragon has died",
            "☬ the dragon",          // Hypixel's dragon reward banner prefix
    };

    private boolean armed = false;
    private long lastFireMs = 0L;

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event.message == null) return;
        String raw = TextUtil.stripColor(event.message.getUnformattedText()).toLowerCase();
        if (raw.isEmpty()) return;

        DragonLootConfig cfg = DragonLootConfig.INSTANCE;
        if (!cfg.autoTrigger) return;

        if (containsAny(raw, SUMMON_PHRASES)) {
            armed = true;
        }

        if (isDeath(raw)) {
            // Require the summon to have been seen first, unless the player
            // turned that guard off.
            if (cfg.requireEyes && !armed) return;
            long now = System.currentTimeMillis();
            if (now - lastFireMs < 8000L) return; // debounce a burst of lines
            lastFireMs = now;
            armed = false;
            DragonReveal.rollAndShow();
        }
    }

    /** True only for death lines that actually mention a dragon. */
    private boolean isDeath(String raw) {
        if (!raw.contains("dragon")) return false;
        return containsAny(raw, DEATH_PHRASES)
                || raw.contains("defeated") || raw.contains("slain") || raw.contains("has died");
    }

    private static boolean containsAny(String haystack, String[] needles) {
        for (String n : needles) if (haystack.contains(n)) return true;
        return false;
    }
}
