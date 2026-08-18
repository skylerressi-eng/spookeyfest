package com.spookybirch;

import com.spookybirch.command.SpookyCommand;
import com.spookybirch.core.SpookyConfig;
import com.spookybirch.event.KeyBinds;
import com.spookybirch.event.StateUpdater;
import com.spookybirch.hud.SpookyHud;

import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * SpookyBirch — a Birch-style overlay for the Hypixel SkyBlock Spooky Festival.
 *
 * Registers the HUD renderer, the per-tick state updater, the keybind and the
 * /spooky command. Everything hangs off {@link com.spookybirch.core.SpookyState}
 * so features stay decoupled.
 */
@Mod(modid = SpookyBirch.MODID, name = SpookyBirch.NAME, version = SpookyBirch.VERSION, clientSideOnly = true)
public class SpookyBirch {

    public static final String MODID = "spookybirch";
    public static final String NAME = "SpookyBirch";
    public static final String VERSION = "1.0.0";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        SpookyConfig.INSTANCE.load(event.getModConfigurationDirectory());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new StateUpdater());
        MinecraftForge.EVENT_BUS.register(new SpookyHud());
        MinecraftForge.EVENT_BUS.register(new KeyBinds());
        ClientCommandHandler.instance.registerCommand(new SpookyCommand());
    }
}
