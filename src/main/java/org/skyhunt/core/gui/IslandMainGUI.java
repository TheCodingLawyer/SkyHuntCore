package org.skyhunt.core.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.skyhunt.core.config.LevelDefinition;
import org.skyhunt.core.data.IslandData;
import org.skyhunt.core.data.MissionCategory;
import org.skyhunt.core.service.HeadService;
import org.skyhunt.core.service.IslandService;
import org.skyhunt.core.service.MissionService;

import java.util.EnumMap;
import java.util.Map;

/**
 * Island main GUI: quick links to missions, heads, info.
 */
public class IslandMainGUI {

    private final IslandService islands;
    private final MissionService missions;
    private final HeadService heads;

    public IslandMainGUI(IslandService islands, MissionService missions, HeadService heads) {
        this.islands = islands;
        this.missions = missions;
        this.heads = heads;
    }

    public void open(Player player) {
        IslandData data = islands.getIsland(player);
        Inventory inv = Bukkit.createInventory(null, 27, GuiUtil.color("&9&lSkyHunt Island"));

        // Background glass (bordered)
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, GuiUtil.simpleItem(Material.WHITE_STAINED_GLASS_PANE, " ", null));
        }

        // Info (left)
        inv.setItem(10, GuiUtil.simpleItem(Material.BOOK, "&eIsland Info",
            java.util.List.of(
                "&7Level: &f" + data.getIslandLevel(),
                "&7Coords: &f" + data.getIslandX() + "," + data.getIslandZ(),
                "&7Balance: &a$" + player.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class).getProvider().getBalance(player)
            )));

        // Missions
        LevelDefinition def = islands.getLevelDef(data);
        Map<MissionCategory, String> percents = new EnumMap<>(MissionCategory.class);
        if (def != null) {
            for (MissionCategory cat : MissionCategory.values()) {
                long done = def.getTasks(cat).stream()
                    .filter(t -> {
                        var p = missions.getProgressMap(player.getUniqueId()).get(t.missionId(def.getLevel()));
                        return p != null && p.isCompleted();
                    }).count();
                long total = def.getTasks(cat).size();
                percents.put(cat, total == 0 ? "0%" : Math.round(done * 100.0 / total) + "%");
            }
        }

        inv.setItem(12, GuiUtil.simpleItem(Material.WRITABLE_BOOK, "&bMissions",
            java.util.List.of(
                "&7HeadHunting: &f" + percents.getOrDefault(MissionCategory.HEADHUNTING, "0%"),
                "&7Mining: &f" + percents.getOrDefault(MissionCategory.MINING, "0%"),
                "&7Farming: &f" + percents.getOrDefault(MissionCategory.FARMING, "0%"),
                "",
                "&eClick to view missions"
            )));

        // Heads
        inv.setItem(14, GuiUtil.simpleItem(Material.PLAYER_HEAD, "&dHeads",
            java.util.List.of(
                "&7Sell unlocked heads",
                "&eClick to view heads"
            )));

        // Home
        inv.setItem(16, GuiUtil.simpleItem(Material.ENDER_PEARL, "&aGo Home",
            java.util.List.of("&7Teleport to your island")));

        // Close / Back (center bottom)
        inv.setItem(22, GuiUtil.simpleItem(Material.ARROW, "&cClose",
            java.util.List.of("&7Return")));

        player.openInventory(inv);
    }

    public void handleClick(Player player, InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;
        Material type = e.getCurrentItem().getType();
        switch (type) {
            case ENDER_PEARL -> islands.teleportHome(player, islands.getIsland(player));
            case WRITABLE_BOOK -> player.performCommand("island missions");
            case PLAYER_HEAD -> player.performCommand("island heads");
            case ARROW -> player.closeInventory();
            default -> {
            }
        }
    }
}

