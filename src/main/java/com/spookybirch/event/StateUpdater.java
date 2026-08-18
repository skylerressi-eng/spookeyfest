package com.spookybirch.event;

import com.spookybirch.core.SpookyState;
import com.spookybirch.util.ScoreboardReader;
import com.spookybirch.util.TextUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The engine of the mod: once per client tick it refreshes {@link SpookyState}
 * from mana (action bar), candy (inventory), event status (scoreboard) and
 * nearby spooky mobs (loaded entities). Everything else just reads the state.
 */
public class StateUpdater {

    // "1234/1234✎" or "1,234/1,234✎ Mana" in the action bar.
    private static final Pattern MANA = Pattern.compile("([0-9,]+)/([0-9,]+)\\u270E");

    // Names that count as "spooky" mobs for the nearby counter.
    private static final Set<String> SPOOKY_NAMES = new HashSet<String>(Arrays.asList(
            "wither gourd", "scary jerry", "batty witch", "phantom spirit",
            "trick or treater", "crazy witch", "scaredy cat", "nightmare",
            "werewolf", "trick or treat"
    ));

    private int tick = 0;

    /** Read mana straight off the action bar as it arrives. */
    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event.type != 2) return; // 2 == action bar
        String raw = event.message == null ? "" : event.message.getUnformattedText();
        Matcher m = MANA.matcher(raw);
        if (m.find()) {
            SpookyState s = SpookyState.INSTANCE;
            s.mana = TextUtil.parseIntSafe(m.group(1), s.mana);
            s.maxMana = TextUtil.parseIntSafe(m.group(2), s.maxMana);
            s.manaKnown = true;
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;

        // Cheap work every tick, heavy work ~4x/second.
        if (tick++ % 5 != 0) return;

        readScoreboard();
        readCandy(mc.thePlayer);
        countNearbyMobs(mc);
    }

    private void readScoreboard() {
        SpookyState s = SpookyState.INSTANCE;
        String title = ScoreboardReader.title().toUpperCase();
        s.inSkyblock = title.contains("SKYBLOCK");

        List<String> lines = ScoreboardReader.lines();
        boolean active = false;
        String timeLeft = "";
        for (String line : lines) {
            String low = line.toLowerCase();
            if (low.contains("spooky festival")) {
                active = true;
                // Try to grab a trailing timer like "12:34".
                Matcher t = Pattern.compile("(\\d{1,2}:\\d{2})").matcher(line);
                if (t.find()) timeLeft = t.group(1);
            }
        }
        s.festivalActive = active;
        s.festivalTimeLeft = timeLeft;
    }

    private void readCandy(EntityPlayer player) {
        SpookyState s = SpookyState.INSTANCE;
        int green = 0, purple = 0;
        ItemStack[] inv = player.inventory.mainInventory;
        for (ItemStack stack : inv) {
            if (stack == null) continue;
            String name = TextUtil.stripColor(stack.getDisplayName()).toLowerCase();
            if (name.contains("green candy")) green += stack.stackSize;
            else if (name.contains("purple candy")) purple += stack.stackSize;
        }
        s.greenCandy = green;
        s.purpleCandy = purple;
        if (s.greenBaseline < 0) s.greenBaseline = green;
        if (s.purpleBaseline < 0) s.purpleBaseline = purple;

        // Feed the score tracker (counts only positive gains -> rate/score/ETA).
        com.spookybirch.core.CandyTracker.INSTANCE.update(green, purple);
    }

    private void countNearbyMobs(Minecraft mc) {
        SpookyState s = SpookyState.INSTANCE;
        int count = 0;
        double range = 30.0;
        double rangeSq = range * range;
        for (Object o : mc.theWorld.loadedEntityList) {
            if (!(o instanceof Entity)) continue;
            Entity e = (Entity) o;
            if (e == mc.thePlayer) continue;
            if (e.getDistanceSqToEntity(mc.thePlayer) > rangeSq) continue;
            String name = e.hasCustomName()
                    ? TextUtil.stripColor(e.getCustomNameTag()).toLowerCase()
                    : TextUtil.stripColor(e.getName()).toLowerCase();
            for (String spooky : SPOOKY_NAMES) {
                if (name.contains(spooky)) { count++; break; }
            }
        }
        s.nearbySpookyMobs = count;
    }
}
