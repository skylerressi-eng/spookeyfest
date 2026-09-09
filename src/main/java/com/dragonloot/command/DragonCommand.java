package com.dragonloot.command;

import com.dragonloot.core.DragonLootConfig;
import com.dragonloot.core.DragonStats;
import com.dragonloot.core.Rarity;
import com.dragonloot.core.RollEngine;
import com.dragonloot.event.DragonReveal;
import com.dragonloot.gui.DragonGuideScreen;
import com.spookybirch.util.Fmt;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.Arrays;
import java.util.List;

/**
 * /dragonloot — client-side command for the dragon egg gamble.
 *   /dragonloot                 help
 *   /dragonloot test            play a reveal right now (great for a demo)
 *   /dragonloot guide           open the odds + luck + rewards GUI
 *   /dragonloot stats           print your lifetime pulls to chat
 *   /dragonloot odds <tier> <%> set a crack chance (tier: rare|epic|legendary)
 *   /dragonloot luck <x>        set the global luck multiplier
 *   /dragonloot sound           toggle reveal sounds
 *   /dragonloot auto            toggle the auto-trigger on dragon death
 *   /dragonloot reset           clear your lifetime stats
 */
public class DragonCommand extends CommandBase {

    private static final String[] SUBS = {
            "test", "roll", "guide", "stats", "odds", "luck", "sound", "auto", "reset"
    };

    @Override
    public String getCommandName() {
        return "dragonloot";
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("dl", "dragon");
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, SUBS);
        if (args.length == 2 && args[0].equalsIgnoreCase("odds")) {
            return getListOfStringsMatchingLastWord(args, "rare", "epic", "legendary");
        }
        return null;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/dragonloot <test|guide|stats|odds|luck|sound|auto|reset>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        final Minecraft mc = Minecraft.getMinecraft();
        String sub = args.length == 0 ? "help" : args[0].toLowerCase();

