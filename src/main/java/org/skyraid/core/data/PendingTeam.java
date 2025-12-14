package org.skyraid.core.data;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a team that hasn't been created yet (in game room phase)
 * Holds members before island generation
 */
public class PendingTeam {
    
    private final UUID ownerId;
    private final List<Player> members;
    private final long createdAt;
    private String teamName;
    
    public PendingTeam(Player owner) {
        this.ownerId = owner.getUniqueId();
        this.members = new ArrayList<>();
        this.members.add(owner); // Owner is always first member
        this.createdAt = System.currentTimeMillis();
        this.teamName = owner.getName() + "'s Team";
    }
    
    public UUID getOwnerId() {
        return ownerId;
    }
    
    public List<Player> getMembers() {
        return new ArrayList<>(members); // Return copy for safety
    }
    
    public void addMember(Player player) {
        if (!members.contains(player) && members.size() < 8) {
            members.add(player);
        }
    }
    
    public void removeMember(Player player) {
        // Can't remove owner
        if (!player.getUniqueId().equals(ownerId)) {
            members.remove(player);
        }
    }
    
    public boolean hasMember(Player player) {
        return members.stream().anyMatch(p -> p.getUniqueId().equals(player.getUniqueId()));
    }
    
    public int getMemberCount() {
        return members.size();
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
    
    /**
     * Check if this pending team has expired (30 minutes)
     */
    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt > 1800000; // 30 minutes
    }
}



