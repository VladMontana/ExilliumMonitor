package me.yourname.exilliummonitor.model;

import java.util.List;
import java.util.Map;

public record LagChunkDiagnostic(
        String worldName,
        int chunkX,
        int chunkZ,
        int centerBlockX,
        int centerBlockZ,
        int score,
        int entityCount,
        int blockEntityCount,
        Map<String, Integer> typeCounts,
        List<String> nearbyPlayers
) {
}
