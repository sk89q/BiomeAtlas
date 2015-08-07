package com.sk89q.biomeatlas;

import com.google.common.base.Predicate;
import com.sk89q.biomeatlas.command.CommandBiomeAtlas;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import org.apache.logging.log4j.Logger;

import java.io.File;

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
        if (System.getProperty("biomeAtlas.mapOnStartup", "false").equals("true")) {
            int apothem = Integer.parseInt(System.getProperty("biomeAtlas.apothem", "250"));
            int dimension = Integer.parseInt(System.getProperty("biomeAtlas.mapDimension", "0"));
            int centerX = Integer.parseInt(System.getProperty("biomeAtlas.centerX", "0"));
            int centerZ = Integer.parseInt(System.getProperty("biomeAtlas.centerZ", "0"));

            if (apothem > 0) {
                World world = MinecraftServer.getServer().worldServerForDimension(dimension);

                BiomeMapper mapper = new BiomeMapper();
                mapper.getListeners().add(new LoggerObserver());
                mapper.generate(world, centerX, centerZ, apothem, new File("biomeatlas_" + world.getSeed() + ".png"));

                if (System.getProperty("biomeAtlas.exitOnFinish", "false").equals("true")) {
                    logger.info("BiomeAtlas finished generating! Now exiting Java as enabled.");
                    FMLCommonHandler.instance().exitJava(0, false);
                }
            }
        }
    }

    private static class LoggerObserver implements Predicate<String> {
        @Override
        public boolean apply(String input) {
            logger.info(input);
            return false;
        }
    }

}
