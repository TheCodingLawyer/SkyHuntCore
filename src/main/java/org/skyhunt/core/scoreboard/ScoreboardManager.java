package org.skyhunt.core.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import java.lang.reflect.Method;
import java.util.Locale;
import org.skyhunt.SkyHuntCorePlugin;
import org.skyhunt.core.config.ConfigService;
import org.skyhunt.core.config.LevelDefinition;
import org.skyhunt.core.data.IslandData;
import org.skyhunt.core.data.MissionCategory;
import org.skyhunt.core.service.IslandService;
import org.skyhunt.core.service.MissionService;

import java.util.List;

public class ScoreboardManager {

    private final SkyHuntCorePlugin plugin;
    private final ConfigService config;
    private final IslandService islands;
    private final MissionService missions;

    public ScoreboardManager(SkyHuntCorePlugin plugin, ConfigService config, IslandService islands, MissionService missions) {
        this.plugin = plugin;
        this.config = config;
        this.islands = islands;
        this.missions = missions;
    }

    public void start() {
        if (!config.isScoreboardEnabled()) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    render(player);
                }
            }
        }.runTaskTimer(plugin, 0L, config.getScoreboardInterval());
    }

    private void render(Player player) {
        IslandData island = islands.getIsland(player);
        // Only show scoreboard if player has created an island
        if (island == null || island.getIslandX() == 0 && island.getIslandZ() == 0) {
            return;
        }

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("skyhunt", "dummy", color(config.getScoreboardTitle()));
        applyBlankNumberFormat(obj);
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        LevelDefinition def = islands.getLevelDef(island);

        double headPct = pct(player, def, MissionCategory.HEADHUNTING);
        double minePct = pct(player, def, MissionCategory.MINING);
        double farmPct = pct(player, def, MissionCategory.FARMING);
        double overall = (headPct + minePct + farmPct) / 3.0;

        List<String> layout = config.getScoreboardLayout();
        int score = Math.min(layout.size(), 15);
        for (int i = 0; i < score; i++) {
            String line = layout.get(i);
            String out = line
                .replace("{level}", String.valueOf(island.getIslandLevel()))
                .replace("{overall}", format(overall))
                .replace("{headhunting}", format(headPct))
                .replace("{mining}", format(minePct))
                .replace("{farming}", format(farmPct))
                .replace("{balance}", formatMoney(player));
            obj.getScore(color(out)).setScore(score--);
        }
        player.setScoreboard(board);
    }

    private double pct(Player player, LevelDefinition def, MissionCategory cat) {
        if (def == null) return 0.0;
        var tasks = def.getTasks(cat);
        if (tasks.isEmpty()) return 0.0;
        double completed = 0;
        for (var t : tasks) {
            var prog = missions.getProgressMap(player.getUniqueId()).get(t.missionId(def.getLevel()));
            if (prog != null && prog.isCompleted()) {
                completed += 1.0;
            }
        }
        return (completed / tasks.size()) * 100.0;
    }

    private String format(double value) {
        return String.format(Locale.US, "%.0f", value);
    }

    private String formatMoney(Player player) {
        var econ = plugin.getEconomy();
        if (econ == null) return "0";
        return String.format(Locale.US, "%,.0f", econ.getBalance(player));
    }

    private String color(String msg) {
        return msg.replace("&", "§");
    }

    private void applyBlankNumberFormat(Objective obj) {
        try {
            // Paper provides numberFormat(NumberFormat) - use reflection to avoid hard dependency
            Class<?> numberFormatClass = Class.forName("io.papermc.paper.scoreboard.numbers.NumberFormat");
            Method blank = numberFormatClass.getMethod("blank");
            Object blankFormat = blank.invoke(null);
            Method setFormat = obj.getClass().getMethod("numberFormat", numberFormatClass);
            setFormat.invoke(obj, blankFormat);
            plugin.getLogger().info("Successfully applied blank number format to scoreboard");
        } catch (Exception e) {
            plugin.getLogger().warning("Could not apply blank number format (Paper API required): " + e.getMessage());
            // Fallback: numbers will show on right on non-Paper/Spigot
        }
    }
    
    public void clearScoreboard(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }
}

