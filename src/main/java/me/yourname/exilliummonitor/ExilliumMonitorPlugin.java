package me.yourname.exilliummonitor;

import me.yourname.exilliummonitor.command.ExilliumMonitorCommand;
import me.yourname.exilliummonitor.config.MonitorConfig;
import me.yourname.exilliummonitor.discord.AlertPayloadBuilder;
import me.yourname.exilliummonitor.discord.DiscordWebhookClient;
import me.yourname.exilliummonitor.monitor.PerformanceAlertService;
import me.yourname.exilliummonitor.monitor.ServerStatsService;
import me.yourname.exilliummonitor.monitor.TpsMonitorTask;
import me.yourname.exilliummonitor.service.PerformanceLogService;
import me.yourname.exilliummonitor.util.MentionBuilder;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class ExilliumMonitorPlugin extends JavaPlugin {
    private MonitorConfig monitorConfig;
    private ServerStatsService serverStatsService;
    private PerformanceLogService performanceLogService;
    private DiscordWebhookClient discordWebhookClient;
    private AlertPayloadBuilder alertPayloadBuilder;
    private PerformanceAlertService performanceAlertService;
    private TpsMonitorTask tpsMonitorTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        initializeServices();
        registerCommand();
        tpsMonitorTask.start();
        getLogger().info("Plugin enabled.");
    }

    @Override
    public void onDisable() {
        if (tpsMonitorTask != null) {
            tpsMonitorTask.stop();
        }
        if (discordWebhookClient != null) {
            discordWebhookClient.shutdown();
        }
        if (performanceLogService != null) {
            performanceLogService.close();
        }
        getLogger().info("Plugin disabled.");
    }

    public void reloadMonitor() {
        reloadConfig();
        monitorConfig = new MonitorConfig(getConfig());
        performanceLogService.updateConfig(monitorConfig);
        discordWebhookClient.updateConfig(monitorConfig);
        alertPayloadBuilder.updateConfig(monitorConfig);
        performanceAlertService.updateConfig(monitorConfig);
        tpsMonitorTask.updateConfig(monitorConfig);
    }

    public MonitorConfig getMonitorConfig() {
        return monitorConfig;
    }

    private void initializeServices() {
        monitorConfig = new MonitorConfig(getConfig());
        serverStatsService = new ServerStatsService();
        performanceLogService = new PerformanceLogService(this, monitorConfig);
        discordWebhookClient = new DiscordWebhookClient(this, monitorConfig);
        alertPayloadBuilder = new AlertPayloadBuilder(monitorConfig, new MentionBuilder());
        performanceAlertService = new PerformanceAlertService(
                monitorConfig,
                discordWebhookClient,
                alertPayloadBuilder,
                performanceLogService
        );
        tpsMonitorTask = new TpsMonitorTask(
                this,
                monitorConfig,
                serverStatsService,
                performanceAlertService,
                performanceLogService
        );
    }

    private void registerCommand() {
        PluginCommand command = getCommand("exilliummonitor");
        if (command == null) {
            getLogger().severe("Command exilliummonitor is missing from plugin.yml.");
            return;
        }

        ExilliumMonitorCommand executor = new ExilliumMonitorCommand(this, serverStatsService, performanceAlertService);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }
}
