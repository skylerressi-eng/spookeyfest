package com.dragonloot;

import com.dragonloot.command.DragonCommand;
import com.dragonloot.core.DragonLootConfig;
import com.dragonloot.event.DragonKeyBinds;
import com.dragonloot.event.DragonWatcher;

import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * DragonLoot — a gambling-style egg reveal for the Hypixel SkyBlock dragon.
 *
 * Summon and slay a dragon in The End and the mod plays a dragon-egg-cracking
 * animation that gambles up the rarity ladder (Uncommon → Rare → Epic →
 * Legendary): each crack either breaks the egg open where it is or escalates it
 * one tier. Purely a client-side cosmetic — it reads chat to know when a dragon
 * died and never automates or touches your inventory.
 *
 * Ships alongside SpookyBirch in the same jar; both are independent @Mod entries.
 */
@Mod(modid = DragonLoot.MODID, name = DragonLoot.NAME, version = DragonLoot.VERSION, clientSideOnly = true)
public class DragonLoot {

    public static final String MODID = "dragonloot";
    public static final String NAME = "DragonLoot";
    public static final String VERSION = "1.0.0";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        DragonLootConfig.INSTANCE.load(event.getModConfigurationDirectory());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new DragonWatcher());
        MinecraftForge.EVENT_BUS.register(new DragonKeyBinds());
        ClientCommandHandler.instance.registerCommand(new DragonCommand());
    }
}