        if (sub.equals("test") || sub.equals("roll")) {
            DragonReveal.rollAndShow();
        } else if (sub.equals("guide")) {
            mc.addScheduledTask(new Runnable() {
                public void run() { mc.displayGuiScreen(new DragonGuideScreen()); }
            });
        } else if (sub.equals("stats")) {
            printStats(sender);
        } else if (sub.equals("odds")) {
            setOdds(sender, args);
        } else if (sub.equals("luck")) {
            setLuck(sender, args);
        } else if (sub.equals("sound")) {
            DragonLootConfig.INSTANCE.playSounds = !DragonLootConfig.INSTANCE.playSounds;
            DragonLootConfig.INSTANCE.save();
            msg(sender, EnumChatFormatting.GOLD + "Reveal sounds " + onOff(DragonLootConfig.INSTANCE.playSounds));
        } else if (sub.equals("auto")) {
            DragonLootConfig.INSTANCE.autoTrigger = !DragonLootConfig.INSTANCE.autoTrigger;
            DragonLootConfig.INSTANCE.save();
            msg(sender, EnumChatFormatting.GOLD + "Auto-trigger on dragon death " + onOff(DragonLootConfig.INSTANCE.autoTrigger));
        } else if (sub.equals("reset")) {
            DragonStats.INSTANCE.reset();
            DragonLootConfig.INSTANCE.saveStats();
            msg(sender, EnumChatFormatting.GOLD + "Lifetime dragon stats cleared.");
        } else {
            printHelp(sender);
        }
    }

    private void printHelp(ICommandSender s) {
        msg(s, EnumChatFormatting.DARK_PURPLE + "" + EnumChatFormatting.BOLD + "☬ DragonLoot");
        msg(s, EnumChatFormatting.LIGHT_PURPLE + "/dragonloot test " + EnumChatFormatting.GRAY + "- play a reveal now");
        msg(s, EnumChatFormatting.LIGHT_PURPLE + "/dragonloot guide " + EnumChatFormatting.GRAY + "- odds, luck & rewards GUI");
        msg(s, EnumChatFormatting.LIGHT_PURPLE + "/dragonloot stats " + EnumChatFormatting.GRAY + "- your lifetime pulls");
        msg(s, EnumChatFormatting.LIGHT_PURPLE + "/dragonloot odds <rare|epic|legendary> <%> " + EnumChatFormatting.GRAY + "- crack chance");
        msg(s, EnumChatFormatting.LIGHT_PURPLE + "/dragonloot luck <x> " + EnumChatFormatting.GRAY + "- global luck multiplier");
        msg(s, EnumChatFormatting.LIGHT_PURPLE + "/dragonloot sound " + EnumChatFormatting.GRAY + "- toggle reveal sounds");
        msg(s, EnumChatFormatting.LIGHT_PURPLE + "/dragonloot auto " + EnumChatFormatting.GRAY + "- toggle auto-trigger");
        msg(s, EnumChatFormatting.LIGHT_PURPLE + "/dragonloot reset " + EnumChatFormatting.GRAY + "- clear stats");
        msg(s, EnumChatFormatting.GRAY + "Keybind: " + EnumChatFormatting.WHITE + "H " + EnumChatFormatting.GRAY + "opens the guide. Aliases: /dl, /dragon");
    }

    private void printStats(ICommandSender s) {
        DragonStats st = DragonStats.INSTANCE;
        msg(s, EnumChatFormatting.DARK_PURPLE + "" + EnumChatFormatting.BOLD + "☬ Dragon Luck");
        msg(s, EnumChatFormatting.GRAY + "Eggs cracked: " + EnumChatFormatting.WHITE + Fmt.num(st.total()));
        if (st.total() == 0) {
            msg(s, EnumChatFormatting.GRAY + "None yet — slay a dragon or try " + EnumChatFormatting.WHITE + "/dragonloot test");
            return;
        }
        for (Rarity r : Rarity.values()) {
            msg(s, "  " + r.colored() + EnumChatFormatting.GRAY + ": " + EnumChatFormatting.WHITE
                    + Fmt.num(st.count(r)) + EnumChatFormatting.DARK_GRAY
                    + String.format(" (%.1f%%)", st.share(r) * 100.0));
        }
        Rarity best = st.best();
        msg(s, EnumChatFormatting.GRAY + "Best pull: " + (best == null ? "none" : best.colored()));
    }

    private void setOdds(ICommandSender s, String[] args) {
        if (args.length < 3) {
            msg(s, EnumChatFormatting.RED + "Usage: /dragonloot odds <rare|epic|legendary> <percent>");
            return;
        }
        Rarity from;
        String tier = args[1].toLowerCase();
        if (tier.equals("rare")) from = Rarity.UNCOMMON;          // chance to reach Rare
        else if (tier.equals("epic")) from = Rarity.RARE;          // chance to reach Epic
        else if (tier.equals("legendary")) from = Rarity.EPIC;     // chance to reach Legendary
        else {
            msg(s, EnumChatFormatting.RED + "Tier must be rare, epic or legendary.");
            return;
        }
        try {
            double pctVal = Double.parseDouble(args[2].replace("%", "").trim());
            double frac = pctVal / 100.0;
            RollEngine.INSTANCE.setAdvanceChance(from, frac);
            // Mirror the clamped value back into config and persist.
            double stored = RollEngine.INSTANCE.getAdvanceChance(from);
            if (from == Rarity.UNCOMMON) DragonLootConfig.INSTANCE.uncommonToRare = stored;
            else if (from == Rarity.RARE) DragonLootConfig.INSTANCE.rareToEpic = stored;
            else DragonLootConfig.INSTANCE.epicToLegendary = stored;
            DragonLootConfig.INSTANCE.save();
            msg(s, EnumChatFormatting.GOLD + "Chance to reach " + tier + " set to "
                    + String.format("%.1f%%", stored * 100.0) + ".");
        } catch (NumberFormatException e) {
            msg(s, EnumChatFormatting.RED + "That's not a number: " + args[2]);
        }
    }

    private void setLuck(ICommandSender s, String[] args) {
        if (args.length < 2) {
            msg(s, EnumChatFormatting.RED + "Usage: /dragonloot luck <multiplier>  (1.0 = default)");
            return;
        }
        try {
            double x = Double.parseDouble(args[1].trim());
            RollEngine.INSTANCE.setLuck(x);
            DragonLootConfig.INSTANCE.luck = RollEngine.INSTANCE.getLuck();
            DragonLootConfig.INSTANCE.save();
            msg(s, EnumChatFormatting.GOLD + "Luck multiplier set to ×" + Fmt.num(RollEngine.INSTANCE.getLuck()) + ".");
        } catch (NumberFormatException e) {
            msg(s, EnumChatFormatting.RED + "That's not a number: " + args[1]);
        }
    }

    private String onOff(boolean b) {
        return (b ? EnumChatFormatting.GREEN + "on" : EnumChatFormatting.RED + "off").toString();
    }

    private void msg(ICommandSender s, String text) {
        s.addChatMessage(new ChatComponentText(text));
    }
}
