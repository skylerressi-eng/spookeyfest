package com.spookybirch.command;

import com.spookybirch.core.SpookyConfig;
import com.spookybirch.core.SpookyState;
import com.spookybirch.data.CandyData;
import com.spookybirch.data.CandyMob;
import com.spookybirch.data.FishingData;
import com.spookybirch.data.SeaCreature;
import com.spookybirch.gui.GuideScreen;
import com.spookybirch.gui.MoveHudScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.Arrays;
import java.util.List;

/**
 * /spooky command. Client-side only (registered via ClientCommandHandler).
 *   /spooky            help
 *   /spooky guide      open the candy + fishing guide GUI
 *   /spooky candy      print the candy ranking to chat
 *   /spooky fishing    print the fishing table to chat
 *   /spooky move       open the drag-to-move HUD editor
 *   /spooky toggle     turn the HUD on/off
 *   /spooky reset      reset the session candy counters
 */
public class SpookyCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "spooky";
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("spookybirch", "sb");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/spooky <guide|candy|fishing|move|toggle|reset>";
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
        msg(s, EnumChatFormatting.YELLOW + "/spooky move " + EnumChatFormatting.GRAY + "- drag the HUD around");
        msg(s, EnumChatFormatting.YELLOW + "/spooky toggle " + EnumChatFormatting.GRAY + "- show/hide the HUD");
        msg(s, EnumChatFormatting.YELLOW + "/spooky reset " + EnumChatFormatting.GRAY + "- reset session candy");
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
