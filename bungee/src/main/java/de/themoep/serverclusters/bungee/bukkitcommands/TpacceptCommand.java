package de.themoep.serverclusters.bungee.bukkitcommands;

import de.themoep.serverclusters.bungee.LocationInfo;
import de.themoep.serverclusters.bungee.ServerClusters;
import de.themoep.vnpbungee.VNPBungee;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class TpacceptCommand extends BukkitCommand {

    public TpacceptCommand(ServerClusters plugin, String name, String permission) {
        super(plugin, name, permission);
    }

    @Override
    public void run(CommandSender sender, LocationInfo location, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(ChatColor.RED + "Error: " + ChatColor.YELLOW + "This command can only be run by a player!");
            return;
        }
        ProxiedPlayer p = (ProxiedPlayer) sender;
        if (args.length == 0) {
            plugin.getTeleportManager().acceptLastRequest(p);
        } else if (args.length == 1) {
            plugin.getTeleportManager().acceptRequest(p, args[0]);
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: " + ChatColor.YELLOW + "/" + this.getName() + " [<playername>]");
        }
    }

    public Iterable<String> onTabComplete(CommandSender sender, String[] strings) {
        List<String> playerNames = new ArrayList<>();
        if (strings.length == 0) {
            for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
                if (!plugin.shouldHideVanished() || plugin.getVnpbungee() == null || !plugin.getVnpbungee().canSee(sender, player)) {
                    playerNames.add(player.getName());
                }
            }
            Collections.sort(playerNames);
        } else if (strings.length == 1) {
            String input = strings[0].toLowerCase();
            for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
                if (!player.getName().toLowerCase().startsWith(input))
                    continue;
                if (!plugin.shouldHideVanished() || plugin.getVnpbungee() == null || !plugin.getVnpbungee().canSee(sender, player)) {
                    playerNames.add(player.getName());
                }
            }
            Collections.sort(playerNames);
        }
        return playerNames;
    }
}
