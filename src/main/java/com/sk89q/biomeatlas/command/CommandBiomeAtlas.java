package com.sk89q.biomeatlas.command;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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
import java.util.*;
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

        int centerChunkX = centerX >> 4;
        int centerChunkZ = centerZ >> 4;
        int minChunkX = centerChunkX - apothem;
        int minChunkZ = centerChunkZ - apothem;
        int maxChunkX = centerChunkX + apothem;
        int maxChunkZ = centerChunkZ + apothem;

        int mapLength = apothem * 2 + 1;
        BufferedImage mapImage = new BufferedImage(mapLength, mapLength, BufferedImage.TYPE_INT_RGB);

        // Progress tracking
        int chunkCount = mapLength * mapLength;
        int completedChunks = 0;
        long lastMessageTime = System.currentTimeMillis();

        Set<BiomeGenBase> seenBiomes = Sets.newHashSet();

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                int x = chunkX - minChunkX;
                int y = chunkZ - minChunkZ;

                BiomeGenBase biome = chunkManager.getBiomeGenAt(chunkX << 4, chunkZ << 4);
                seenBiomes.add(biome);

                mapImage.setRGB(x, y, getBiomeRGB(biome));

                completedChunks++;

                long now = System.currentTimeMillis();
                if (now - lastMessageTime > 500) {
                    broadcast(String.format("BiomeAtlas: %d/%d (%f%%)", completedChunks, chunkCount, (completedChunks / (double) chunkCount * 100)));
                    lastMessageTime = now;
                }
            }
        }

        broadcast("Creating output image...");

        List<BiomeGenBase> sortedBiomes = Lists.newArrayList(seenBiomes);
        Collections.sort(sortedBiomes, new BiomeColorComparator());

        int lineHeight = 8;
        int legendWidth = 200;
        int legendHeight = sortedBiomes.size() * lineHeight;

        BufferedImage outputImage = new BufferedImage(mapLength + legendWidth, Math.max(mapLength, legendHeight), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = (Graphics2D) outputImage.getGraphics();

        try {
            // Background
            g2d.setColor(Color.WHITE);
            g2d.fill(new Rectangle(0, 0, outputImage.getWidth(), outputImage.getHeight()));

            // Copy image to output image
            g2d.drawImage(mapImage, 0, 0, null);

            g2d.setFont(new Font("Sans", 0, 9));
            FontMetrics fm = g2d.getFontMetrics();

            int i = 0;
            for (BiomeGenBase biome : sortedBiomes) {
                int y = lineHeight * i;
                g2d.setColor(new Color(getBiomeRGB(biome)));
                g2d.fill(new Rectangle(mapLength + 5, y, lineHeight, lineHeight));

                g2d.setPaint(Color.BLACK);
                g2d.drawString(biome.biomeName, mapLength + lineHeight + 10, y + fm.getHeight() / 2 + 1);

                i++;
            }
        } finally {
            g2d.dispose();
        }

        broadcast("Writing image to disk...");

        try {
            File file = new File("biomeatlas_map.png");
            ImageIO.write(outputImage, "png", file);

            broadcast("Written to: " + file.getAbsolutePath());
        } catch (IOException e) {
            BiomeAtlas.logger.error("Failed to generate biome map", e);
            broadcast("Map generation failed because the file couldn't be written! More details can be found in the log.");
        }
    }

    private static class BiomeColorComparator implements Comparator<BiomeGenBase> {

        @Override
        public int compare(BiomeGenBase o1, BiomeGenBase o2) {
            Color c1 = new Color(o1.color);
            Color c2 = new Color(o2.color);
            float[] hsv1 = new float[3];
            float[] hsv2 = new float[3];
            Color.RGBtoHSB(c1.getRed(), c1.getGreen(), c1.getBlue(), hsv1);
            Color.RGBtoHSB(c2.getRed(), c2.getGreen(), c2.getBlue(), hsv2);
            if (hsv1[0] < hsv2[0]) {
                return -1;
            } else if (hsv1[0] > hsv2[0]) {
                return 1;
            } else {
                return 0;
            }
        }
    }
    
}
