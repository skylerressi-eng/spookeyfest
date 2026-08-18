package com.spookybirch.command;

import com.spookybirch.core.CandyTracker;
import com.spookybirch.core.SpookyConfig;
import com.spookybirch.core.SpookyState;
import com.spookybirch.util.Fmt;
import com.spookybirch.data.CandyData;
import com.spookybirch.data.CandyMob;
import com.spookybirch.data.FishingData;
import com.spookybirch.data.SeaCreature;
import com.spookybirch.gui.GuideScreen;
import com.spookybirch.gui.MoveHudScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.Arrays;
import java.util.List;

/**
 * /spooky command. Client-side only (registered via ClientCommandHandler).
 *   /spooky            help
 *   /spooky guide      open the score + candy + fishing guide GUI
 *   /spooky stats      print candy score / rate / ETA to chat
 *   /spooky goal <n>   set a candy-score goal for the ETA (0 = off)
 *   /spooky candy      print the candy ranking to chat
 *   /spooky fishing    print the fishing table to chat
 *   /spooky move       open the drag-to-move HUD editor
 *   /spooky toggle     turn the HUD on/off
 *   /spooky reset      reset the session candy counters
 */
public class SpookyCommand extends CommandBase {

    private static final String[] SUBS = {
            "guide", "stats", "goal", "candy", "fishing", "move", "toggle", "reset"
    };

    @Override
    public String getCommandName() {
        return "spooky";
    }

