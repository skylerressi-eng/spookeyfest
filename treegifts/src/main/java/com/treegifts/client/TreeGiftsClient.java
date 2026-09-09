package com.treegifts.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.treegifts.TreeGiftsMod;
import com.treegifts.core.GiftConfig;
import com.treegifts.core.GiftResult;
import com.treegifts.core.TreeGiftRoller;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.BlockTags;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

/**
 * The client hookup for Tree Gifts (Minecraft 26.1.2 / Fabric, Mojang mappings):
 *
 *   • Chop a log (anything in the {@code minecraft:logs} tag) → a gift reveal opens.
 *   • Press the keybind (default <b>G</b>, rebindable under Controls → Tree Gifts)
 *     → open a reveal on demand. Handy on a real server, where a client-only mod
 *     can't always see block breaks — and handy for just enjoying the animation.
 *
 * All the chance lives in {@link TreeGiftRoller}; here we just decide *when* to
 * pop a reveal and hand a freshly-rolled {@link GiftResult} to the screen.
 */
public class TreeGiftsClient implements ClientModInitializer {

    private final GiftConfig config = GiftConfig.defaults();
    private final TreeGiftRoller roller = new TreeGiftRoller(config, new Random());

    /** Don't stack a dozen reveals when a tree drops several logs at once. */
    private long lastOpenMs = 0L;
    private static final long OPEN_COOLDOWN_MS = 400L;

    private KeyMapping openKey;

    @Override
    public void onInitializeClient() {
        // 26.x: KeyMapping takes a KeyMapping.Category object (not a String).
        openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.treegifts.open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                KeyMapping.Category.MISC));

        // Keybind: open a reveal whenever the player asks.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.consumeClick()) {
                openGift(client);
            }
        });

        // Chop a log → open a reveal. Fires in singleplayer / on an integrated
        // server; on a remote server use the keybind instead.
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || player != client.player) return;
            if (!state.is(BlockTags.LOGS)) return;
            openGift(client);
        });

        TreeGiftsMod.LOGGER.info("[Tree Gifts] client ready — keybind: G");
    }

    /** Roll a gift and show the reveal, unless one is already up or we're on cooldown. */
    private void openGift(Minecraft client) {
        long now = System.currentTimeMillis();
        if (now - lastOpenMs < OPEN_COOLDOWN_MS) return;
        if (client.screen instanceof TreeGiftScreen) return;
        lastOpenMs = now;

        final GiftResult result = roller.roll();
        // Screens must be opened on the client thread.
        client.execute(() -> client.setScreen(new TreeGiftScreen(result)));
    }
}
