package com.sk89q.biomeatlas.command;

import com.google.common.base.Predicate;
import com.sk89q.biomeatlas.BiomeMapper;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import java.io.File;
import java.util.List;

public class CommandBiomeAtlas extends CommandBase {
    
    @Override
    public String getCommandName() {
        return "biomeatlas";
    }
    
    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/biomeatlas <apothem> [<resolution>]";
    }
    
    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length >= 1 && args.length <= 2) {
            EntityPlayerMP player = getCommandSenderAsPlayer(sender);
            int apothem;
            int resolution = 1;

            try {
                apothem = Integer.parseInt(args[0]);
                if (apothem < 0) {
                    throw new WrongUsageException("Apothem should be >= 1");
                }
            } catch (NumberFormatException e) {
                throw new WrongUsageException(this.getCommandUsage(sender));
            }

            if (args.length >= 2) {
                try {
                    resolution = Integer.parseInt(args[1]);
                    if (resolution < 1) {
                        throw new WrongUsageException("Resolution should be >= 1");
                    }
                } catch (NumberFormatException e) {
                    throw new WrongUsageException(this.getCommandUsage(sender));
                }
            }

            World world = player.getEntityWorld();
            int centerX = (int) player.posX;
            int centerZ = (int) player.posZ;

            BiomeMapper mapper = new BiomeMapper();
            mapper.setResolution(resolution);
            mapper.getListeners().add(new BroadcastObserver());
            mapper.generate(world, centerX, centerZ, apothem, new File("biomeatlas_" + world.getSeed() + ".png"));
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

    private static class BroadcastObserver implements Predicate<String> {
        @Override
        public boolean apply(String input) {
            ChatComponentText message = new ChatComponentText(input);
            message.getChatStyle().setColor(EnumChatFormatting.YELLOW);
            MinecraftServer.getServer().getConfigurationManager().sendChatMsg(message);
            return false;
        }
    }

}
