package org.skyhunt.core.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.skyhunt.core.config.LevelDefinition;
import org.skyhunt.core.config.MissionTask;
import org.skyhunt.core.data.IslandData;
import org.skyhunt.core.data.MissionCategory;
import org.skyhunt.core.data.MissionProgress;
import org.skyhunt.core.service.IslandService;
import org.skyhunt.core.service.MissionService;

import java.util.ArrayList;
import java.util.List;

public class MissionsGUI {

    private final IslandService islands;
    private final MissionService missions;

    public MissionsGUI(IslandService islands, MissionService missions) {
        this.islands = islands;
        this.missions = missions;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, GuiUtil.color("&b&lMissions"));
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, GuiUtil.simpleItem(Material.WHITE_STAINED_GLASS_PANE, " ", null));
        }

        IslandData island = islands.getIsland(player);
        LevelDefinition def = islands.getLevelDef(island);
        if (def == null) {
            player.sendMessage("§cNo missions configured.");
            return;
        }

        // Spread books across the row: slots 10, 13, 16 (full row width)
        int[] bookSlots = {10, 13, 16};
        int bookIndex = 0;
        for (MissionCategory cat : MissionCategory.values()) {
            if (bookIndex >= bookSlots.length) break;
            List<String> lore = new ArrayList<>();
            for (MissionTask task : def.getTasks(cat)) {
                MissionProgress prog = missions.getProgressMap(player.getUniqueId()).get(task.missionId(def.getLevel()));
                long current = prog == null ? 0 : prog.getProgress();
                boolean done = prog != null && prog.isCompleted();
                lore.add("&7" + task.getTypeName() + " &f" + current + "/" + task.getRequired() + (done ? " &a✓" : ""));
            }
            inv.setItem(bookSlots[bookIndex], GuiUtil.simpleItem(Material.WRITABLE_BOOK, "&e" + cat.name(),
                lore));
            bookIndex++;
        }

        // Level up indicator
        boolean allComplete = missions.areAllMissionsComplete(player.getUniqueId(), def);
        inv.setItem(22, GuiUtil.simpleItem(allComplete ? Material.LIME_DYE : Material.BARRIER,
            allComplete ? "&aReady to Level Up" : "&cMissions Incomplete",
            allComplete
                ? List.of("&7Click to /levelup")
                : List.of("&7Complete all missions to level up")));

        // Back button bottom middle
        inv.setItem(49, GuiUtil.simpleItem(Material.ARROW, "&cBack", List.of("&7Return to island")));

        player.openInventory(inv);
    }

    public void handleClick(Player player, InventoryClickEvent e) {
        if (e.getSlot() == 22) {
            player.closeInventory();
            player.performCommand("levelup");
        }
        if (e.getSlot() == 49) {
            player.closeInventory();
            player.performCommand("island");
        }
    }
}

