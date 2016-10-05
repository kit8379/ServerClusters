package de.themoep.serverclusters.bungee;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import de.themoep.serverclusters.bungee.enums.Backend;
import de.themoep.serverclusters.bungee.storage.MysqlStorage;
import de.themoep.serverclusters.bungee.storage.ValueStorage;
import de.themoep.serverclusters.bungee.storage.YamlStorage;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

public class Cluster implements Comparable<Cluster> {

    private ServerClusters plugin = null;

    /**
     * The list of servernames in the cluster
     */
    private List<String> serverlist = new ArrayList<String>();

    /**
     * Name of the cluster
     */
    private String name;

    /**
     * List of aliases of this cluster
     */
    private List<String> aliaslist = new ArrayList<String>();

    /**
     * Map of lowercase warpnames to their warp info
     */

    private Map<String, WarpInfo> warps = new HashMap<String, WarpInfo>();

    /**
     * Allow ignoring the last logout server. This also stops storing of that value!
     */
    private boolean ignoreLogoutServer = true;

    /**
     * Map of players UUID's to the servername they logged out of
     */
    private LoadingCache<UUID, String> logoutCache = null;

    private ValueStorage logoutStorage = null;

    /**
     * The default server of this cluster one connects to the first time
     */
    private String defaultServer;

    /**
     * Whether or not this cluster should how up in the lists
     */
    private boolean hidden;

    /**
     * The cluster object
     * @param plugin     The ServerClusters plugin
     * @param name       The name of the cluster
     * @param serverlist The list of servernames this cluster contains. Cannot be empty!
     */
    public Cluster(ServerClusters plugin, String name, List<String> serverlist) {
        this(plugin, name, serverlist, serverlist.get(0));
    }

    /**
     * The cluster object
     * @param plugin        The ServerClusters plugin
     * @param name          The name of the cluster
     * @param serverlist    The list of servernames this cluster contains. Cannot be empty!
     * @param defaultServer The name of the default server the player's connect to if they weren't on the cluster before.
     */
    public Cluster(ServerClusters plugin, String name, List<String> serverlist, String defaultServer) {
        this.plugin = plugin;
        this.name = name;
        this.serverlist = serverlist;
        this.defaultServer = defaultServer;

        if (serverlist.size() > 1 && !shouldIgnoreLogoutServer()) {
            initLogoutStorage();
        }
    }

    public Cluster(ServerClusters plugin, String name, Configuration config) {
        this(plugin, name, config.getStringList("server"),config.getString("default", null));

        setAliaslist(config.getStringList("alias"));
        setHidden(config.getBoolean("hidden", false));
        setDefaultServer(config.getString("cluster", null));
        setIgnoreLogoutServer(config.getBoolean("ignoreLogoutServer", false));
    }

