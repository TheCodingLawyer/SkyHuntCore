package org.skyhunt.core.gui;

import org.skyhunt.SkyHuntCorePlugin;
import org.skyhunt.core.service.HeadService;
import org.skyhunt.core.service.IslandService;
import org.skyhunt.core.service.MissionService;

public class GuiManager {

    private final IslandMainGUI islandMainGUI;
    private final MissionsGUI missionsGUI;
    private final HeadsGUI headsGUI;

    public GuiManager(SkyHuntCorePlugin plugin, IslandService islands, MissionService missions, HeadService heads) {
        this.islandMainGUI = new IslandMainGUI(islands, missions, heads);
        this.missionsGUI = new MissionsGUI(islands, missions);
        this.headsGUI = new HeadsGUI(plugin, heads);
    }

    public IslandMainGUI getIslandMainGUI() {
        return islandMainGUI;
    }

    public MissionsGUI getMissionsGUI() {
        return missionsGUI;
    }

    public HeadsGUI getHeadsGUI() {
        return headsGUI;
    }
}




