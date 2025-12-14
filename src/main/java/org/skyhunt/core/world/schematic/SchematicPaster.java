package org.skyhunt.core.world.schematic;

import org.bukkit.Location;

public interface SchematicPaster {
    boolean isLoaded();
    void paste(Location loc);
}




