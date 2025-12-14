package org.skyhunt.core.data;

import java.util.UUID;

/**
 * Tracks per-player mission progress for a specific category + mission id.
 */
public class MissionProgress {

    private final UUID playerId;
    private final MissionCategory category;
    private final String missionId;
    private long progress;
    private boolean completed;

    public MissionProgress(UUID playerId, MissionCategory category, String missionId, long progress, boolean completed) {
        this.playerId = playerId;
        this.category = category;
        this.missionId = missionId;
        this.progress = progress;
        this.completed = completed;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public MissionCategory getCategory() {
        return category;
    }

    public String getMissionId() {
        return missionId;
    }

    public long getProgress() {
        return progress;
    }

    public void setProgress(long progress) {
        this.progress = progress;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}




