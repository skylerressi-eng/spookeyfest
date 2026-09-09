package com.dragonloot;

import com.dragonloot.client.DragonReveal;
import com.dragonloot.core.DragonConfig;
import com.dragonloot.core.DragonStats;
import com.dragonloot.core.Rarity;
import com.dragonloot.core.RollEngine;
import com.dragonloot.gui.DragonGuideScreen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import org.lwjgl.glfw.GLFW;

/**
 * DragonLoot — a gambling-style egg reveal for the Minecraft/Hypixel dragon,
 * built for Fabric (Minecraft 26.1.2, Mojang mappings). Summon and slay a dragon
 * and a dragon egg gambles its way up the rarity ladder (Uncommon → Rare → Epic
 * → Legendary) with a slot-machine reveal. Client-side and cosmetic only — it
 * reads chat to know when a dragon died and never automates or touches your
 * inventory.
 *
 * Test it any time with <b>/dragongamble</b> (or the Test button in the guide).
 */
public class DragonLootClient implements ClientModInitializer {

    public static final String MODID = "dragonloot";

    private KeyMapping guideKey;

    // Chat trigger state machine (armed by a summon, fires on a dragon death).
    private boolean armed = false;
    private long lastFireMs = 0L;

    private static final String[] SUMMON_PHRASES = {
            "summoning eye", "the dragon has spawned", "dragon has spawned", "superior dragon"
    };
    private static final String[] DEATH_PHRASES = {
            "dragon has been defeated", "dragon has been slain", "dragon is dead", "dragon has died"
    };

