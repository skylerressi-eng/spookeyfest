package com.treegifts.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.treegifts.TreeGiftsMod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * Client hookup for Tree Gifts on Minecraft 26.1.2 (Fabric).
 *
 * The reveal is driven ENTIRELY by Hypixel's real Tree Gift chat message:
 * {@link TreeGiftChatListener} parses it and queues the actual drop, which
 * {@link RealTreeGiftReveal} then animates. Nothing here rolls or invents loot.
 *
 * The keybind (default G) just <b>replays the last real drop</b> — handy for
 * showing it off again — and never fabricates a result.
 */
public class TreeGiftsClient implements ClientModInitializer {

    private KeyMapping replayKey;

    @Override
    public void onInitializeClient() {
        // The one and only trigger: real Tree Gift chat messages.
        new TreeGiftChatListener().register();

        replayKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.treegifts.replay",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                KeyMapping.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (replayKey.consumeClick()) {
                if (!RealTreeGiftReveal.INSTANCE.replayLast()) {
                    TreeGiftsMod.LOGGER.info("[Tree Gifts] no real Tree Gift seen yet — chop a tree on Galatea first.");
                }
            }
        });

        TreeGiftsMod.LOGGER.info("[Tree Gifts] client ready — watching chat for real Tree Gifts. Replay key: G");
    }
}
