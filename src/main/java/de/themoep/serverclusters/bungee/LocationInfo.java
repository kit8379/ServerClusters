package de.themoep.serverclusters.bungee;

import com.google.common.base.Preconditions;

public class LocationInfo {
    private String server;
    private String world;
    private double x;
    private double y;
    private double z;
    private float yaw = 0;
    private float pitch = 0;

    private LocationInfo(String server, String world, double x, double y, double z) {
        Preconditions.checkArgument(server != null, "server");
        Preconditions.checkArgument(world != null, "world");
        this.server = server;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public LocationInfo(String server, String world, double x, double y, double z, float yaw, float pitch){
        this(server, world, x, y, z);
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public String getServer() {
        return server;
    }

    public String getWorld() {
        return world;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }
}
