package me.yourname.exilliummonitor.monitor;

import me.yourname.exilliummonitor.config.MonitorConfig;
import me.yourname.exilliummonitor.discord.AlertPayloadBuilder;
import me.yourname.exilliummonitor.discord.DiscordWebhookClient;
import me.yourname.exilliummonitor.model.AlertLevel;
import me.yourname.exilliummonitor.model.LagDiagnosticsReport;
import me.yourname.exilliummonitor.model.PerformanceAlert;
import me.yourname.exilliummonitor.model.ServerStats;
import me.yourname.exilliummonitor.service.LagDiagnosticsService;
import me.yourname.exilliummonitor.service.PerformanceLogService;
import me.yourname.exilliummonitor.util.FormatUtil;

import java.time.Instant;
import java.util.Optional;

public final class PerformanceAlertService {
    private MonitorConfig config;
    private final DiscordWebhookClient discordWebhookClient;
    private final AlertPayloadBuilder payloadBuilder;
    private final PerformanceLogService performanceLogService;
    private final LagDiagnosticsService lagDiagnosticsService;
    private long lastWarningAlertMillis;
    private long lastCriticalAlertMillis;

    public PerformanceAlertService(
            MonitorConfig config,
            DiscordWebhookClient discordWebhookClient,
            AlertPayloadBuilder payloadBuilder,
            PerformanceLogService performanceLogService,
            LagDiagnosticsService lagDiagnosticsService
    ) {
        this.config = config;
        this.discordWebhookClient = discordWebhookClient;
        this.payloadBuilder = payloadBuilder;
        this.performanceLogService = performanceLogService;
        this.lagDiagnosticsService = lagDiagnosticsService;
    }

    public void updateConfig(MonitorConfig config) {
        this.config = config;
        this.lagDiagnosticsService.updateConfig(config);
    }

    public Optional<PerformanceAlert> evaluate(ServerStats stats) {
        if (!config.isMonitoringEnabled()) {
            return Optional.empty();
        }

        PerformanceAlert alert = chooseHighestPriority(stats);
        if (alert == null || isCoolingDown(alert.level())) {
            return Optional.empty();
        }

        recordAlertTime(alert.level());
        return Optional.of(alert);
    }

    public Optional<PerformanceAlert> handle(ServerStats stats) {
        Optional<PerformanceAlert> alert = evaluate(stats);
        alert.ifPresent(this::sendAndLog);
        return alert;
    }

    public void sendTestAlert(ServerStats stats) {
        sendTestAlert(stats, false);
    }

    public void sendTestAlert(ServerStats stats, boolean includeMentions) {
        PerformanceAlert alert = new PerformanceAlert(
                AlertLevel.INFO,
                "ExilliumMonitor test alert",
                "This is a test alert from ExilliumMonitor.",
                stats,
                Instant.now(),
                includeMentions && config.isPingEnabled(),
                null
        );
        sendAndLog(alert);
    }

    private void sendAndLog(PerformanceAlert alert) {
        performanceLogService.logAlert(alert);
        discordWebhookClient.send(payloadBuilder.build(alert));
    }

    private PerformanceAlert chooseHighestPriority(ServerStats stats) {
        PerformanceAlert tpsAlert = tpsAlert(stats);
        PerformanceAlert memoryAlert = memoryAlert(stats);

        if (isCritical(tpsAlert)) {
            return tpsAlert;
        }
        if (isCritical(memoryAlert)) {
            return memoryAlert;
        }
        if (tpsAlert != null) {
            return tpsAlert;
        }
        return memoryAlert;
    }

    private PerformanceAlert tpsAlert(ServerStats stats) {
        if (!config.isTpsMonitoringEnabled()) {
            return null;
        }

        if (stats.tps1m() <= config.getTpsCriticalThreshold()) {
            return alert(AlertLevel.CRITICAL, "Critical TPS Alert",
                    "TPS is critically low: " + FormatUtil.decimal(stats.tps1m(), 2), stats);
        }
        if (stats.tps1m() <= config.getTpsWarningThreshold()) {
            return alert(AlertLevel.WARNING, "TPS Warning",
                    "TPS is low: " + FormatUtil.decimal(stats.tps1m(), 2), stats);
        }
        return null;
    }

    private PerformanceAlert memoryAlert(ServerStats stats) {
        if (!config.isMemoryMonitoringEnabled()) {
            return null;
        }

        double usedPercent = stats.usedMemoryPercent();
        if (usedPercent >= config.getMemoryCriticalUsedPercent()) {
            return alert(AlertLevel.CRITICAL, "Critical Memory Alert",
                    "Memory usage is critical: " + FormatUtil.decimal(usedPercent, 0) + "%", stats);
        }
        if (usedPercent >= config.getMemoryWarningUsedPercent()) {
            return alert(AlertLevel.WARNING, "Memory Warning",
                    "Memory usage is high: " + FormatUtil.decimal(usedPercent, 0) + "%", stats);
        }
        return null;
    }

    private PerformanceAlert alert(AlertLevel level, String title, String message, ServerStats stats) {
        LagDiagnosticsReport diagnosticsReport = null;
        try {
            diagnosticsReport = lagDiagnosticsService.diagnose(level);
        } catch (RuntimeException exception) {
            diagnosticsReport = null;
        }
        return new PerformanceAlert(level, title, message, stats, Instant.now(), shouldPing(level), diagnosticsReport);
    }

    private boolean shouldPing(AlertLevel level) {
        if (!config.isPingEnabled()) {
            return false;
        }
        return switch (level) {
            case CRITICAL -> config.isPingOnCritical();
            case WARNING -> config.isPingOnWarning();
            case INFO -> false;
        };
    }

    private boolean isCritical(PerformanceAlert alert) {
        return alert != null && alert.level() == AlertLevel.CRITICAL;
    }

    private void recordAlertTime(AlertLevel level) {
        long now = System.currentTimeMillis();
        if (level == AlertLevel.CRITICAL) {
            lastCriticalAlertMillis = now;
            return;
        }
        if (level == AlertLevel.WARNING) {
            lastWarningAlertMillis = now;
        }
    }

    private boolean isCoolingDown(AlertLevel level) {
        int cooldownSeconds = config.getAlertCooldownSeconds();
        long lastAlertMillis = switch (level) {
            case CRITICAL -> lastCriticalAlertMillis;
            case WARNING -> Math.max(lastWarningAlertMillis, lastCriticalAlertMillis);
            case INFO -> 0L;
        };
        if (cooldownSeconds <= 0 || lastAlertMillis == 0L) {
            return false;
        }
        long elapsedMillis = System.currentTimeMillis() - lastAlertMillis;
        return elapsedMillis < cooldownSeconds * 1000L;
    }
}
