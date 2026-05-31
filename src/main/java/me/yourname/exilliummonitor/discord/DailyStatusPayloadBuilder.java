package me.yourname.exilliummonitor.discord;

import me.yourname.exilliummonitor.config.MonitorConfig;
import me.yourname.exilliummonitor.model.ServerStats;
import me.yourname.exilliummonitor.util.FormatUtil;
import me.yourname.exilliummonitor.util.TimeUtil;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public final class DailyStatusPayloadBuilder {
    private MonitorConfig config;

    public DailyStatusPayloadBuilder(MonitorConfig config) {
        this.config = config;
    }

    public void updateConfig(MonitorConfig config) {
        this.config = config;
    }

    public String build(ServerStats stats) {
        if (config.isDiscordUseEmbeds()) {
            return buildEmbedPayload(stats);
        }
        return buildPlainPayload(stats);
    }

    private String buildPlainPayload(ServerStats stats) {
        String content = config.getDailyStatusTitle()
                + "\n\n" + formatStats(stats)
                + "\n\nLast update: " + Instant.now();
        return "{"
                + jsonField("username", config.getDiscordUsername()) + ","
                + optionalAvatarField()
                + jsonField("content", content)
                + "}";
    }

    private String buildEmbedPayload(ServerStats stats) {
        return "{"
                + jsonField("username", config.getDiscordUsername()) + ","
                + optionalAvatarField()
                + jsonField("content", "") + ","
                + "\"embeds\":[{"
                + jsonField("title", config.getDailyStatusTitle()) + ","
                + jsonField("description", "Live server status. Alerts are sent as separate messages.") + ","
                + "\"color\":3447003,"
                + "\"fields\":[" + String.join(",", fields(stats)) + "],"
                + "\"timestamp\":\"" + escape(Instant.now().toString()) + "\""
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
}
