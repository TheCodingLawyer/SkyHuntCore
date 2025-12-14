package org.skyhunt.core.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.skyhunt.core.scoreboard.ScoreboardManager;

/**
 * Clears scoreboard when player quits.
 */
public class QuitListener implements Listener {

    private final ScoreboardManager scoreboardManager;

    public QuitListener(ScoreboardManager scoreboardManager) {
        this.scoreboardManager = scoreboardManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Clear scoreboard to prevent any residual display issues
        scoreboardManager.clearScoreboard(event.getPlayer());
    }
}


