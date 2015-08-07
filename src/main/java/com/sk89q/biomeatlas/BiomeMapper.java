package com.sk89q.biomeatlas;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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

import static com.google.common.base.Preconditions.checkNotNull;

public class BiomeMapper {

    private final List<Predicate<String>> listeners = Lists.newArrayList();

    public List<Predicate<String>> getListeners() {
        return listeners;
    }

    public void generate(World world, int centerX, int centerZ, int apothem, File outputFile) {
        checkNotNull(outputFile, "outputFile");

        sendStatus("Generating a map of biomes...");

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
                    sendStatus(String.format("BiomeAtlas: %d/%d (%f%%)", completedChunks, chunkCount, (completedChunks / (double) chunkCount * 100)));
                    lastMessageTime = now;
                }
            }
        }

        sendStatus("Creating output image...");

        int lineHeight = 8;
        int legendWidth = 200;
        int legendHeight = seenBiomes.size() * lineHeight;

        BufferedImage outputImage = new BufferedImage(mapImage.getWidth() + legendWidth, Math.max(mapImage.getHeight(), legendHeight), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = (Graphics2D) outputImage.getGraphics();

        try {
            // Background
            g2d.setColor(Color.WHITE);
            g2d.fill(new Rectangle(0, 0, outputImage.getWidth(), outputImage.getHeight()));

            // Copy image to output image
            g2d.drawImage(mapImage, 0, 0, null);
        } finally {
            g2d.dispose();
        }

        paintLegend(outputImage, seenBiomes, mapImage.getWidth() + 5, 0);

        try {
            ImageIO.write(outputImage, "png", outputFile);

            sendStatus("Written to: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            BiomeAtlas.logger.error("Failed to generate biome map", e);
            sendStatus("Map generation failed because the file couldn't be written! More details can be found in the log.");
        }
    }

    private void sendStatus(String message) {
        for (Predicate observer : listeners) {
            if (observer.apply(message)) {
                return;
            }
        }
    }

    private void paintLegend(BufferedImage image, Set<BiomeGenBase> biomes, int baseX, int baseY) {
        List<BiomeGenBase> sortedBiomes = Lists.newArrayList(biomes);
        Collections.sort(sortedBiomes, new BiomeColorComparator());

        int lineHeight = 8;

        Graphics2D g2d = (Graphics2D) image.getGraphics();

        try {
            g2d.setFont(new Font("Sans", 0, 9));
            FontMetrics fm = g2d.getFontMetrics();

            int i = 0;
            for (BiomeGenBase biome : sortedBiomes) {
                int y = lineHeight * i;
                g2d.setColor(new Color(getBiomeRGB(biome)));
                g2d.fill(new Rectangle(baseX, baseY + y, lineHeight, lineHeight));

                g2d.setPaint(Color.BLACK);
                g2d.drawString(biome.biomeName, baseX + lineHeight + 5, baseY + y + fm.getHeight() / 2 + 1);

                i++;
            }
        } finally {
            g2d.dispose();
        }
    }

    private static int getBiomeRGB(BiomeGenBase biome) {
        return biome.color != 0 ? biome.color : biome.biomeName.hashCode();
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
