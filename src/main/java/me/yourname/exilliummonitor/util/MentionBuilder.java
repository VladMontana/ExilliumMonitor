package me.yourname.exilliummonitor.util;

import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Pattern;

public final class MentionBuilder {
    private static final Pattern DISCORD_ID = Pattern.compile("\\d{16,25}");

    public String buildMentions(List<String> roleIds, List<String> userIds) {
        StringJoiner joiner = new StringJoiner(" ");
        for (String roleId : roleIds) {
            String normalized = normalizeId(roleId);
            if (normalized != null) {
                joiner.add("<@&" + normalized + ">");
            }
        }
        for (String userId : userIds) {
            String normalized = normalizeId(userId);
            if (normalized != null) {
                joiner.add("<@" + normalized + ">");
            }
        }
        return joiner.toString();
    }

    private String normalizeId(String id) {
        if (id == null) {
            return null;
        }
        String trimmed = id.trim();
        return DISCORD_ID.matcher(trimmed).matches() ? trimmed : null;
    }
}
