package org.skyhunt.core.world;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Minimal schematic loader using WorldEdit if present.
 */
public class SchematicLoader {

    private final Plugin plugin;
    private Clipboard clipboard;

    public SchematicLoader(Plugin plugin) {
        this.plugin = plugin;
        loadDefault();
    }

    private void loadDefault() {
        File schemFile = new File(plugin.getDataFolder(), "island.schem");
        if (!schemFile.exists()) {
            plugin.saveResource("island.schem", false);
        }
        load(schemFile);
    }

    private void load(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             ClipboardReader reader = ClipboardFormats.findByFile(file).getReader(fis)) {
            clipboard = reader.read();
            plugin.getLogger().info("Loaded schematic: " + file.getName());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load schematic, will use fallback island: " + e.getMessage());
            clipboard = null;
        }
    }

    public boolean isLoaded() {
        return clipboard != null;
    }

    public void pasteSchematic(Location loc) {
        if (clipboard == null) return;
        World world = BukkitAdapter.adapt(loc.getWorld());
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(world)) {
            ClipboardHolder holder = new ClipboardHolder(clipboard);
            Operation op = holder.createPaste(editSession)
                .to(BukkitAdapter.asBlockVector(loc))
                .ignoreAirBlocks(false)
                .build();
            try {
                Operations.complete(op);
            } catch (com.sk89q.worldedit.WorldEditException e) {
                plugin.getLogger().warning("Failed to paste schematic: " + e.getMessage());
            }
        }
    }
}

