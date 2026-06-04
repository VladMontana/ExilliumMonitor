package me.yourname.exilliummonitor.model;

import java.util.List;

public record LagDiagnosticsReport(
        int scannedChunks,
        int totalEntities,
        int totalBlockEntities,
        List<LagChunkDiagnostic> topChunks
) {
    public boolean isEmpty() {
        return scannedChunks <= 0 || topChunks.isEmpty();
    }
}
