package org.skyhunt.core.data;

import java.util.UUID;

/**
 * Stores per-player island progression metadata.
 */
public class IslandData {

    private final UUID ownerId;
    private int islandLevel;
    private long createdAt;
    private long lastLogin;
    private int islandX;
    private int islandZ;
    private int homeX;
    private int homeY;
    private int homeZ;
    private float homeYaw;
    private float homePitch;

    public IslandData(UUID ownerId, int islandLevel, long createdAt, long lastLogin) {
        this.ownerId = ownerId;
        this.islandLevel = islandLevel;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public int getIslandLevel() {
        return islandLevel;
    }

    public void setIslandLevel(int islandLevel) {
        this.islandLevel = islandLevel;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }

    public int getIslandX() {
        return islandX;
    }

    public void setIslandX(int islandX) {
        this.islandX = islandX;
    }

    public int getIslandZ() {
        return islandZ;
    }

    public void setIslandZ(int islandZ) {
        this.islandZ = islandZ;
    }

    public int getHomeX() {
        return homeX;
    }

    public void setHomeX(int homeX) {
        this.homeX = homeX;
    }

    public int getHomeY() {
        return homeY;
    }

    public void setHomeY(int homeY) {
        this.homeY = homeY;
    }

    public int getHomeZ() {
        return homeZ;
    }

    public void setHomeZ(int homeZ) {
        this.homeZ = homeZ;
    }

    public float getHomeYaw() {
        return homeYaw;
    }

    public void setHomeYaw(float homeYaw) {
        this.homeYaw = homeYaw;
    }

    public float getHomePitch() {
        return homePitch;
    }

    public void setHomePitch(float homePitch) {
        this.homePitch = homePitch;
    }
}