    private void initLogoutStorage() {
        if (plugin.getBackend() == Backend.MYSQL) {
            try {
                logoutStorage = new MysqlStorage(plugin, "logoutserver_" + getName());
            } catch (InvalidPropertiesFormatException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (logoutStorage == null) {
            logoutStorage = new YamlStorage(plugin, "logoutserver_" + getName());
        }
        logoutCache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .build(new CacheLoader<UUID, String>() {
                    @Override
                    public String load(UUID uuid) throws Exception {
                        //make the expensive call
                        if(logoutStorage != null) {
                            return logoutStorage.getValue(uuid);
                        }
                        return null;
                    }
                });
    }

    /**
     * Connects a player to the server cluster and the last server he was on
     * @param playername The name of the player
     */
    public void connectPlayer(String playername) {
        ProxiedPlayer player = plugin.getProxy().getPlayer(playername);
        if (player != null)
            connectPlayer(player);
    }

    /**
     * Connects a player to the server cluster and the last server he was on
     * @param playerid The UUID of the player
     */
    public void connectPlayer(UUID playerid) {
        ProxiedPlayer player = plugin.getProxy().getPlayer(playerid);
        if (player != null)
            connectPlayer(player);
    }

    /**
     * Connects a player to the server cluster and the last server he was on
     * @param player The player to connect
     */
    public void connectPlayer(ProxiedPlayer player) {
        String servername = getLogoutServer(player.getUniqueId());
        if (servername == null) {
            servername = getDefaultServer();
        }
        ServerInfo server = plugin.getProxy().getServers().get(servername);
        if (server != null) {
            player.connect(server);
        } else {
            player.sendMessage(new ComponentBuilder("Error:").color(ChatColor.DARK_RED).append(" The server " + servername + " does not exist!").color(ChatColor.RED).create());
        }
    }

    /**
     * Get the default server of this cluster
     * @return The name of the default server
     */
    public String getDefaultServer() {
        return defaultServer != null ? defaultServer : getServerlist().get(0);
    }

    /**
     * Set the default server of this cluster
     * @param defaultServer The name of the default server
     */
    public void setDefaultServer(String defaultServer) {
        if (defaultServer == null || getServerlist().contains(defaultServer)) {
            this.defaultServer = defaultServer;
        }
    }

    /**
     * Sets the server a player loggout out from
     * @param player     The Player to save the loggout server
     * @param servername The name of the server the player logged out from as a string
     */
    public void setLogoutServer(ProxiedPlayer player, String servername) {
        if (!shouldIgnoreLogoutServer() && getServerlist().contains(servername)) {
            logoutStorage.putValue(player.getUniqueId(), servername);
            logoutCache.put(player.getUniqueId(), servername);
        }
    }

    /**
     * Gets the name of the server a player logged out of
     * @param playerid The Player to get the servername
     * @return The servername as a string, null if not found
     */
    public String getLogoutServer(UUID playerid) {
        if (!shouldIgnoreLogoutServer() && logoutCache != null) {
            try {
                return logoutCache.get(playerid);
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public String getLogoutServer(String playername) {
        ProxiedPlayer player = plugin.getProxy().getPlayer(playername);
        if (player != null) {
            return getLogoutServer(player.getUniqueId());
        }
        return null;
    }


    public List<ProxiedPlayer> getPlayerlist() {
        // TODO Auto-generated method stub
        List<ProxiedPlayer> playerlist = new ArrayList<ProxiedPlayer>();
        for (String s : getServerlist()) {
            ServerInfo si = plugin.getProxy().getServerInfo(s);
            if (si != null)
                playerlist.addAll(si.getPlayers());
        }
        return playerlist;
    }

    /**
     * Get the list of servers in this cluster
     * @return the serverlist
     */
    public List<String> getServerlist() {
        return serverlist;
    }

    /**
     * Adds a server to a cluster
     * @param name String representing the name of the server
     */
    public void addServer(String name) {
        int countOld = serverlist.size();
        if (!serverlist.contains(name.toLowerCase())) {
            serverlist.add(name.toLowerCase());
            if (countOld == 1) {
                initLogoutStorage();
            }
        }
    }

    /**
     * Get a collection of all warps of this cluster
     * @return Collection of warp info
     */
    public Collection<WarpInfo> getWarps() {
        return warps.values();
    }

    /**
     * Get a collection of all warps of this cluster a sender has access to
     * @param sender The sender to check permissions for
     * @return Collection of warp info
     */
    public Collection<WarpInfo> getWarps(CommandSender sender) {
        Collection<WarpInfo> warps = new HashSet<WarpInfo>();
        for (WarpInfo warp : getWarps()) {
            if (plugin.getWarpManager().checkAccess(sender, warp)) {
                warps.add(warp);
            }
        }
        return warps;
    }

    /**
     * Get the location of a warp
     * @param name The name of the warp (case insensitive)
     * @return The location, <tt>null</tt> if not found
     */
    public WarpInfo getWarp(String name) {
        return warps.get(name.toLowerCase());
    }

    /**
     * Add a new warp point to this cluster
     * @param warp Info about the warp
     */
    public void addWarp(WarpInfo warp) {
        warps.put(warp.getName().toLowerCase(), warp);
    }

    /**
     * Removes a warp
     * @param name The name of the warp (case insensitive)
     * @return The old location, <tt>null</tt> if there was no warp with this name
     */
    public LocationInfo removeWarp(String name) {
        return warps.remove(name.toLowerCase());
    }

    /**
     * Get the name of the cluster
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the aliaslist
     */
    public List<String> getAliaslist() {
        return aliaslist;
    }

    /**
     * @param aliaslist the aliaslist to set
     */
    public void setAliaslist(List<String> aliaslist) {
        this.aliaslist = aliaslist;
    }

    public boolean containsServer(String servername) {
        for (String s : getServerlist()) {
            if (s.equalsIgnoreCase(servername)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param c is a non-null Cluster.
     * @throws NullPointerException if o is null.
     */
    public int compareTo(Cluster c) {
        if (this == c) {
            return 0;
        }

        return getName().compareToIgnoreCase(c.getName());
    }

    /**
     * Set whether or not this cluster should be hidden
     * @param hidden <tt>true</tt> if it should be hidden; <tt>false</tt> if not
     */
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    /**
     * Get whether or not this cluster is hidden
     * @return <tt>true</tt> if it should be hidden; <tt>false</tt> if not
     */
    public boolean isHidden() {
        return hidden;
    }

    public void destroy() {
        if (logoutStorage != null) {
            logoutStorage.close();
        }
    }

    public boolean hasAccess(CommandSender sender) {
        return sender.hasPermission("serverclusters.cluster." + getName());
    }

    public boolean shouldIgnoreLogoutServer() {
        return ignoreLogoutServer;
    }

    public void setIgnoreLogoutServer(boolean ignoreLogoutServer) {
        this.ignoreLogoutServer = ignoreLogoutServer;
    }
}
