package com.sk89q.biomeatlas.command;

import com.google.common.collect.Lists;
import com.sk89q.biomeatlas.BiomeAtlas;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class CommandBiomeAtlas extends CommandBase {
    
    @Override
    public String getCommandName() {
        return "biomeatlas";
    }
    
    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/biomeatlas <apothem>";
    }
    
    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            EntityPlayerMP player = getCommandSenderAsPlayer(sender);
            int apothem;
            try {
                apothem = Integer.parseInt(args[0]);

                if (apothem < 0) {
                    throw new WrongUsageException("Bigger number, foo'!");
                }
            } catch (NumberFormatException e) {
                throw new WrongUsageException(this.getCommandUsage(sender));
            }

            World world = player.getEntityWorld();
            int centerX = (int) player.posX;
            int centerZ = (int) player.posZ;

            generateMap(world, centerX, centerZ, apothem);
        } else {
            throw new WrongUsageException(this.getCommandUsage(sender));
        }
    }
    
    @Override
    public int getRequiredPermissionLevel() {
        return 4;
    }
    
    @Override
    public List addTabCompletionOptions(ICommandSender par1ICommandSender, String[] par2ArrayOfStr) {
        return null;
    }

    private static void broadcast(String text) {
        ChatComponentText message = new ChatComponentText(text);
        message.getChatStyle().setColor(EnumChatFormatting.YELLOW);
        MinecraftServer.getServer().getConfigurationManager().sendChatMsg(message);
    }

    private static int getBiomeRGB(BiomeGenBase biome) {
        return biome.color != 0 ? biome.color : biome.biomeName.hashCode();
    }

    public static void generateMap(World world, int centerX, int centerZ, int apothem) {
        broadcast("Generating a map of biomes...");

        WorldChunkManager chunkManager = world.getWorldChunkManager();

        List<BiomeGenBase> biomes = Lists.newArrayList();
        for (BiomeGenBase biome : BiomeGenBase.getBiomeGenArray()) {
            if (biome != null) {
                biomes.add(biome);
            }
        }

        int centerChunkX = centerX >> 4;
        int centerChunkZ = centerZ >> 4;
        int minChunkX = centerChunkX - apothem;
        int minChunkZ = centerChunkZ - apothem;
        int maxChunkX = centerChunkX + apothem;
        int maxChunkZ = centerChunkZ + apothem;

        int textWidth = 200;
        int lineHeight = 8;
        int textHeight = biomes.size() * lineHeight;

        int length = apothem * 2 + 1;
        int count = length * length;
        int done = 0;
        long then = System.currentTimeMillis();

        BufferedImage image = new BufferedImage(length + textWidth, Math.max(length, textHeight), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = (Graphics2D) image.getGraphics();

        try {
            g2d.setColor(Color.WHITE);
            g2d.fill(new Rectangle(0, 0, image.getWidth(), image.getHeight()));
            g2d.setFont(new Font("Sans", 0, 9));
            FontMetrics fm = g2d.getFontMetrics();

            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    int x = chunkX - minChunkX;
                    int y = chunkZ - minChunkZ;

                    BiomeGenBase biome = chunkManager.getBiomeGenAt(chunkX << 4, chunkZ << 4);

                    image.setRGB(x, y, getBiomeRGB(biome));

                    done++;

                    long now = System.currentTimeMillis();
                    if (now - then > 500) {
                        broadcast(String.format("BiomeAtlas: %d/%d (%f%%)", done, count, (done / (double) count * 100)));
                        then = now;
                    }
                }
            }

            for (int i = 0; i < biomes.size(); i++) {
                int y = lineHeight * i;

                BiomeGenBase biome = biomes.get(i);
                g2d.setColor(new Color(getBiomeRGB(biome)));
                g2d.fill(new Rectangle(length + 5, y, lineHeight, lineHeight));

                g2d.setPaint(Color.BLACK);
                g2d.drawString(biome.biomeName, length + lineHeight + 10, y + fm.getHeight() / 2 + 1);
            }
        } finally {
            g2d.dispose();
        }

        try {
            File file = new File("biomeatlas_map.png");
            ImageIO.write(image, "png", file);

            broadcast("Written to: " + file.getAbsolutePath());
        } catch (IOException e) {
            BiomeAtlas.logger.error("Failed to generate biome map", e);
            broadcast("Map generation failed because the file couldn't be written! More details can be found in the log.");
        }
    }
    
}
