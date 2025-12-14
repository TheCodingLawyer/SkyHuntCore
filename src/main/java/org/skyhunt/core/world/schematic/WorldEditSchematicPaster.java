package org.skyhunt.core.world.schematic;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * WorldEdit-based schematic paster (ported from SkyRaidCore)
 * Uses FAWE/WorldEdit API for proper schematic loading and pasting
 */
public class WorldEditSchematicPaster implements SchematicPaster {

    private final Plugin plugin;
    private File schematicFile;
    private boolean loaded = false;

    public WorldEditSchematicPaster(Plugin plugin) {
        this.plugin = plugin;
        loadSchematic();
    }

    /**
     * Load schematic from plugin folder
     */
    private void loadSchematic() {
        // Only use island.schem - the format that works with WorldEdit
        String[] schematicNames = {"island.schem"};
        
        plugin.getLogger().info("═══════════════════════════════════════");
        plugin.getLogger().info("LOADING SCHEMATIC WITH WORLDEDIT API");
        
        // Log available formats for debugging
        try {
            StringBuilder formatList = new StringBuilder("Available formats: ");
            for (ClipboardFormat f : ClipboardFormats.getAll()) {
                formatList.append(f.getName()).append(" (").append(String.join(",", f.getFileExtensions())).append("), ");
            }
            plugin.getLogger().info(formatList.toString());
        } catch (Exception e) {
            plugin.getLogger().warning("Could not list available formats: " + e.getMessage());
        }
        
        File pluginFolder = plugin.getDataFolder();
        if (!pluginFolder.exists()) {
            pluginFolder.mkdirs();
        }
        
        for (String schematicName : schematicNames) {
            // Extract from JAR if available
            File targetFile = new File(pluginFolder, schematicName);
            if (!targetFile.exists() && plugin.getResource(schematicName) != null) {
                try {
                    plugin.getLogger().info("Extracting " + schematicName + " from JAR...");
                    plugin.saveResource(schematicName, false);
                    plugin.getLogger().info("✓ Extracted to: " + targetFile.getAbsolutePath());
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to extract " + schematicName + ": " + e.getMessage());
                    continue;
                }
            }
            
            // Verify the file exists and is valid
            if (targetFile.exists()) {
                try {
                    plugin.getLogger().info("Checking schematic: " + targetFile.getAbsolutePath() + " (" + targetFile.length() + " bytes)");
                    
                    // Try findByFile first (uses extension)
                    ClipboardFormat format = ClipboardFormats.findByFile(targetFile);
                    
                    // If that fails, try findByAlias with common format names
                    if (format == null) {
                        plugin.getLogger().info("findByFile returned null, trying findByAlias...");
                        // Try sponge schematic (modern .schem)
                        format = ClipboardFormats.findByAlias("sponge");
                        if (format == null) {
                            format = ClipboardFormats.findByAlias("schem");
                        }
                        if (format == null) {
                            format = ClipboardFormats.findByAlias("schematic");
                        }
                        if (format == null) {
                            format = ClipboardFormats.findByAlias("mcedit");
                        }
                    }
                    
                    if (format != null) {
                        plugin.getLogger().info("✓ Format detected: " + format.getName());
                        schematicFile = targetFile;
                        loaded = true;
                        plugin.getLogger().info("✓ Schematic loaded successfully: " + schematicName);
                        plugin.getLogger().info("═══════════════════════════════════════");
                        return;
                    } else {
                        plugin.getLogger().warning("✗ No valid schematic format found for: " + schematicName);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("✗ Failed to load " + schematicName + ": " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                plugin.getLogger().info("Not found: " + schematicName);
            }
        }
        
        plugin.getLogger().warning("═══════════════════════════════════════");
        plugin.getLogger().warning("✗ NO VALID SCHEMATIC FOUND!");
        plugin.getLogger().warning("Will use fallback island generation.");
        plugin.getLogger().warning("Place a 'island.schem' file in plugins/SkyHuntCore/");
        plugin.getLogger().warning("OR install WorldEdit/FastAsyncWorldEdit");
        plugin.getLogger().warning("═══════════════════════════════════════");
        loaded = false;
    }

    @Override
    public boolean isLoaded() {
        return loaded && schematicFile != null;
    }

    @Override
    public void paste(Location location) {
        if (!loaded || schematicFile == null) {
            plugin.getLogger().warning("Cannot paste: schematic not loaded");
            return;
        }
        
        if (location.getWorld() == null) {
            plugin.getLogger().severe("Cannot paste: world is null");
            return;
        }
        
        try {
            plugin.getLogger().info("═══════════════════════════════════════");
            plugin.getLogger().info("PASTING SCHEMATIC WITH WORLDEDIT");
            
            // Load schematic using WorldEdit (creates fresh clipboard each time)
            ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
            if (format == null) {
                plugin.getLogger().severe("Could not determine schematic format");
                return;
            }
            
            Clipboard clipboard;
            try (FileInputStream fis = new FileInputStream(schematicFile);
                 ClipboardReader reader = format.getReader(fis)) {
                clipboard = reader.read();
            }
            
            // Get dimensions for centering
            int width = clipboard.getDimensions().getBlockX();
            int height = clipboard.getDimensions().getBlockY();
            int length = clipboard.getDimensions().getBlockZ();
            
            plugin.getLogger().info("Schematic dimensions: " + width + "x" + height + "x" + length);
            
            // Center the schematic horizontally, but NOT vertically
            int offsetX = width / 2;
            int offsetY = height / 2;
            int offsetZ = length / 2;
            
            Location pasteLocation = location.clone().subtract(offsetX, offsetY, offsetZ);
            
            plugin.getLogger().info("Paste location: " + 
                pasteLocation.getBlockX() + "," + 
                pasteLocation.getBlockY() + "," + 
                pasteLocation.getBlockZ());
            
            // Set origin to minimum point for proper pasting
            clipboard.setOrigin(clipboard.getRegion().getMinimumPoint());
            
            // Paste using WorldEdit API (SkyRaidCore method)
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(new BukkitWorld(location.getWorld()))) {
                Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(BlockVector3.at(pasteLocation.getX(), pasteLocation.getY(), pasteLocation.getZ()))
                    .copyEntities(true)
                    .ignoreAirBlocks(false)
                    .build();
                
                Operations.complete(operation);
                Operations.complete(editSession.commit());
                
                plugin.getLogger().info("✓ SCHEMATIC PASTED SUCCESSFULLY!");
                plugin.getLogger().info("═══════════════════════════════════════");
            }
            
        } catch (IOException | WorldEditException e) {
            plugin.getLogger().severe("═══════════════════════════════════════");
            plugin.getLogger().severe("✗ ERROR PASTING SCHEMATIC: " + e.getMessage());
            e.printStackTrace();
            plugin.getLogger().severe("═══════════════════════════════════════");
        }
    }
}
