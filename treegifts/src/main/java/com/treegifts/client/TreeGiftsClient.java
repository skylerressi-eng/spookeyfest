package com.treegifts.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.treegifts.TreeGiftsMod;
import com.treegifts.core.ItemIcons;
import com.treegifts.core.Rarity;
import com.treegifts.core.TreeGiftConfig;
import com.treegifts.core.TreeGiftResult;
import com.treegifts.core.TreeGiftStats;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Client hookup for Tree Gifts on Minecraft 26.1.2 (Fabric).
 *
 * The reveal is driven entirely by Hypixel's real Tree Gift chat message
 * ({@link TreeGiftChatListener}); nothing here rolls loot. This class loads the
 * config, registers the replay keybind (G), and adds the /treegifts command to
 * tune settings, view lifetime stats, and preview any rarity.
 */
public class TreeGiftsClient implements ClientModInitializer {

    public static final String MOD_ID = "treegifts";

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

    private KeyMapping replayKey;

    @Override
    public void onInitializeClient() {
        TreeGiftConfig.INSTANCE.load(FabricLoader.getInstance().getConfigDir());

        new TreeGiftChatListener().register();

        replayKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.treegifts.replay", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (replayKey != null && replayKey.consumeClick()) {
                if (!RealTreeGiftReveal.INSTANCE.replayLast()) demo(Rarity.SPECIAL);
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> registerCommands(dispatcher));

