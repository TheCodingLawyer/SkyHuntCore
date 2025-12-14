package org.skyraid.core.util;

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
import org.skyraid.SkyRaidPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * WorldEdit-based schematic loader
 * Uses FAWE/WorldEdit API for proper schematic loading and pasting
 */
public class SchematicLoader {
    
    private final SkyRaidPlugin plugin;
    private File schematicFile;
    private boolean loaded = false;
    private boolean worldEditAvailable = false;
    
    public SchematicLoader(SkyRaidPlugin plugin) {
        this.plugin = plugin;
        checkWorldEdit();
        if (worldEditAvailable) {
            loadSchematic();
        }
    }
    
    /**
     * Check if WorldEdit is available
     */
    private void checkWorldEdit() {
        try {
            Class.forName("com.sk89q.worldedit.WorldEdit");
            worldEditAvailable = true;
            plugin.logInfo("WorldEdit detected! Using WorldEdit API for schematic loading.");
        } catch (ClassNotFoundException e) {
            plugin.logWarning("WorldEdit not found! Schematic pasting will not work.");
            plugin.logWarning("Please install WorldEdit or FastAsyncWorldEdit to enable schematic features.");
            worldEditAvailable = false;
        }
    }
    
    /**
     * Load schematic from JAR or filesystem
     */
    private void loadSchematic() {
        String schematicName = "island.schem";
        
        // First try to extract from JAR to plugin folder
        File pluginFolder = plugin.getDataFolder();
        if (!pluginFolder.exists()) {
            pluginFolder.mkdirs();
        }
        
        File targetFile = new File(pluginFolder, schematicName);
        
        // Always extract from JAR (overwrites old schematic on plugin update)
        try {
            InputStream resourceStream = plugin.getResource(schematicName);
            if (resourceStream != null) {
                if (targetFile.exists()) {
                    plugin.logInfo("Updating existing schematic from JAR: " + schematicName);
                } else {
                    plugin.logInfo("Extracting schematic from JAR: " + schematicName);
                }
                Files.copy(resourceStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                resourceStream.close();
                plugin.logInfo("Schematic ready at: " + targetFile.getAbsolutePath());
            }
        } catch (Exception e) {
            plugin.logWarning("Could not extract " + schematicName + " from JAR: " + e.getMessage());
        }
        
        // Try multiple locations
        File[] locations = {
            targetFile, // Plugin folder
            new File(schematicName), // Server root
            new File(plugin.getDataFolder().getParent(), schematicName) // Plugins folder
        };
        
        for (File file : locations) {
            if (file.exists()) {
                try {
                    plugin.logInfo("Found schematic: " + file.getAbsolutePath());
                    
                    // Verify it's a valid schematic using WorldEdit
                    ClipboardFormat format = ClipboardFormats.findByFile(file);
                    if (format != null) {
                        plugin.logInfo("Schematic format detected: " + format.getName());
                        schematicFile = file;
                        loaded = true;
                        plugin.logInfo("✓ Schematic loaded successfully!");
                        return;
                    } else {
                        plugin.logWarning("File is not a valid schematic format: " + file.getName());
                    }
                } catch (Exception e) {
                    plugin.logWarning("Failed to load " + file.getAbsolutePath() + ": " + e.getMessage());
                }
            }
        }
        
        plugin.logWarning("Could not find valid " + schematicName + "! Will use fallback island generator.");
        loaded = false;
    }
    
    /**
     * Paste schematic at location using WorldEdit API
     */
    public void pasteSchematic(Location location) {
        if (!worldEditAvailable) {
            plugin.logError("WorldEdit not available, cannot paste schematic");
            return;
        }
        
        if (!loaded || schematicFile == null) {
            plugin.logWarning("Schematic not loaded, cannot paste");
            return;
        }
        
        if (location.getWorld() == null) {
            plugin.logError("World is null");
            return;
        }
        
        try {
            plugin.logInfo("Pasting schematic using WorldEdit API...");
            
            // Load schematic using WorldEdit
            ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
            if (format == null) {
                plugin.logError("Could not determine schematic format");
                return;
            }
            
            ClipboardReader reader = format.getReader(new FileInputStream(schematicFile));
            Clipboard clipboard = reader.read();
            
            // Get dimensions for centering
            int width = clipboard.getDimensions().getBlockX();
            int height = clipboard.getDimensions().getBlockY();
            int length = clipboard.getDimensions().getBlockZ();
            
            plugin.logInfo("Schematic dimensions: " + width + "x" + height + "x" + length);
            
            // Center the schematic
            int newWidth = width / 2;
            int newHeight = height / 2;
            int newLength = length / 2;
            
            Location pasteLocation = location.clone().subtract(newWidth, newHeight, newLength);
            
            // Set origin to minimum point
            clipboard.setOrigin(clipboard.getRegion().getMinimumPoint());
            
            // Paste using WorldEdit
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(new BukkitWorld(location.getWorld()))) {
                Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(BlockVector3.at(pasteLocation.getX(), pasteLocation.getY(), pasteLocation.getZ()))
                    .copyEntities(true)
                    .ignoreAirBlocks(false)
                    .build();
                
                Operations.complete(operation);
                Operations.complete(editSession.commit());
                
                plugin.logInfo("✓ Schematic pasted successfully!");
            }
            
        } catch (IOException | WorldEditException e) {
            plugin.logError("Error pasting schematic: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public boolean isLoaded() {
        return worldEditAvailable && loaded && schematicFile != null;
    }
}
