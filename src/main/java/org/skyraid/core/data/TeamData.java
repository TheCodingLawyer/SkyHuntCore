package org.skyraid.core.data;

import java.util.*;

/**
 * Represents a team/island with members and island data
 */
public class TeamData {
    
    private final UUID teamId;
    private final UUID leaderId;
    private String teamName;
    private long createdAt;
    private long balance;
    private int islandX;
    private int islandZ;
    private int homeX;
    private int homeY;
    private int homeZ;
    private float homeYaw;
    private float homePitch;
    
    // Members and roles
    private final Set<UUID> members = new HashSet<>();
    private final Set<UUID> leaders = new HashSet<>();  // Promoted members with leader permissions
    
    // Forcefield data
    private long forcefieldEndTime = 0;
    
    // Border color
    private String borderColor = "BLUE"; // Default blue border
    
    // Pending invites
    private final Set<UUID> pendingInvites = new HashSet<>();
    
    public TeamData(UUID teamId, UUID leaderId, String teamName, int islandX, int islandZ) {
        this.teamId = teamId;
        this.leaderId = leaderId;
        this.teamName = teamName;
        this.createdAt = System.currentTimeMillis();
        this.balance = 0;
        this.islandX = islandX;
        this.islandZ = islandZ;
        
        // Initialize home position at island center
        this.homeX = islandX;
        this.homeY = 100;  // Default height
        this.homeZ = islandZ;
        this.homeYaw = 0;
        this.homePitch = 0;
        
        // Add leader to members
        members.add(leaderId);
    }
    
    // Getters and Setters
    public UUID getTeamId() {
        return teamId;
    }
    
    public UUID getLeaderId() {
        return leaderId;
    }
    
    public String getTeamName() {
        return teamName;
    }
    
    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public long getBalance() {
        return balance;
    }
    
    public void setBalance(long balance) {
        this.balance = balance;
    }
    
    public void addBalance(long amount) {
        this.balance += amount;
    }
    
    public int getIslandX() {
        return islandX;
    }
    
    public int getIslandZ() {
        return islandZ;
    }
    
    public int getHomeX() {
        return homeX;
    }
    
    public int getHomeY() {
        return homeY;
    }
    
    public int getHomeZ() {
        return homeZ;
    }
    
    public void setHome(int x, int y, int z, float yaw, float pitch) {
        this.homeX = x;
        this.homeY = y;
        this.homeZ = z;
        this.homeYaw = yaw;
        this.homePitch = pitch;
    }
    
    public float getHomeYaw() {
        return homeYaw;
    }
    
    public float getHomePitch() {
        return homePitch;
    }
    
    // Member management
    public Set<UUID> getMembers() {
        return new HashSet<>(members);
    }
    
    public boolean isMember(UUID playerUUID) {
        return members.contains(playerUUID);
    }
    
    public void addMember(UUID playerUUID) {
        members.add(playerUUID);
    }
    
    public void removeMember(UUID playerUUID) {
        members.remove(playerUUID);
        leaders.remove(playerUUID);
        pendingInvites.remove(playerUUID);
    }
    
    public int getMemberCount() {
        return members.size();
    }
    
    // Leader management
    public Set<UUID> getLeaders() {
        return new HashSet<>(leaders);
    }
    
    public boolean isLeader(UUID playerUUID) {
        return playerUUID.equals(leaderId) || leaders.contains(playerUUID);
    }
    
    public void promoteToLeader(UUID playerUUID) {
        if (members.contains(playerUUID)) {
            leaders.add(playerUUID);
        }
    }
    
    public void demoteFromLeader(UUID playerUUID) {
        leaders.remove(playerUUID);
    }
    
    // Invite management
    public Set<UUID> getPendingInvites() {
        return new HashSet<>(pendingInvites);
    }
    
    public boolean hasInvite(UUID playerUUID) {
        return pendingInvites.contains(playerUUID);
    }
    
    public void addInvite(UUID playerUUID) {
        pendingInvites.add(playerUUID);
    }
    
    public void removeInvite(UUID playerUUID) {
        pendingInvites.remove(playerUUID);
    }
    
    // Forcefield management
    public long getForcefieldEndTime() {
        return forcefieldEndTime;
    }
    
    public void setForcefieldEndTime(long endTime) {
        this.forcefieldEndTime = endTime;
    }
    
    public boolean hasForcefield() {
        return forcefieldEndTime > System.currentTimeMillis();
    }
    
    public long getRemainingForcefieldTime() {
        if (!hasForcefield()) {
            return 0;
        }
        return forcefieldEndTime - System.currentTimeMillis();
    }
    
    public long getRemainingForcefieldHours() {
        long ms = getRemainingForcefieldTime();
        if (ms <= 0) return 0;
        return ms / (1000 * 60 * 60);
    }
    
    // Border color management
    public String getBorderColor() {
        return borderColor;
    }
    
    public void setBorderColor(String borderColor) {
        this.borderColor = borderColor;
    }
}

