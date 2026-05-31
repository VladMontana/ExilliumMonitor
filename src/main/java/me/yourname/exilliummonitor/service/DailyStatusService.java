package me.yourname.exilliummonitor.service;

import me.yourname.exilliummonitor.config.MonitorConfig;
import me.yourname.exilliummonitor.discord.DailyStatusPayloadBuilder;
import me.yourname.exilliummonitor.discord.DiscordWebhookClient;
import me.yourname.exilliummonitor.model.ServerStats;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;

public final class DailyStatusService {
    private final JavaPlugin plugin;
    private final DiscordWebhookClient discordWebhookClient;
    private final DailyStatusPayloadBuilder payloadBuilder;
    private final File stateFile;
    private MonitorConfig config;
    private String messageId;
    private LocalDate messageDate;
    private boolean requestInFlight;

    public DailyStatusService(
            JavaPlugin plugin,
            MonitorConfig config,
            DiscordWebhookClient discordWebhookClient,
            DailyStatusPayloadBuilder payloadBuilder
    ) {
        this.plugin = plugin;
        this.config = config;
        this.discordWebhookClient = discordWebhookClient;
        this.payloadBuilder = payloadBuilder;
        this.stateFile = new File(plugin.getDataFolder(), "daily-status.yml");
        loadState();
    }

    public void updateConfig(MonitorConfig config) {
        this.config = config;
        this.payloadBuilder.updateConfig(config);
    }

    public void updateStatus(ServerStats stats) {
        if (!config.isDiscordEnabled() || !config.isDailyStatusEnabled() || requestInFlight) {
            return;
        }

        String payload = payloadBuilder.build(stats);
        LocalDate today = LocalDate.now();
        requestInFlight = true;

        if (messageId == null || messageId.isBlank() || !today.equals(messageDate)) {
            createStatusMessage(payload, today);
            return;
        }

        discordWebhookClient.editMessage(messageId, payload)
                .thenAccept(edited -> {
                    if (edited) {
                        requestInFlight = false;
                        return;
                    }
                    createStatusMessage(payload, today);
                })
                .exceptionally(throwable -> {
                    plugin.getLogger().warning("Daily status update failed: " + throwable.getClass().getSimpleName());
                    requestInFlight = false;
                    return null;
                });
    }

    private void createStatusMessage(String payload, LocalDate date) {
        discordWebhookClient.sendAndReturnMessageId(payload)
                .thenAccept(createdMessageId -> {
                    createdMessageId.ifPresent(id -> {
                        messageId = id;
                        messageDate = date;
                        saveState();
                    });
                    requestInFlight = false;
                })
                .exceptionally(throwable -> {
                    plugin.getLogger().warning("Daily status message creation failed: " + throwable.getClass().getSimpleName());
                    requestInFlight = false;
                    return null;
                });
    }

    private void loadState() {
        if (!stateFile.isFile()) {
            return;
        }

        YamlConfiguration state = YamlConfiguration.loadConfiguration(stateFile);
        messageId = state.getString("message-id", "");
        String date = state.getString("date", "");
        try {
            messageDate = date == null || date.isBlank() ? null : LocalDate.parse(date);
        } catch (RuntimeException exception) {
            messageDate = null;
        }
    }

    private void saveState() {
        YamlConfiguration state = new YamlConfiguration();
        state.set("message-id", messageId);
        state.set("date", messageDate == null ? "" : messageDate.toString());
        try {
            state.save(stateFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save daily status state: " + exception.getClass().getSimpleName());
        }
    }
}
