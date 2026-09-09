package com.treegifts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;

/**
 * Tree Gifts — a client-side SkyBlock-flavoured reveal.
 *
 * The common entrypoint does almost nothing: everything interesting is client
 * rendering + input, wired up in {@link com.treegifts.client.TreeGiftsClient}.
 * The roll engine itself lives in {@link com.treegifts.core} and has no
 * Minecraft dependency at all.
 */
public class TreeGiftsMod implements ModInitializer {

    public static final String MOD_ID = "treegifts";
    public static final Logger LOGGER = LoggerFactory.getLogger("Tree Gifts");

    @Override
    public void onInitialize() {
        LOGGER.info("[Tree Gifts] loaded — go chop a tree. 🌳🎁");
    }
}