    /** Tab-complete the sub-command names after "/spooky ". */
    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, SUBS);
        }
        return null;
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("spookybirch", "sb");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/spooky <guide|stats|goal|candy|fishing|move|toggle|reset>";
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

        if (sub.equals("guide")) {
            mc.addScheduledTask(new Runnable() {
                public void run() { mc.displayGuiScreen(new GuideScreen()); }
            });
        } else if (sub.equals("move")) {
            mc.addScheduledTask(new Runnable() {
                public void run() { mc.displayGuiScreen(new MoveHudScreen()); }
            });
        } else if (sub.equals("candy")) {
            printCandy(sender);
        } else if (sub.equals("fishing")) {
            printFishing(sender);
        } else if (sub.equals("stats") || sub.equals("score")) {
            printStats(sender);
        } else if (sub.equals("goal")) {
            setGoal(sender, args);
        } else if (sub.equals("toggle")) {
            SpookyConfig.INSTANCE.hudEnabled = !SpookyConfig.INSTANCE.hudEnabled;
            SpookyConfig.INSTANCE.save();
            msg(sender, EnumChatFormatting.GOLD + "HUD " + (SpookyConfig.INSTANCE.hudEnabled ? "enabled" : "disabled"));
        } else if (sub.equals("reset")) {
            SpookyState.INSTANCE.resetSession();
            msg(sender, EnumChatFormatting.GOLD + "Session candy counters reset.");
        } else {
            printHelp(sender);
        }
    }

    private void printHelp(ICommandSender s) {
        msg(s, EnumChatFormatting.GOLD + "" + EnumChatFormatting.BOLD + "☠ SpookyBirch");
        msg(s, EnumChatFormatting.YELLOW + "/spooky guide " + EnumChatFormatting.GRAY + "- candy + fishing tables");
        msg(s, EnumChatFormatting.YELLOW + "/spooky candy " + EnumChatFormatting.GRAY + "- best candy mobs in chat");
        msg(s, EnumChatFormatting.YELLOW + "/spooky fishing " + EnumChatFormatting.GRAY + "- spooky sea creatures");
        msg(s, EnumChatFormatting.YELLOW + "/spooky stats " + EnumChatFormatting.GRAY + "- candy score, rate & ETA");
        msg(s, EnumChatFormatting.YELLOW + "/spooky goal <n> " + EnumChatFormatting.GRAY + "- set a candy-score goal (0=off)");
        msg(s, EnumChatFormatting.YELLOW + "/spooky move " + EnumChatFormatting.GRAY + "- drag the HUD around");
        msg(s, EnumChatFormatting.YELLOW + "/spooky toggle " + EnumChatFormatting.GRAY + "- show/hide the HUD");
        msg(s, EnumChatFormatting.YELLOW + "/spooky reset " + EnumChatFormatting.GRAY + "- reset session candy");
    }

    private void printStats(ICommandSender s) {
        CandyTracker t = CandyTracker.INSTANCE;
        SpookyConfig cfg = SpookyConfig.INSTANCE;
        msg(s, EnumChatFormatting.GOLD + "" + EnumChatFormatting.BOLD + "☠ Candy score tracker");
        msg(s, EnumChatFormatting.GRAY + "Collected: " + EnumChatFormatting.GREEN + t.collectedGreen() + " green"
                + EnumChatFormatting.GRAY + ", " + EnumChatFormatting.LIGHT_PURPLE + t.collectedPurple() + " purple");
        msg(s, EnumChatFormatting.GRAY + "Score: " + EnumChatFormatting.GOLD + Fmt.num(t.score())
                + EnumChatFormatting.DARK_GRAY + " (purple ×" + Fmt.num(cfg.purpleWeight) + ")");
        msg(s, EnumChatFormatting.GRAY + "Rate now: " + EnumChatFormatting.AQUA + Fmt.rate(t.recentRatePerHour())
                + EnumChatFormatting.GRAY + "  •  session: " + EnumChatFormatting.AQUA + Fmt.rate(t.overallRatePerHour()));
        msg(s, EnumChatFormatting.GRAY + "Best rate: " + EnumChatFormatting.AQUA + Fmt.rate(t.bestRecentRate())
                + EnumChatFormatting.GRAY + "  •  played: " + EnumChatFormatting.WHITE + Fmt.duration(t.elapsedMs() / 1000L));
        if (cfg.candyGoal > 0) {
            long eta = t.etaSecondsToGoal();
            String etaStr = eta == 0L ? "reached!" : (eta < 0L ? "need more data" : "~" + Fmt.duration(eta));
            msg(s, EnumChatFormatting.GRAY + "Goal " + Fmt.num(cfg.candyGoal) + ": " + EnumChatFormatting.YELLOW + etaStr);
        }
    }

    private void setGoal(ICommandSender s, String[] args) {
        if (args.length < 2) {
            msg(s, EnumChatFormatting.RED + "Usage: /spooky goal <score>  (0 turns it off)");
            return;
        }
        try {
            int goal = Integer.parseInt(args[1].replace(",", ""));
            if (goal < 0) goal = 0;
            SpookyConfig.INSTANCE.candyGoal = goal;
            SpookyConfig.INSTANCE.save();
            msg(s, EnumChatFormatting.GOLD + (goal == 0 ? "Candy goal cleared." : "Candy goal set to " + Fmt.num(goal) + "."));
        } catch (NumberFormatException e) {
            msg(s, EnumChatFormatting.RED + "That's not a number: " + args[1]);
        }
    }

    private void printCandy(ICommandSender s) {
        msg(s, EnumChatFormatting.GOLD + "" + EnumChatFormatting.BOLD + "Best candy mobs (ranked):");
        int i = 1;
        for (CandyMob m : CandyData.ranked()) {
            msg(s, EnumChatFormatting.YELLOW + "" + (i++) + ". " + EnumChatFormatting.WHITE + m.name
                    + EnumChatFormatting.GRAY + " — " + EnumChatFormatting.GREEN + m.greenPerKill + " green"
                    + EnumChatFormatting.GRAY + ", " + EnumChatFormatting.LIGHT_PURPLE
                    + String.format("%.1f%%", m.purpleChance * 100) + " purple"
                    + EnumChatFormatting.DARK_GRAY + " (" + m.where + ")");
        }
    }

    private void printFishing(ICommandSender s) {
        msg(s, EnumChatFormatting.GOLD + "" + EnumChatFormatting.BOLD + "Spooky sea creatures:");
        for (SeaCreature c : FishingData.all()) {
            msg(s, EnumChatFormatting.AQUA + c.name + EnumChatFormatting.GRAY + " — "
                    + EnumChatFormatting.WHITE + String.format("%.0f%%", c.chance * 100)
                    + EnumChatFormatting.GRAY + " — " + c.drops);
        }
    }

    private void msg(ICommandSender s, String text) {
        s.addChatMessage(new ChatComponentText(text));
    }
}
