package me.yourname.exilliummonitor.monitor;

import me.yourname.exilliummonitor.config.MonitorConfig;
import me.yourname.exilliummonitor.model.ServerStats;
import me.yourname.exilliummonitor.service.PerformanceLogService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class TpsMonitorTask {
    private final JavaPlugin plugin;
    private MonitorConfig config;
    private final ServerStatsService serverStatsService;
    private final PerformanceAlertService performanceAlertService;
    private final PerformanceLogService performanceLogService;
    private BukkitTask task;

    public TpsMonitorTask(
            JavaPlugin plugin,
            MonitorConfig config,
            ServerStatsService serverStatsService,
            PerformanceAlertService performanceAlertService,
            PerformanceLogService performanceLogService
    ) {
        this.plugin = plugin;
        this.config = config;
        this.serverStatsService = serverStatsService;
        this.performanceAlertService = performanceAlertService;
        this.performanceLogService = performanceLogService;
    }

    public void updateConfig(MonitorConfig config) {
        this.config = config;
        reschedule();
    }

    public void start() {
        if (task != null || !config.isMonitoringEnabled() || !config.isTpsMonitoringEnabled()) {
            return;
        }

        long intervalTicks = Math.max(20L, config.getTpsCheckIntervalSeconds() * 20L);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::runOnceSafely, intervalTicks, intervalTicks);
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
            performanceLogService.logRegularStatus(stats);
            performanceAlertService.handle(stats);
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Monitoring task failed: " + exception.getClass().getSimpleName());
        }
    }
}
