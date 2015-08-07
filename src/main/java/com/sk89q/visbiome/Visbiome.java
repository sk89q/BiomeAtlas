package com.sk89q.visbiome;

import com.sk89q.visbiome.command.CommandVisbiome;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.*;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.Logger;

@Mod(modid = Visbiome.MODID)
public class Visbiome {
    
    public static final String MODID = "visbiome";
    public static Logger logger;
    
    @EventHandler
    public void preInit(FMLPreInitializationEvent evt) {
        logger = evt.getModLog();
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent evt) {
        evt.registerServerCommand(new CommandVisbiome());
    }

    @EventHandler
    public void serverStarted(FMLServerStartedEvent evt) {
        String mapOnStartProp = System.getProperty("visbiome.mapApothemAtStart");
        if (mapOnStartProp != null) {
            int apothem = Integer.parseInt(mapOnStartProp);

            if (apothem > 0) {
                CommandVisbiome.generateMap(MinecraftServer.getServer().worldServerForDimension(0), 0, 0, apothem);
            }
        }
    }

}
