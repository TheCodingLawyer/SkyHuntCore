package org.skyhunt.core.world.schematic;

import org.bukkit.Location;

public class NoopSchematicPaster implements SchematicPaster {
    @Override
    public boolean isLoaded() {
        return false;
    }

    @Override
    public void paste(Location loc) {
        // no-op
    }
}




