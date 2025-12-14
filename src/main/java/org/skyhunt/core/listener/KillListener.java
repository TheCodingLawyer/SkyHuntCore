package org.skyhunt.core.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.skyhunt.core.service.HeadService;
import org.skyhunt.core.service.MissionService;
import org.skyhunt.core.database.DatabaseManager;

/**
 * Tracks mob kills for missions and handles head drops for unlocked mobs.
 */
public class KillListener implements Listener {

    private final MissionService missions;
    private final HeadService heads;
    private final DatabaseManager database;

    public KillListener(MissionService missions, HeadService heads, DatabaseManager database) {
        this.missions = missions;
        this.heads = heads;
        this.database = database;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        String mobType = event.getEntityType().name();
        missions.handleKill(killer, mobType);

        if (database.isHeadUnlocked(killer.getUniqueId(), mobType)) {
            event.getDrops().add(heads.createHeadItem(mobType));
            String msg = killer.getServer().getPluginManager().getPlugin("SkyHuntCore")
                .getConfig().getString("messages.head-drop", "&7+1 {mob} head")
                .replace("{mob}", mobType);
            killer.sendMessage(msg.replace("&", "§"));
        }
    }
}




