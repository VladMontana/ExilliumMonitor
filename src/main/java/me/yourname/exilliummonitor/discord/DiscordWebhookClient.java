package me.yourname.exilliummonitor.discord;

import me.yourname.exilliummonitor.config.MonitorConfig;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public final class DiscordWebhookClient {
    private static final String PLACEHOLDER_WEBHOOK = "PASTE_ALERTS_WEBHOOK_HERE";

    private final JavaPlugin plugin;
    private final HttpClient httpClient;
    private MonitorConfig config;

    public DiscordWebhookClient(JavaPlugin plugin, MonitorConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void updateConfig(MonitorConfig config) {
        this.config = config;
    }

    public CompletableFuture<Void> send(String payloadJson) {
        if (!config.isDiscordEnabled()) {
            return CompletableFuture.completedFuture(null);
        }

        String webhookUrl = config.getDiscordWebhookUrl();
        if (isWebhookMissing(webhookUrl)) {
            if (config.isDebugEnabled()) {
                plugin.getLogger().info("Discord webhook is not configured; skipping alert.");
            }
            return CompletableFuture.completedFuture(null);
        }

        URI uri;
        try {
            uri = URI.create(webhookUrl.trim());
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Discord webhook URL is invalid; skipping alert.");
            return CompletableFuture.completedFuture(null);
        }

        if (!isSupportedWebhookUri(uri)) {
            plugin.getLogger().warning("Discord webhook URL is invalid; skipping alert.");
            return CompletableFuture.completedFuture(null);
        }

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payloadJson))
                    .build();
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Discord webhook URL is invalid; skipping alert.");
            return CompletableFuture.completedFuture(null);
        }

        try {
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(response -> {
                        int statusCode = response.statusCode();
                        if (statusCode < 200 || statusCode >= 300) {
                            plugin.getLogger().warning("Discord webhook returned HTTP " + statusCode + ".");
                        }
                    })
                    .exceptionally(throwable -> {
                        plugin.getLogger().warning("Failed to send Discord webhook alert: " + throwable.getClass().getSimpleName());
                        return null;
                    });
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Failed to queue Discord webhook alert: " + exception.getClass().getSimpleName());
            return CompletableFuture.completedFuture(null);
        }
    }

    public void shutdown() {
        // HttpClient has no explicit close method on Java 21.
    }

    private boolean isWebhookMissing(String webhookUrl) {
        return webhookUrl == null
                || webhookUrl.isBlank()
                || PLACEHOLDER_WEBHOOK.equalsIgnoreCase(webhookUrl.trim());
    }

    private boolean isSupportedWebhookUri(URI uri) {
        String scheme = uri.getScheme();
        return scheme != null
                && ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))
                && uri.getHost() != null
                && !uri.getHost().isBlank();
    }
}
