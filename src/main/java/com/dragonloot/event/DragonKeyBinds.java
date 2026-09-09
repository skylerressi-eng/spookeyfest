package com.dragonloot.event;

import com.dragonloot.gui.DragonGuideScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

/** One keybind (default: H) to open the Dragon Luck guide. Rebind in Controls. */
public class DragonKeyBinds {

    private final KeyBinding openGuide;

    public DragonKeyBinds() {
        openGuide = new KeyBinding("Open Dragon Luck guide", Keyboard.KEY_H, "DragonLoot");
        ClientRegistry.registerKeyBinding(openGuide);
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (openGuide.isPressed()) {
            Minecraft.getMinecraft().displayGuiScreen(new DragonGuideScreen());
        }
    }
}