    @Override
    public void onInitializeClient() {
        DragonConfig.INSTANCE.load(FabricLoader.getInstance().getConfigDir());

        guideKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.dragonloot.guide", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "category.dragonloot"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (guideKey.consumeClick()) {
                client.setScreen(new DragonGuideScreen());
            }
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay || message == null) return;
            onChat(message.getString().toLowerCase());
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> registerCommands(dispatcher));
    }

    // ---- chat trigger ----

    private void onChat(String raw) {
        if (raw.isEmpty()) return;
        DragonConfig cfg = DragonConfig.INSTANCE;
        if (!cfg.autoTrigger) return;

        if (containsAny(raw, SUMMON_PHRASES)) armed = true;

        if (isDeath(raw)) {
            if (cfg.requireEyes && !armed) return;
            long now = System.currentTimeMillis();
            if (now - lastFireMs < 8000L) return;
            lastFireMs = now;
            armed = false;
            DragonReveal.rollAndShow();
        }
    }

    private boolean isDeath(String raw) {
        if (!raw.contains("dragon")) return false;
        return containsAny(raw, DEATH_PHRASES)
                || raw.contains("defeated") || raw.contains("slain") || raw.contains("has died");
    }

    private static boolean containsAny(String haystack, String[] needles) {
        for (String n : needles) if (haystack.contains(n)) return true;
        return false;
    }

    // ---- commands ----

    private void registerCommands(com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource> d) {
        // The quick tester the user asked for.
        d.register(ClientCommandManager.literal("dragongamble")
                .executes(c -> { DragonReveal.rollAndShow(); return 1; }));

        var root = d.register(ClientCommandManager.literal("dragonloot")
                .executes(c -> { help(c.getSource()); return 1; })
                .then(ClientCommandManager.literal("test").executes(c -> { DragonReveal.rollAndShow(); return 1; }))
                .then(ClientCommandManager.literal("guide").executes(c -> { openGuide(); return 1; }))
                .then(ClientCommandManager.literal("stats").executes(c -> { stats(c.getSource()); return 1; }))
                .then(ClientCommandManager.literal("reset").executes(c -> {
                    DragonStats.INSTANCE.reset();
                    DragonConfig.INSTANCE.saveStats();
                    ok(c.getSource(), "Lifetime dragon stats cleared.");
                    return 1;
                }))
                .then(ClientCommandManager.literal("sound").executes(c -> {
                    DragonConfig cfg = DragonConfig.INSTANCE;
                    cfg.playSounds = !cfg.playSounds; cfg.save();
                    ok(c.getSource(), "Reveal sounds " + (cfg.playSounds ? "ON" : "OFF"));
                    return 1;
                }))
                .then(ClientCommandManager.literal("auto").executes(c -> {
                    DragonConfig cfg = DragonConfig.INSTANCE;
                    cfg.autoTrigger = !cfg.autoTrigger; cfg.save();
                    ok(c.getSource(), "Auto-trigger on dragon death " + (cfg.autoTrigger ? "ON" : "OFF"));
                    return 1;
                }))
                .then(ClientCommandManager.literal("luck")
                        .then(ClientCommandManager.argument("multiplier", DoubleArgumentType.doubleArg(0.0, 10.0))
                                .executes(c -> {
                                    double x = DoubleArgumentType.getDouble(c, "multiplier");
                                    RollEngine.INSTANCE.setLuck(x);
                                    DragonConfig.INSTANCE.luck = RollEngine.INSTANCE.getLuck();
                                    DragonConfig.INSTANCE.save();
                                    ok(c.getSource(), "Luck set to x" + RollEngine.INSTANCE.getLuck());
                                    return 1;
                                })))
                .then(ClientCommandManager.literal("odds")
                        .then(ClientCommandManager.argument("tier", StringArgumentType.word())
                                .then(ClientCommandManager.argument("percent", DoubleArgumentType.doubleArg(0.0, 98.0))
                                        .executes(c -> setOdds(c.getSource(),
                                                StringArgumentType.getString(c, "tier"),
                                                DoubleArgumentType.getDouble(c, "percent")))))));

        d.register(ClientCommandManager.literal("dl").redirect(root));
    }

    private int setOdds(FabricClientCommandSource src, String tier, double percent) {
        Rarity from;
        String t = tier.toLowerCase();
        if (t.equals("rare")) from = Rarity.UNCOMMON;
        else if (t.equals("epic")) from = Rarity.RARE;
        else if (t.equals("legendary")) from = Rarity.EPIC;
        else { err(src, "Tier must be rare, epic or legendary."); return 0; }

        RollEngine.INSTANCE.setAdvanceChance(from, percent / 100.0);
        double stored = RollEngine.INSTANCE.getAdvanceChance(from);
        DragonConfig cfg = DragonConfig.INSTANCE;
        if (from == Rarity.UNCOMMON) cfg.uncommonToRare = stored;
        else if (from == Rarity.RARE) cfg.rareToEpic = stored;
        else cfg.epicToLegendary = stored;
        cfg.save();
        ok(src, String.format("Chance to reach %s set to %.1f%%.", t, stored * 100.0));
        return 1;
    }

    private void openGuide() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.setScreen(new DragonGuideScreen()));
    }

    private void stats(FabricClientCommandSource src) {
        DragonStats s = DragonStats.INSTANCE;
        src.sendFeedback(Component.literal("☬ Dragon Luck").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        src.sendFeedback(Component.literal("Eggs cracked: " + s.total()).withStyle(ChatFormatting.GRAY));
        if (s.total() == 0) {
            src.sendFeedback(Component.literal("None yet — try /dragongamble").withStyle(ChatFormatting.GRAY));
            return;
        }
        for (Rarity r : Rarity.values()) {
            src.sendFeedback(Component.literal("  " + r.displayName + ": " + s.count(r)
                    + String.format(" (%.1f%%)", s.share(r) * 100.0)).withStyle(fmt(r)));
        }
        Rarity best = s.best();
        src.sendFeedback(Component.literal("Best pull: " + (best == null ? "none" : best.displayName))
                .withStyle(best == null ? ChatFormatting.GRAY : fmt(best)));
    }

    private void help(FabricClientCommandSource src) {
        src.sendFeedback(Component.literal("☬ DragonLoot").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        help(src, "/dragongamble", "play a reveal right now");
        help(src, "/dragonloot guide", "odds, luck & rewards GUI (key: H)");
        help(src, "/dragonloot stats", "your lifetime pulls");
        help(src, "/dragonloot odds <rare|epic|legendary> <%>", "set a crack chance");
        help(src, "/dragonloot luck <x>", "global luck multiplier");
        help(src, "/dragonloot sound", "toggle reveal sounds");
        help(src, "/dragonloot auto", "toggle auto-trigger on dragon death");
        help(src, "/dragonloot reset", "clear stats");
    }

    private void help(FabricClientCommandSource src, String cmd, String desc) {
        src.sendFeedback(Component.literal(cmd + " ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal("- " + desc).withStyle(ChatFormatting.GRAY)));
    }

    private void ok(FabricClientCommandSource src, String msg) {
        src.sendFeedback(Component.literal(msg).withStyle(ChatFormatting.GOLD));
    }

    private void err(FabricClientCommandSource src, String msg) {
        src.sendError(Component.literal(msg));
    }

    private static ChatFormatting fmt(Rarity r) {
        switch (r) {
            case UNCOMMON: return ChatFormatting.GREEN;
            case RARE: return ChatFormatting.BLUE;
            case EPIC: return ChatFormatting.DARK_PURPLE;
            default: return ChatFormatting.GOLD;
        }
    }
}
