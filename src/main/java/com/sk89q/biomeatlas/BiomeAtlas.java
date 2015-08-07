package com.sk89q.biomeatlas;

import com.sk89q.biomeatlas.command.CommandBiomeAtlas;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.*;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.Logger;

@Mod(modid = BiomeAtlas.MODID)
public class BiomeAtlas {
    
    public static final String MODID = "biomeatlas";
    public static Logger logger;
    
    @EventHandler
    public void preInit(FMLPreInitializationEvent evt) {
        logger = evt.getModLog();
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent evt) {
        evt.registerServerCommand(new CommandBiomeAtlas());
    }

    @EventHandler
    public void serverStarted(FMLServerStartedEvent evt) {
        String mapOnStartProp = System.getProperty("biomeatlas.mapApothemAtStart");
        if (mapOnStartProp != null) {
            int apothem = Integer.parseInt(mapOnStartProp);

            if (apothem > 0) {
                CommandBiomeAtlas.generateMap(MinecraftServer.getServer().worldServerForDimension(0), 0, 0, apothem);
            }
        }
    }

}
