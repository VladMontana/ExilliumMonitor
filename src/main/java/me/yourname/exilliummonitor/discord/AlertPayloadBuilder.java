package me.yourname.exilliummonitor.discord;

import me.yourname.exilliummonitor.config.MonitorConfig;
import me.yourname.exilliummonitor.model.AlertLevel;
import me.yourname.exilliummonitor.model.PerformanceAlert;
import me.yourname.exilliummonitor.model.ServerStats;
import me.yourname.exilliummonitor.util.FormatUtil;
import me.yourname.exilliummonitor.util.MentionBuilder;
import me.yourname.exilliummonitor.util.TimeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public final class AlertPayloadBuilder {
    private final MentionBuilder mentionBuilder;
    private MonitorConfig config;

    public AlertPayloadBuilder(MonitorConfig config, MentionBuilder mentionBuilder) {
        this.config = config;
        this.mentionBuilder = mentionBuilder;
    }

    public void updateConfig(MonitorConfig config) {
        this.config = config;
    }

    public String build(PerformanceAlert alert) {
        if (config.isDiscordUseEmbeds()) {
            return buildEmbedPayload(alert);
        }
        return buildPlainPayload(alert);
    }

    private String buildPlainPayload(PerformanceAlert alert) {
        String content = alert.title() + "\n\n" + alert.message() + "\n\n" + formatStats(alert.stats());
        String mentions = mentions(alert);
        if (!mentions.isBlank()) {
            content += "\n\n" + mentions;
        }
        return "{"
                + jsonField("username", config.getDiscordUsername()) + ","
                + optionalAvatarField()
                + jsonField("content", content)
                + "}";
    }

    private String buildEmbedPayload(PerformanceAlert alert) {
        ServerStats stats = alert.stats();
        String mentions = mentions(alert);
        String color = Integer.toString(color(alert.level()));
        String content = mentions.isBlank() ? "" : mentions;
        return "{"
                + jsonField("username", config.getDiscordUsername()) + ","
                + optionalAvatarField()
                + jsonField("content", content) + ","
                + "\"embeds\":[{"
                + jsonField("title", alert.title()) + ","
                + jsonField("description", alert.message()) + ","
                + "\"color\":" + color + ","
                + "\"fields\":[" + String.join(",", fields(stats)) + "],"
                + "\"timestamp\":\"" + escape(alert.timestamp().toString()) + "\""
                + "}]"
                + "}";
    }

    private String formatStats(ServerStats stats) {
        StringJoiner joiner = new StringJoiner(System.lineSeparator());
        joiner.add("TPS: " + FormatUtil.decimal(stats.tps1m(), 2)
                + " / " + FormatUtil.decimal(stats.tps5m(), 2)
                + " / " + FormatUtil.decimal(stats.tps15m(), 2));
        joiner.add("MSPT: " + FormatUtil.decimal(stats.mspt(), 1) + " ms");
        if (config.isIncludeMemory()) {
            joiner.add("RAM: " + FormatUtil.memory(stats.usedMemoryBytes(), stats.maxMemoryBytes())
                    + " (" + FormatUtil.decimal(stats.usedMemoryPercent(), 0) + "%)");
        }
        if (config.isIncludeOnline()) {
            joiner.add("Online: " + stats.onlinePlayers() + "/" + stats.maxPlayers());
        }
        if (config.isIncludeChunks()) {
            joiner.add("Chunks: " + stats.loadedChunks());
        }
        if (config.isIncludeEntities()) {
            joiner.add("Entities: " + stats.entities());
        }
        if (config.isIncludeUptime()) {
            joiner.add("Uptime: " + TimeUtil.formatDuration(stats.uptimeMillis()));
        }
        return joiner.toString();
    }

    private List<String> fields(ServerStats stats) {
        List<String> fields = new ArrayList<>();
        fields.add(field("TPS", FormatUtil.decimal(stats.tps1m(), 2) + " / " + FormatUtil.decimal(stats.tps5m(), 2) + " / " + FormatUtil.decimal(stats.tps15m(), 2), true));
        fields.add(field("MSPT", FormatUtil.decimal(stats.mspt(), 1) + " ms", true));
        if (config.isIncludeMemory()) {
            fields.add(field("RAM", FormatUtil.memory(stats.usedMemoryBytes(), stats.maxMemoryBytes()) + " (" + FormatUtil.decimal(stats.usedMemoryPercent(), 0) + "%)", true));
        }
        if (config.isIncludeOnline()) {
            fields.add(field("Online", stats.onlinePlayers() + "/" + stats.maxPlayers(), true));
        }
        if (config.isIncludeChunks()) {
            fields.add(field("Chunks", Integer.toString(stats.loadedChunks()), true));
        }
        if (config.isIncludeEntities()) {
            fields.add(field("Entities", Integer.toString(stats.entities()), true));
        }
        if (config.isIncludeUptime()) {
            fields.add(field("Uptime", TimeUtil.formatDuration(stats.uptimeMillis()), true));
        }
        return fields;
    }

    private String mentions(PerformanceAlert alert) {
        if (!alert.shouldPing()) {
            return "";
        }
        return mentionBuilder.buildMentions(config.getPingRoleIds(), config.getPingUserIds());
    }

    private String field(String name, String value, boolean inline) {
        return "{"
                + jsonField("name", name) + ","
                + jsonField("value", value) + ","
                + "\"inline\":" + inline
                + "}";
    }

    private String optionalAvatarField() {
        String avatarUrl = config.getDiscordAvatarUrl();
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return "";
        }
        return jsonField("avatar_url", avatarUrl) + ",";
    }

    private String jsonField(String name, String value) {
        return "\"" + name + "\":\"" + escape(value) + "\"";
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (character < 0x20) {
                        builder.append(String.format("\\u%04x", (int) character));
                    } else {
                        builder.append(character);
                    }
                }
            }
        }
        return builder.toString();
    }

    private int color(AlertLevel level) {
        return switch (level) {
            case CRITICAL -> 0xD93025;
            case WARNING -> 0xF9AB00;
            case INFO -> 0x1A73E8;
        };
    }
}
