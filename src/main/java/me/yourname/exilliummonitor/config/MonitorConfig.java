package me.yourname.exilliummonitor.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public final class MonitorConfig {
    private final FileConfiguration config;

    public MonitorConfig(FileConfiguration config) {
        this.config = config;
    }

    public boolean isMonitoringEnabled() {
        return config.getBoolean("monitoring.enabled", true);
    }

    public boolean isTpsMonitoringEnabled() {
        return config.getBoolean("monitoring.tps.enabled", true);
    }

    public int getTpsCheckIntervalSeconds() {
        return Math.max(1, config.getInt("monitoring.tps.check-interval-seconds", 60));
    }

    public double getTpsWarningThreshold() {
        return config.getDouble("monitoring.tps.warning-threshold", 17.0D);
    }

    public double getTpsCriticalThreshold() {
        return config.getDouble("monitoring.tps.critical-threshold", 10.0D);
    }

    public int getAlertCooldownSeconds() {
        return Math.max(0, config.getInt("monitoring.tps.alert-cooldown-seconds", 300));
    }

    public boolean isMemoryMonitoringEnabled() {
        return config.getBoolean("monitoring.memory.enabled", true);
    }

    public int getMemoryWarningUsedPercent() {
        return config.getInt("monitoring.memory.warning-used-percent", 85);
    }

    public int getMemoryCriticalUsedPercent() {
        return config.getInt("monitoring.memory.critical-used-percent", 95);
    }

    public boolean isIncludeOnline() {
        return config.getBoolean("monitoring.stats.include-online", true);
    }

    public boolean isIncludeMemory() {
        return config.getBoolean("monitoring.stats.include-memory", true);
    }

    public boolean isIncludeChunks() {
        return config.getBoolean("monitoring.stats.include-chunks", true);
    }

    public boolean isIncludeEntities() {
        return config.getBoolean("monitoring.stats.include-entities", true);
    }

    public boolean isIncludeUptime() {
        return config.getBoolean("monitoring.stats.include-uptime", true);
    }

    public boolean isDiagnosticsEnabled() {
        return config.getBoolean("diagnostics.enabled", true);
    }

    public boolean isDiagnosticsRunOnWarning() {
        return config.getBoolean("diagnostics.run-on-warning", true);
    }

    public boolean isDiagnosticsRunOnCritical() {
        return config.getBoolean("diagnostics.run-on-critical", true);
    }

    public int getDiagnosticsTopChunks() {
        return Math.max(1, config.getInt("diagnostics.top-chunks", 5));
    }

    public int getDiagnosticsMaxScannedChunks() {
        return Math.max(1, config.getInt("diagnostics.max-scanned-chunks", 300));
    }

    public int getDiagnosticsNearbyPlayerRadiusBlocks() {
        return Math.max(0, config.getInt("diagnostics.nearby-player-radius-blocks", 128));
    }

    public boolean isDiagnosticsIncludeBlockEntities() {
        return config.getBoolean("diagnostics.include-block-entities", true);
    }

    public boolean isDiagnosticsIncludeEntityBreakdown() {
        return config.getBoolean("diagnostics.include-entity-breakdown", true);
    }

    public boolean isDiscordEnabled() {
        return config.getBoolean("discord.enabled", true);
    }

    public String getDiscordWebhookUrl() {
        return config.getString("discord.webhook-url", "");
    }

    public String getDiscordUsername() {
        return config.getString("discord.username", "Exillium Monitor");
    }

    public String getDiscordAvatarUrl() {
        return config.getString("discord.avatar-url", "");
    }

    public boolean isDiscordUseEmbeds() {
        return config.getBoolean("discord.use-embeds", true);
    }

    public boolean isPingEnabled() {
        return config.getBoolean("discord.ping.enabled", true);
    }

    public boolean isPingOnWarning() {
        return config.getBoolean("discord.ping.ping-on-warning", false);
    }

    public boolean isPingOnCritical() {
        return config.getBoolean("discord.ping.ping-on-critical", true);
    }

    public List<String> getPingRoleIds() {
        return config.getStringList("discord.ping.role-ids");
    }

    public List<String> getPingUserIds() {
        return config.getStringList("discord.ping.user-ids");
    }

    public boolean isDailyStatusEnabled() {
        return config.getBoolean("discord.daily-status.enabled", true);
    }

    public int getDailyStatusUpdateIntervalMinutes() {
        return Math.max(1, config.getInt("discord.daily-status.update-interval-minutes", 5));
    }

    public String getDailyStatusTitle() {
        return config.getString("discord.daily-status.title", "ExilliumMonitor Daily Status");
    }

    public boolean isPerformanceLogEnabled() {
        return config.getBoolean("performance-log.enabled", true);
    }

    public String getPerformanceLogFolder() {
        return config.getString("performance-log.folder", "logs");
    }

    public boolean isPerformanceLogSplitByDate() {
        return config.getBoolean("performance-log.split-by-date", true);
    }

    public boolean isWriteRegularStatusEnabled() {
        return config.getBoolean("performance-log.write-regular-status", true);
    }

    public int getWriteIntervalMinutes() {
        return Math.max(1, config.getInt("performance-log.write-interval-minutes", 5));
    }

    public int getKeepDays() {
        return Math.max(0, config.getInt("performance-log.keep-days", 30));
    }

    public boolean isShowTps() {
        return config.getBoolean("display.status-command.show-tps", true);
    }

    public boolean isShowMemory() {
        return config.getBoolean("display.status-command.show-memory", true);
    }

    public boolean isShowOnline() {
        return config.getBoolean("display.status-command.show-online", true);
    }

    public boolean isShowChunks() {
        return config.getBoolean("display.status-command.show-chunks", true);
    }

    public boolean isShowEntities() {
        return config.getBoolean("display.status-command.show-entities", true);
    }

    public boolean isShowUptime() {
        return config.getBoolean("display.status-command.show-uptime", true);
    }

    public boolean isDebugEnabled() {
        return config.getBoolean("debug.enabled", false);
    }
}