        TreeGiftsMod.LOGGER.info("[Tree Gifts] ready — watching chat for real Tree Gifts. /treegifts, replay key G.");
    }

    // ---- commands ----

    private void registerCommands(CommandDispatcher<FabricClientCommandSource> d) {
        var root = d.register(ClientCommands.literal("treegifts")
                .executes(c -> { if (!RealTreeGiftReveal.INSTANCE.replayLast()) demo(Rarity.SPECIAL); return 1; })
                .then(ClientCommands.literal("replay").executes(c -> {
                    if (!RealTreeGiftReveal.INSTANCE.replayLast()) {
                        feedback(c.getSource(), "§7No real Tree Gift seen yet — showing a demo.");
                        demo(Rarity.SPECIAL);
                    }
                    return 1;
                }))
                .then(ClientCommands.literal("demo")
                        .executes(c -> { demo(Rarity.SPECIAL); return 1; })
                        .then(ClientCommands.argument("rarity", StringArgumentType.word()).executes(c -> {
                            Rarity r = Rarity.fromName(StringArgumentType.getString(c, "rarity"));
                            if (r == null) { feedback(c.getSource(), "§cUnknown rarity. Try common…special."); return 0; }
                            demo(r);
                            return 1;
                        })))
                .then(ClientCommands.literal("stats").executes(c -> { stats(c.getSource()); return 1; }))
                .then(ClientCommands.literal("reset").executes(c -> {
                    TreeGiftStats.INSTANCE.reset();
                    TreeGiftConfig.INSTANCE.saveStats();
                    feedback(c.getSource(), "§6Tree Gift stats cleared.");
                    return 1;
                }))
                .then(ClientCommands.literal("sound").executes(c -> {
                    TreeGiftConfig cfg = TreeGiftConfig.INSTANCE;
                    cfg.playSounds = !cfg.playSounds; cfg.save();
                    feedback(c.getSource(), "§6Reveal sounds " + (cfg.playSounds ? "§aON" : "§cOFF"));
                    return 1;
                }))
                .then(ClientCommands.literal("auto").executes(c -> {
                    TreeGiftConfig cfg = TreeGiftConfig.INSTANCE;
                    cfg.autoReveal = !cfg.autoReveal; cfg.save();
                    feedback(c.getSource(), "§6Auto-reveal on Tree Gift " + (cfg.autoReveal ? "§aON" : "§cOFF"));
                    return 1;
                }))
                .then(ClientCommands.literal("threshold")
                        .then(ClientCommands.argument("rarity", StringArgumentType.word()).executes(c -> {
                            Rarity r = Rarity.fromName(StringArgumentType.getString(c, "rarity"));
                            if (r == null) { feedback(c.getSource(), "§cUnknown rarity. Try common…special."); return 0; }
                            TreeGiftConfig cfg = TreeGiftConfig.INSTANCE;
                            cfg.minRarity = r.ordinal(); cfg.save();
                            feedback(c.getSource(), "§6Now auto-revealing " + r.colored() + "§6+ (and above).");
                            return 1;
                        })))
                .then(ClientCommands.literal("help").executes(c -> { help(c.getSource()); return 1; })));

        d.register(ClientCommands.literal("tg").redirect(root));
    }

    /** Build and show a clearly-labelled demo reveal of a given rarity. */
    private void demo(Rarity r) {
        String name = demoName(r) + " §7(demo)";
        double pct = demoDrop(r);
        RealTreeGiftReveal.INSTANCE.enqueue(new TreeGiftResult(
                stripColor(demoName(r)) + " (demo)", r, -1, pct, "Fig", r == Rarity.MYTHIC,
                ItemIcons.iconFor(demoName(r))));
    }

    private static String demoName(Rarity r) {
        switch (r) {
            case COMMON:    return "Forest Essence";
            case UNCOMMON:  return "Stretching Sticks";
            case RARE:      return "Enchanted Wood";
            case EPIC:      return "Sweep Booster";
            case LEGENDARY: return "Dreadwing";
            case MYTHIC:    return "Phanpyre";
            case SPECIAL:   return "Tree the Fish";
            default:        return "Tree Gift";
        }
    }
    private static double demoDrop(Rarity r) {
        switch (r) {
            case UNCOMMON: return 20.0;
            case RARE:     return 10.0;
            case EPIC:     return 1.0;
            case LEGENDARY:return 1.2;
            case MYTHIC:   return 0.3;
            case SPECIAL:  return 0.05;
            default:       return -1.0;
        }
    }
    private static String stripColor(String s) { return s.replaceAll("§.", ""); }

    private void stats(FabricClientCommandSource src) {
        TreeGiftStats s = TreeGiftStats.INSTANCE;
        feedback(src, "§2§l🌳 Tree Gift stats");
        feedback(src, "§7Gifts revealed: §f" + s.total());
        if (s.total() == 0) { feedback(src, "§7None yet — chop a tree on Galatea (or §f/treegifts demo§7)."); return; }
        for (Rarity r : Rarity.values()) {
            long n = s.count(r);
            if (n == 0) continue;
            feedback(src, "  " + r.colored() + "§7: §f" + n + String.format(" §8(%.1f%%)", s.share(r) * 100.0));
        }
        Rarity best = s.best();
        feedback(src, "§7Best rarity: " + (best == null ? "§8none" : best.colored()));
        if (s.rarestDrop() > 0) {
            feedback(src, String.format("§7Rarest find: §f%s §8(%s%% drop)", s.rarestName(), trim(s.rarestDrop())));
        }
    }

    private void help(FabricClientCommandSource src) {
        feedback(src, "§2§l🌳 Tree Gifts §7commands:");
        feedback(src, "§a/treegifts §7or §a/tg §8- replay your last drop");
        feedback(src, "§a/tg demo [rarity] §8- preview a reveal (common…special)");
        feedback(src, "§a/tg stats §8- lifetime stats   §a/tg reset §8- clear them");
        feedback(src, "§a/tg auto §8- toggle auto-reveal   §a/tg sound §8- toggle sound");
        feedback(src, "§a/tg threshold <rarity> §8- only auto-reveal that rarity and up");
    }

    private static String trim(double d) {
        return d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
    }

    private void feedback(FabricClientCommandSource src, String msg) {
        src.sendFeedback(Component.literal(msg));
    }
}
