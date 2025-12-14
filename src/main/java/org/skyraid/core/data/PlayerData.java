package org.skyraid.core.data;

import java.util.UUID;

/**
 * Represents player-specific data in SkyRaid
 */
public class PlayerData {
    
    private final UUID playerUUID;
    private String playerName;
    private UUID currentTeamId;  // Null if not in a team
    private long joinedAt;
    private boolean useTeamChat = false;  // Team chat toggle
    private long firstJoinTime;
    private UUID pendingInvite;  // UUID of player who invited this player
    
    public PlayerData(UUID playerUUID, String playerName) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.joinedAt = System.currentTimeMillis();
        this.firstJoinTime = System.currentTimeMillis();
        this.currentTeamId = null;
    }
    
    // Getters and Setters
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    public UUID getCurrentTeamId() {
        return currentTeamId;
    }
    
    public void setCurrentTeamId(UUID teamId) {
        this.currentTeamId = teamId;
    }
    
    public boolean isInTeam() {
        return currentTeamId != null;
    }
    
    public long getJoinedAt() {
        return joinedAt;
    }
    
    public void setJoinedAt(long joinedAt) {
        this.joinedAt = joinedAt;
    }
    
    public long getFirstJoinTime() {
        return firstJoinTime;
    }
    
    public boolean isUsingTeamChat() {
        return useTeamChat;
    }
    
    public void setUseTeamChat(boolean useTeamChat) {
        this.useTeamChat = useTeamChat;
    }
    
    public void toggleTeamChat() {
        this.useTeamChat = !this.useTeamChat;
    }
    
    public UUID getPendingInvite() {
        return pendingInvite;
    }
    
    public void setPendingInvite(UUID pendingInvite) {
        this.pendingInvite = pendingInvite;
    }
}

