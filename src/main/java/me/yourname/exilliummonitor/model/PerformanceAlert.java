package me.yourname.exilliummonitor.model;

import java.time.Instant;

public record PerformanceAlert(
        AlertLevel level,
        String title,
        String message,
        ServerStats stats,
        Instant timestamp,
        boolean shouldPing
) {
}
