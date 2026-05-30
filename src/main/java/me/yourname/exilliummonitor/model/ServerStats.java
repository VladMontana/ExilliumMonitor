package me.yourname.exilliummonitor.model;

import java.time.Instant;

public record ServerStats(
        double tps1m,
        double tps5m,
        double tps15m,
        double mspt,
        int onlinePlayers,
        int maxPlayers,
        long usedMemoryBytes,
        long maxMemoryBytes,
        int loadedChunks,
        int entities,
        long uptimeMillis,
        Instant timestamp
) {
    public double usedMemoryPercent() {
        if (maxMemoryBytes <= 0) {
            return 0.0D;
        }
        return (usedMemoryBytes * 100.0D) / maxMemoryBytes;
    }
}
