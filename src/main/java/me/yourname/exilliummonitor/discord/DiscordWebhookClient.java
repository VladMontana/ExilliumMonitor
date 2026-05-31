package me.yourname.exilliummonitor.discord;

import me.yourname.exilliummonitor.config.MonitorConfig;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DiscordWebhookClient {
    private static final String PLACEHOLDER_WEBHOOK = "PASTE_ALERTS_WEBHOOK_HERE";
    private static final Pattern MESSAGE_ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");

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
        Optional<URI> uri = webhookUri();
        if (uri.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        HttpRequest request = buildRequest(uri.get(), "POST", payloadJson);
        if (request == null) {
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

    public CompletableFuture<Optional<String>> sendAndReturnMessageId(String payloadJson) {
        Optional<URI> uri = webhookUri();
        if (uri.isEmpty()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        URI waitUri = appendQuery(uri.get(), "wait=true");
        HttpRequest request = buildRequest(waitUri, "POST", payloadJson);
        if (request == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        try {
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        int statusCode = response.statusCode();
                        if (statusCode < 200 || statusCode >= 300) {
                            plugin.getLogger().warning("Discord webhook returned HTTP " + statusCode + " while creating status message.");
                            return Optional.<String>empty();
                        }
                        return extractMessageId(response.body());
                    })
                    .exceptionally(throwable -> {
                        plugin.getLogger().warning("Failed to create Discord status message: " + throwable.getClass().getSimpleName());
                        return Optional.empty();
                    });
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Failed to queue Discord status message: " + exception.getClass().getSimpleName());
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    public CompletableFuture<Boolean> editMessage(String messageId, String payloadJson) {
        if (messageId == null || messageId.isBlank()) {
            return CompletableFuture.completedFuture(false);
        }

        Optional<URI> uri = webhookUri();
        if (uri.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        URI editUri = appendPath(uri.get(), "/messages/" + messageId.trim());
        HttpRequest request = buildRequest(editUri, "PATCH", payloadJson);
        if (request == null) {
            return CompletableFuture.completedFuture(false);
        }

        try {
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenApply(response -> {
                        int statusCode = response.statusCode();
                        if (statusCode >= 200 && statusCode < 300) {
                            return true;
                        }
                        plugin.getLogger().warning("Discord webhook returned HTTP " + statusCode + " while editing status message.");
                        return false;
                    })
                    .exceptionally(throwable -> {
                        plugin.getLogger().warning("Failed to edit Discord status message: " + throwable.getClass().getSimpleName());
                        return false;
                    });
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Failed to queue Discord status edit: " + exception.getClass().getSimpleName());
            return CompletableFuture.completedFuture(false);
        }
    }

    public void shutdown() {
        // HttpClient has no explicit close method on Java 21.
    }

    private Optional<URI> webhookUri() {
        if (!config.isDiscordEnabled()) {
            return Optional.empty();
        }

        String webhookUrl = config.getDiscordWebhookUrl();
        if (isWebhookMissing(webhookUrl)) {
            if (config.isDebugEnabled()) {
                plugin.getLogger().info("Discord webhook is not configured; skipping alert.");
            }
            return Optional.empty();
        }

        URI uri;
        try {
            uri = URI.create(webhookUrl.trim());
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Discord webhook URL is invalid; skipping alert.");
            return Optional.empty();
        }

        if (!isSupportedWebhookUri(uri)) {
            plugin.getLogger().warning("Discord webhook URL is invalid; skipping alert.");
            return Optional.empty();
        }

        return Optional.of(uri);
    }

    private HttpRequest buildRequest(URI uri, String method, String payloadJson) {
        try {
            return HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(payloadJson))
                    .build();
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Discord webhook URL is invalid; skipping alert.");
            return null;
        }
    }

    private Optional<String> extractMessageId(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = MESSAGE_ID_PATTERN.matcher(responseBody);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        plugin.getLogger().warning("Discord webhook response did not include a message id.");
        return Optional.empty();
    }

    private URI appendQuery(URI uri, String query) {
        String existingQuery = uri.getRawQuery();
        String newQuery = existingQuery == null || existingQuery.isBlank()
                ? query
                : existingQuery + "&" + query;
        return URI.create(uri.toString().split("\\?", 2)[0] + "?" + newQuery);
    }

    private URI appendPath(URI uri, String suffix) {
        String base = uri.toString().split("\\?", 2)[0];
        return URI.create(base + suffix);
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
