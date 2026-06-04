package me.yourname.exilliummonitor.service;

import me.yourname.exilliummonitor.config.MonitorConfig;
import me.yourname.exilliummonitor.model.LagChunkDiagnostic;
import me.yourname.exilliummonitor.model.LagDiagnosticsReport;
import me.yourname.exilliummonitor.model.PerformanceAlert;
import me.yourname.exilliummonitor.model.ServerStats;
import me.yourname.exilliummonitor.util.FormatUtil;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;

public final class PerformanceLogService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final JavaPlugin plugin;
    private MonitorConfig config;
    private long lastRegularWriteMillis;

    public PerformanceLogService(JavaPlugin plugin, MonitorConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void updateConfig(MonitorConfig config) {
        this.config = config;
    }

    public void logRegularStatus(ServerStats stats) {
        if (!config.isPerformanceLogEnabled() || !config.isWriteRegularStatusEnabled()) {
            return;
        }

        long now = System.currentTimeMillis();
        long intervalMillis = Duration.ofMinutes(config.getWriteIntervalMinutes()).toMillis();
        if (lastRegularWriteMillis != 0L && now - lastRegularWriteMillis < intervalMillis) {
            return;
        }

        lastRegularWriteMillis = now;
        StringJoiner line = new StringJoiner(" ");
        line.add("[" + LocalTime.now().format(TIME_FORMAT) + "]");
        line.add("TPS=" + FormatUtil.decimal(stats.tps1m(), 2));
        line.add("MSPT=" + FormatUtil.decimal(stats.mspt(), 1));
        if (config.isIncludeOnline()) {
            line.add("Online=" + stats.onlinePlayers() + "/" + stats.maxPlayers());
        }
        if (config.isIncludeMemory()) {
            line.add("RAM=" + FormatUtil.memory(stats.usedMemoryBytes(), stats.maxMemoryBytes()));
        }
        if (config.isIncludeChunks()) {
            line.add("Chunks=" + stats.loadedChunks());
        }
        if (config.isIncludeEntities()) {
            line.add("Entities=" + stats.entities());
        }
        if (config.isIncludeUptime()) {
            line.add("Uptime=" + FormatUtil.decimal(Duration.ofMillis(stats.uptimeMillis()).toMinutes(), 0) + "m");
        }
        appendLine(performanceLogPath(), line.toString());
    }

    public void logAlert(PerformanceAlert alert) {
        if (!config.isPerformanceLogEnabled()) {
            return;
        }

        ServerStats stats = alert.stats();
        StringJoiner line = new StringJoiner(" ");
        line.add("[" + LocalTime.now().format(TIME_FORMAT) + "]");
        line.add(alert.level().toString());
        line.add(alert.title() + ":");
        line.add("TPS=" + FormatUtil.decimal(stats.tps1m(), 2));
        if (config.isIncludeOnline()) {
            line.add("Online=" + stats.onlinePlayers());
        }
        if (config.isIncludeMemory()) {
            line.add("RAM=" + FormatUtil.memory(stats.usedMemoryBytes(), stats.maxMemoryBytes()));
        }
        if (config.isIncludeChunks()) {
            line.add("Chunks=" + stats.loadedChunks());
        }
        if (config.isIncludeEntities()) {
            line.add("Entities=" + stats.entities());
        }
        if (config.isIncludeUptime()) {
            line.add("Uptime=" + FormatUtil.decimal(Duration.ofMillis(stats.uptimeMillis()).toMinutes(), 0) + "m");
        }
        appendLine(alertsLogPath(), line.toString());
        appendDiagnostics(alert.diagnosticsReport());
    }

    public void close() {
        // Writes are immediate; no buffered resources to close.
    }

    private Path performanceLogPath() {
        return logPath("performance");
    }

    private Path alertsLogPath() {
        return logPath("alerts");
    }

    private Path logPath(String prefix) {
        String fileName = prefix + (config.isPerformanceLogSplitByDate() ? "-" + LocalDate.now().format(DATE_FORMAT) : "") + ".log";
        return safeLogDirectory().resolve(fileName);
    }

    private Path safeLogDirectory() {
        Path dataDirectory = plugin.getDataFolder().toPath().toAbsolutePath().normalize();
        String configuredFolder = config.getPerformanceLogFolder();
        if (configuredFolder == null || configuredFolder.isBlank()) {
            return dataDirectory.resolve("logs").normalize();
        }

        try {
            Path candidate = dataDirectory.resolve(Path.of(configuredFolder.trim())).normalize();
            if (candidate.startsWith(dataDirectory)) {
                return candidate;
            }
        } catch (InvalidPathException exception) {
            plugin.getLogger().warning("Performance log folder is invalid; using default logs folder.");
        }
        return dataDirectory.resolve("logs").normalize();
    }

    private void appendLine(Path path, String line) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> appendLineNow(path, line));
    }

    private void appendDiagnostics(LagDiagnosticsReport report) {
        if (report == null || report.isEmpty()) {
            return;
        }

        StringJoiner line = new StringJoiner(" ");
        line.add("[" + LocalTime.now().format(TIME_FORMAT) + "]");
        line.add("DIAGNOSTICS");
        line.add("ScannedChunks=" + report.scannedChunks());
        line.add("Entities=" + report.totalEntities());
        line.add("BlockEntities=" + report.totalBlockEntities());
        for (LagChunkDiagnostic chunk : report.topChunks()) {
            line.add("Chunk=" + chunk.worldName() + ":" + chunk.chunkX() + "," + chunk.chunkZ()
                    + " Score=" + chunk.score()
                    + " Nearby=" + (chunk.nearbyPlayers().isEmpty() ? "none" : String.join(",", chunk.nearbyPlayers()))
                    + " Top=" + formatTypeCounts(chunk));
        }
        appendLine(alertsLogPath(), line.toString());
    }

    private String formatTypeCounts(LagChunkDiagnostic chunk) {
        if (chunk.typeCounts().isEmpty()) {
            return "none";
        }
        StringJoiner joiner = new StringJoiner(",");
        chunk.typeCounts().forEach((type, count) -> joiner.add(type + "=" + count));
        return joiner.toString();
    }

    private void appendLineNow(Path path, String line) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to write performance log: " + exception.getClass().getSimpleName());
        }
    }
}
