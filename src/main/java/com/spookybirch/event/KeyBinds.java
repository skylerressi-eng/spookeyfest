package com.spookybirch.event;

import com.spookybirch.gui.GuideScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

/** A single keybind (default: G) to open the guide GUI. Rebind in Controls. */
public class KeyBinds {

    private final KeyBinding openGuide;

    public KeyBinds() {
        openGuide = new KeyBinding("Open SpookyBirch guide", Keyboard.KEY_G, "SpookyBirch");
        ClientRegistry.registerKeyBinding(openGuide);
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (openGuide.isPressed()) {
            Minecraft.getMinecraft().displayGuiScreen(new GuideScreen());
        }
    }
}
