package de.themoep.serverclusters.bukkit;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

/**
 * Created by Phoenix616 on 08.01.2015.
 */
public class BungeePluginMessageListener implements PluginMessageListener {

    ServerClustersBukkit plugin = null;

    public BungeePluginMessageListener(ServerClustersBukkit plugin) {
        this.plugin = plugin;

        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "sc:tptoplayer", this);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "sc:tptolocation", this);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "sc:getlocation", this);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "sc:playerlocation");
    }

    public void onPluginMessageReceived(String channel, Player recevier, byte[] message) {
        if (channel.startsWith("sc:")) {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);

            plugin.debug(recevier.getName() + " received plugin message on channel '" + channel + "'");

            if ("sc:tptoplayer".equals(channel)) {
                String playername = in.readUTF();
                String targetname = in.readUTF();
                plugin.getTeleportManager().teleport(playername, targetname);

            } else if ("sc:tptolocation".equals(channel)) {
                String playername = in.readUTF();
                String worldname = in.readUTF();
                double x = in.readDouble();
                double y = in.readDouble();
                double z = in.readDouble();
                float yaw = in.readFloat();
                float pitch = in.readFloat();
                World world = plugin.getServer().getWorld(worldname);
                if (world == null) {
                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeUTF("unknown world");
                    out.writeUTF(worldname);
                    recevier.sendPluginMessage(this.plugin, "sc:error", out.toByteArray());
                    return;
                }

                Location loc = new Location(plugin.getServer().getWorld(worldname), x, y, z, yaw, pitch);
                plugin.getTeleportManager().teleport(playername, loc);

            } else if ("sc:getlocation".equals(channel)) {
                String reason = in.readUTF();
                String sender = in.readUTF();
                Location loc = recevier.getLocation();

                ByteArrayDataOutput out = ByteStreams.newDataOutput();

                out.writeUTF(reason);
                out.writeUTF(sender);
                out.writeUTF(loc.getWorld().getName());
                out.writeDouble(loc.getX());
                out.writeDouble(loc.getY());
                out.writeDouble(loc.getZ());
                out.writeFloat(loc.getYaw());
                out.writeFloat(loc.getPitch());

                recevier.sendPluginMessage(this.plugin, "sc:playerlocation", out.toByteArray());
            }
        }
    }
}
