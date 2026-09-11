package com.spookybirch.reforge;

import com.spookybirch.core.SpookyConfig;
import com.spookybirch.util.TextUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Random;

/**
 * The brain of the Blacksmith reforge gamble.
 *
 * <p><b>It never invents, applies, or alters a reforge.</b> It watches the
 * reforge GUI's item slot; when the server rerolls the item (its
 * {@code ExtraAttributes.modifier} changes on the <i>same</i> item), it reads
 * that authoritative result and plays a cinematic that <i>ends</i> on it. The
 * two earlier reforges shown are believable fakes; the final one is always the
 * real server result. If the result can't be confirmed, nothing is shown.
 *
 * <p>No packets are sent, no clicks are automated, no inventory or coins are
 * touched — this is a presentation layer over the normal Apply Reforge action.
 */
public class ReforgeGambleHandler {

    /** Set on construction so the /spooky reforge test command can trigger a preview. */
    public static ReforgeGambleHandler INSTANCE;

    private ReforgeAnimation anim;
    private boolean demoMode;      // a preview started from the command, not a real reforge
    private final Random demoRng = new Random();

    public ReforgeGambleHandler() {
        INSTANCE = this;
    }

    // Baseline for the item currently sitting in the reforge slot.
    private String slotIdentity;   // stable identity of that item
    private String slotModifier;   // its reforge id (null = unreforged)
    private boolean baselineSet;   // have we recorded a baseline to diff against

    // Which one-shot sounds we've already played for the running animation.
    private int soundHits;
    private boolean soundBreak, soundReveal;

