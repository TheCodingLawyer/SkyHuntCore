package org.skyhunt.core.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.skyhunt.SkyHuntCorePlugin;
import org.skyhunt.core.service.HeadService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class HeadsGUI {

    private final SkyHuntCorePlugin plugin;
    private final HeadService heads;

    public HeadsGUI(SkyHuntCorePlugin plugin, HeadService heads) {
        this.plugin = plugin;
        this.heads = heads;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, GuiUtil.color("&d&lHeads"));
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, GuiUtil.simpleItem(Material.WHITE_STAINED_GLASS_PANE, " ", null));
        }

        Set<String> unlocked = plugin.getDatabase().loadUnlockedHeads(player.getUniqueId());
        List<String> allMobs = heads.getMobTypes();

        int[] slots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};
        int idx = 0;
        for (String mob : allMobs) {
            if (idx >= slots.length) break;
            boolean isUnlocked = unlocked.contains(mob);
            double price = heads.getPrice(mob);
            List<String> lore = new ArrayList<>();
            lore.add("&7Price: &a$" + price);
            lore.add(isUnlocked ? "&aUnlocked" : "&cLocked");
            lore.add("&eClick to sell one");
            lore.add("&eShift-Click to sell all");
            ItemStack display = heads.createDisplayHead(mob, isUnlocked);
            var meta = display.getItemMeta();
            meta.setDisplayName(GuiUtil.color((isUnlocked ? "&f" : "&8") + mob + " Head"));
            meta.setLore(lore.stream().map(GuiUtil::color).toList());
            display.setItemMeta(meta);
            inv.setItem(slots[idx], display);
            idx++;
        }

        // Back button bottom middle
        inv.setItem(49, GuiUtil.simpleItem(Material.ARROW, "&cBack", List.of("&7Return to island")));

        player.openInventory(inv);
    }

    public void handleClick(Player player, InventoryClickEvent e) {
        ItemStack item = e.getCurrentItem();
        if (item == null || !heads.isHead(item)) return;
        String mob = heads.getMobType(item);
        if (mob == null) return;

        if (e.isShiftClick()) {
            heads.sellAll(player, mob);
        } else {
            heads.sellOne(player, item);
        }
        if (e.getSlot() == 49) {
            player.closeInventory();
            player.performCommand("island");
        }
    }
}

