package me.yourname.exilliummonitor.service;

import me.yourname.exilliummonitor.config.MonitorConfig;
import me.yourname.exilliummonitor.model.AlertLevel;
import me.yourname.exilliummonitor.model.LagChunkDiagnostic;
import me.yourname.exilliummonitor.model.LagDiagnosticsReport;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class LagDiagnosticsService {
    private MonitorConfig config;

    public LagDiagnosticsService(MonitorConfig config) {
        this.config = config;
    }

    public void updateConfig(MonitorConfig config) {
        this.config = config;
    }

    public LagDiagnosticsReport diagnose(AlertLevel level) {
        if (!shouldRun(level)) {
            return null;
        }

        int maxScannedChunks = config.getDiagnosticsMaxScannedChunks();
        List<Chunk> chunks = prioritizedLoadedChunks(maxScannedChunks);
        List<LagChunkDiagnostic> diagnostics = new ArrayList<>();
        int totalEntities = 0;
        int totalBlockEntities = 0;

        for (Chunk chunk : chunks) {
            LagChunkDiagnostic diagnostic = diagnoseChunk(chunk);
            totalEntities += diagnostic.entityCount();
            totalBlockEntities += diagnostic.blockEntityCount();
            if (diagnostic.score() > 0) {
                diagnostics.add(diagnostic);
            }
        }

        diagnostics.sort(Comparator.comparingInt(LagChunkDiagnostic::score).reversed());
        int topLimit = Math.min(config.getDiagnosticsTopChunks(), diagnostics.size());
        return new LagDiagnosticsReport(
                chunks.size(),
                totalEntities,
                totalBlockEntities,
                List.copyOf(diagnostics.subList(0, topLimit))
        );
    }

    private boolean shouldRun(AlertLevel level) {
        if (!config.isDiagnosticsEnabled()) {
            return false;
        }
        return switch (level) {
            case CRITICAL -> config.isDiagnosticsRunOnCritical();
            case WARNING -> config.isDiagnosticsRunOnWarning();
            case INFO -> false;
        };
    }

    private List<Chunk> prioritizedLoadedChunks(int limit) {
        Map<String, Chunk> chunks = new LinkedHashMap<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            addChunk(chunks, player.getLocation().getChunk(), limit);
            if (chunks.size() >= limit) {
                return List.copyOf(chunks.values());
            }
        }

        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                addChunk(chunks, chunk, limit);
                if (chunks.size() >= limit) {
                    return List.copyOf(chunks.values());
                }
            }
        }
        return List.copyOf(chunks.values());
    }

    private void addChunk(Map<String, Chunk> chunks, Chunk chunk, int limit) {
        if (chunks.size() >= limit) {
            return;
        }
        chunks.putIfAbsent(chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ(), chunk);
    }

    private LagChunkDiagnostic diagnoseChunk(Chunk chunk) {
        Map<String, Integer> typeCounts = new LinkedHashMap<>();
        int entityCount = 0;
        int blockEntityCount = 0;
        int score = 0;

        Entity[] entities = chunk.getEntities();
        entityCount = entities.length;
        for (Entity entity : entities) {
            String typeName = entity.getType().name();
            score += entityWeight(entity.getType());
            if (config.isDiagnosticsIncludeEntityBreakdown()) {
                addType(typeCounts, typeName);
            }
        }

        if (config.isDiagnosticsIncludeBlockEntities()) {
            BlockState[] blockStates = chunk.getTileEntities(false);
            blockEntityCount = blockStates.length;
            for (BlockState blockState : blockStates) {
                Material type = blockState.getType();
                score += blockEntityWeight(type);
                addType(typeCounts, type.name());
            }
        }

        int centerX = chunk.getX() * 16 + 8;
        int centerZ = chunk.getZ() * 16 + 8;
        return new LagChunkDiagnostic(
                chunk.getWorld().getName(),
                chunk.getX(),
                chunk.getZ(),
                centerX,
                centerZ,
                score,
                entityCount,
                blockEntityCount,
                topTypeCounts(typeCounts, 5),
                nearbyPlayers(chunk.getWorld(), centerX, centerZ)
        );
    }

    private int entityWeight(EntityType type) {
        String name = type.name();
        if (name.contains("MINECART") || name.contains("BOAT")) {
            return 4;
        }
        if (type == EntityType.ITEM || type == EntityType.EXPERIENCE_ORB) {
            return 2;
        }
        if (name.contains("VILLAGER") || name.contains("ZOMBIE") || name.contains("SKELETON")
                || name.contains("CREEPER") || name.contains("SPIDER") || name.contains("SLIME")
                || name.contains("COW") || name.contains("PIG") || name.contains("SHEEP")
                || name.contains("CHICKEN")) {
            return 2;
        }
        return 1;
    }

    private int blockEntityWeight(Material type) {
        String name = type.name();
        if ("HOPPER".equals(name)) {
            return 6;
        }
        if (name.contains("SPAWNER")) {
            return 8;
        }
        if (name.contains("CHEST") || name.equals("BARREL") || name.contains("FURNACE")
                || name.equals("DROPPER") || name.equals("DISPENSER") || name.equals("CRAFTER")) {
            return 1;
        }
        return 1;
    }

    private void addType(Map<String, Integer> typeCounts, String typeName) {
        typeCounts.merge(typeName.toUpperCase(Locale.ROOT), 1, Integer::sum);
    }

    private Map<String, Integer> topTypeCounts(Map<String, Integer> counts, int limit) {
        Map<String, Integer> topCounts = new LinkedHashMap<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .forEach(entry -> topCounts.put(entry.getKey(), entry.getValue()));
        return topCounts;
    }

    private List<String> nearbyPlayers(World world, int centerX, int centerZ) {
        int radius = config.getDiagnosticsNearbyPlayerRadiusBlocks();
        if (radius <= 0) {
            return List.of();
        }

        double radiusSquared = radius * (double) radius;
        Location center = new Location(world, centerX + 0.5D, 64.0D, centerZ + 0.5D);
        List<PlayerDistance> nearby = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().equals(world)) {
                continue;
            }
            Location location = player.getLocation();
            double dx = location.getX() - center.getX();
            double dz = location.getZ() - center.getZ();
            double distanceSquared = dx * dx + dz * dz;
            if (distanceSquared <= radiusSquared) {
                nearby.add(new PlayerDistance(player.getName(), distanceSquared));
            }
        }

        nearby.sort(Comparator.comparingDouble(PlayerDistance::distanceSquared));
        return nearby.stream()
                .limit(3)
                .map(PlayerDistance::playerName)
                .toList();
    }

    private record PlayerDistance(String playerName, double distanceSquared) {
    }
}
