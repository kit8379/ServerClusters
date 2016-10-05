package de.themoep.serverclusters.bukkit;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.themoep.serverclusters.bukkit.manager.TeleportManager;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class ServerClustersBukkit extends JavaPlugin {

    private TeleportManager tpman;

    public void onEnable() {

        getLogger().log(Level.INFO, "Initialising Teleport Manager");
        tpman = new TeleportManager(this);

        getLogger().log(Level.INFO, "Registering Plugin Message Channel");
        getServer().getMessenger().registerIncomingPluginChannel(this, "ServerClusters", new BungeePluginMessageListener(this));
        getServer().getMessenger().registerOutgoingPluginChannel(this, "ServerClusters");

        getLogger().log(Level.INFO, "Registering Event Listener");
        getServer().getPluginManager().registerEvents(getTeleportManager(), this);
    }

    /**
     * Get the teleport manager.
     */
    public TeleportManager getTeleportManager() {
        return tpman;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Player player;
        String senderName = sender.getName();
        if (sender instanceof Player) {
            player = (Player) sender;
        } else if (getServer().getOnlinePlayers().size() > 0) {
            senderName = "[@]";
            player = getServer().getOnlinePlayers().iterator().next();
        } else {
            sender.sendMessage(ChatColor.RED + "This command can only be run with at least one player online as it relies on plugin messages!");
            return true;
        }
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("RunCommand");
        out.writeUTF(senderName);
        out.writeUTF(cmd.getName());
        out.writeBoolean(sender instanceof Player);
        if (sender instanceof Player) {
            out.writeUTF(player.getLocation().getWorld().getName());
            out.writeDouble(player.getLocation().getX());
            out.writeDouble(player.getLocation().getY());
            out.writeDouble(player.getLocation().getZ());
            out.writeFloat(player.getLocation().getYaw());
            out.writeFloat(player.getLocation().getPitch());
        }
        out.writeUTF(StringUtils.join(args, " "));
        player.sendPluginMessage(this, "ServerClusters", out.toByteArray());
        return true;
    }
}