    // ---------------------------------------------------------------- polling

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!SpookyConfig.INSTANCE.reforgeEnabled) { abort(); return; }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) { abort(); return; }

        GuiScreen gui = mc.currentScreen;
        if (!isReforgeMenu(mc, gui)) {
            // Left the menu: cancel a real cinematic and forget the baseline. A
            // command-triggered preview (demoMode) is allowed to keep running.
            if (!demoMode) abort();
            clearBaseline();
            return;
        }

        ItemStack input = findReforgeInput(mc, (GuiChest) gui);
        if (input == null) {
            // Empty slot / ambiguous contents — hold, don't diff against stale data.
            clearBaseline();
            return;
        }

        String identity = ReforgeData.identity(input);
        String modifier = ReforgeData.modifierId(input);

        if (!eq(identity, slotIdentity)) {
            // A different item arrived in the slot: (re)establish the baseline
            // without triggering — merely viewing an already-reforged item must
            // not fire the cinematic.
            slotIdentity = identity;
            slotModifier = modifier;
            baselineSet = true;
            return;
        }

        // Same item as last tick. A modifier that is now present and differs from
        // the baseline means the server just applied a real reforge.
        if (baselineSet && modifier != null && !eqIgnoreCase(modifier, slotModifier)) {
            start(input, slotModifier, modifier);
        }
        slotModifier = modifier;
    }

    // ---------------------------------------------------------------- rendering

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (anim == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!demoMode && !isReforgeMenu(mc, event.gui)) { abort(); return; }
        advanceAndRender(mc);
    }

    /** Renders a command-triggered preview while the player is in the world. */
    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (anim == null || !demoMode) return;
        advanceAndRender(Minecraft.getMinecraft());
    }

    private void advanceAndRender(Minecraft mc) {
        anim.update(System.currentTimeMillis());
        playOneShots(mc);
        ReforgeGambleRenderer.render(mc, anim);
        if (anim.isDone()) { anim = null; demoMode = false; }
    }

    /**
     * Start a self-contained preview (not tied to a real item) so the player can
     * see the cinematic without visiting the Blacksmith. Clearly a demo — it
     * makes up both reforges — which is why it's gated behind an explicit command.
     */
    public void playDemo() {
        SpookyConfig cfg = SpookyConfig.INSTANCE;
        ReforgeData.Category[] cats = ReforgeData.Category.values();
        ReforgeData.Category cat = cats[demoRng.nextInt(cats.length)];
        java.util.List<String> pool = ReforgeData.pool(cat);
        String from = pool.get(demoRng.nextInt(pool.size()));
        String to = pool.get(demoRng.nextInt(pool.size()));
        anim = new ReforgeAnimation(from, to, pool, cfg.reforgeSpeed, cfg.reforgeReducedMotion);
        demoMode = true;
        soundHits = 0;
        soundBreak = soundReveal = false;
    }

    // ---------------------------------------------------------------- internals

    private void start(ItemStack input, String fromModifier, String toModifier) {
        SpookyConfig cfg = SpookyConfig.INSTANCE;
        ReforgeData.Category cat = ReforgeData.categoryOf(input);
        String original = ReforgeData.displayName(fromModifier); // "" if none
        String result = ReforgeData.displayName(toModifier);     // the REAL one
        anim = new ReforgeAnimation(original, result, ReforgeData.pool(cat), cfg.reforgeSpeed, cfg.reforgeReducedMotion);
        demoMode = false;
        soundHits = 0;
        soundBreak = soundReveal = false;
    }

    private void abort() {
        anim = null;
    }

    private void clearBaseline() {
        slotIdentity = null;
        slotModifier = null;
        baselineSet = false;
    }

    private void playOneShots(Minecraft mc) {
        if (!SpookyConfig.INSTANCE.reforgeSounds || mc.thePlayer == null) return;
        int hit = anim.hitLandedThisFrame();
        if (hit == 1 && soundHits < 1) { soundHits = 1; sound(mc, "random.anvil_land", 0.5f, 1.35f); }
        else if (hit == 2 && soundHits < 2) { soundHits = 2; sound(mc, "random.anvil_land", 0.6f, 1.1f); }
        else if (hit == 3 && soundHits < 3) { soundHits = 3; sound(mc, "random.anvil_land", 1.0f, 0.65f); }
        if (anim.breakStartedThisFrame() && !soundBreak) { soundBreak = true; sound(mc, "random.anvil_break", 1.0f, 0.9f); }
        if (anim.revealStartedThisFrame() && !soundReveal) { soundReveal = true; sound(mc, "random.levelup", 0.7f, 1.2f); }
    }

    private void sound(Minecraft mc, String name, float vol, float pitch) {
        mc.thePlayer.playSound(name, vol, pitch);
    }

    /** True when the given screen is Hypixel's "Reforge Item" chest menu. */
    private boolean isReforgeMenu(Minecraft mc, GuiScreen gui) {
        if (!(gui instanceof GuiChest)) return false;
        String title = chestTitle(mc, (GuiChest) gui);
        if (title == null) return false;
        String low = TextUtil.stripColor(title).toLowerCase();
        return low.contains("reforge");
    }

    /** The lower-chest inventory's display name (the GUI title). */
    private String chestTitle(Minecraft mc, GuiChest gui) {
        IInventory chest = chestInventory(mc, gui);
        if (chest == null) return null;
        try {
            return chest.getDisplayName().getUnformattedText();
        } catch (Exception e) {
            return chest.getName();
        }
    }

    /** The chest (non-player) inventory backing this GuiChest, or null. */
    private IInventory chestInventory(Minecraft mc, GuiChest gui) {
        Container c = gui.inventorySlots;
        if (c == null) return null;
        for (Object o : c.inventorySlots) {
            Slot slot = (Slot) o;
            if (slot.inventory != null && slot.inventory != mc.thePlayer.inventory) {
                return slot.inventory;
            }
        }
        return null;
    }

    /**
     * The single reforgeable gear item in the chest, or null if there are none
     * or more than one (ambiguous — better to show nothing than guess).
     */
    private ItemStack findReforgeInput(Minecraft mc, GuiChest gui) {
        Container c = gui.inventorySlots;
        if (c == null) return null;
        ItemStack found = null;
        for (Object o : c.inventorySlots) {
            Slot slot = (Slot) o;
            if (slot.inventory == mc.thePlayer.inventory) continue; // skip player inv
            ItemStack stack = slot.getStack();
            if (stack == null) continue;
            // A reforgeable SkyBlock item carries ExtraAttributes; decorative
            // glass panes and the menu buttons don't, so this isolates the real item.
            if (!hasExtraAttributes(stack)) continue;
            if (found != null) return null; // ambiguous
            found = stack;
        }
        return found;
    }

    private boolean hasExtraAttributes(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().hasKey("ExtraAttributes");
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static boolean eqIgnoreCase(String a, String b) {
        return a == null ? b == null : a.equalsIgnoreCase(b);
    }
}
