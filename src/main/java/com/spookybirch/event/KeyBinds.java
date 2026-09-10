package com.spookybirch.event;

import com.spookybirch.dragon.gui.DragonRouletteScreen;
import com.spookybirch.gui.GuideScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

/**
 * Keybinds for SpookyBirch. Both default to keys that are unbound in vanilla
 * 1.8.9 (G, K) and can be rebound in Options → Controls.
 */
public class KeyBinds {

    private final KeyBinding openGuide;
    private final KeyBinding openRoulette;

    public KeyBinds() {
        openGuide = new KeyBinding("Open SpookyBirch guide", Keyboard.KEY_G, "SpookyBirch");
        openRoulette = new KeyBinding("Open Dragon Altar Roulette", Keyboard.KEY_K, "SpookyBirch");
        ClientRegistry.registerKeyBinding(openGuide);
        ClientRegistry.registerKeyBinding(openRoulette);
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (openGuide.isPressed()) {
            mc.displayGuiScreen(new GuideScreen());
        }
        if (openRoulette.isPressed()) {
            mc.displayGuiScreen(new DragonRouletteScreen());
        }
    }
}
