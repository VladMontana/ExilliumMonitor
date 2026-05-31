package me.yourname.exilliummonitor.monitor;

import me.yourname.exilliummonitor.config.MonitorConfig;
import me.yourname.exilliummonitor.model.ServerStats;
import me.yourname.exilliummonitor.service.DailyStatusService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class DailyStatusTask {
    private final JavaPlugin plugin;
    private final ServerStatsService serverStatsService;
    private final DailyStatusService dailyStatusService;
    private MonitorConfig config;
    private BukkitTask task;

    public DailyStatusTask(
            JavaPlugin plugin,
            MonitorConfig config,
            ServerStatsService serverStatsService,
            DailyStatusService dailyStatusService
    ) {
        this.plugin = plugin;
        this.config = config;
        this.serverStatsService = serverStatsService;
        this.dailyStatusService = dailyStatusService;
    }

    public void updateConfig(MonitorConfig config) {
        this.config = config;
        reschedule();
    }

    public void start() {
        if (task != null || !config.isDiscordEnabled() || !config.isDailyStatusEnabled()) {
            return;
        }

        long intervalTicks = Math.max(20L, config.getDailyStatusUpdateIntervalMinutes() * 60L * 20L);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::runOnceSafely, 20L, intervalTicks);
    }

    public void reschedule() {
        stop();
        start();
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void runOnceSafely() {
        try {
            ServerStats stats = serverStatsService.collect();
            dailyStatusService.updateStatus(stats);
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Daily status task failed: " + exception.getClass().getSimpleName());
        }
    }
}
