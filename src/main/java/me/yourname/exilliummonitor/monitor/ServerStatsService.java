package me.yourname.exilliummonitor.monitor;

import me.yourname.exilliummonitor.model.ServerStats;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.lang.management.ManagementFactory;
import java.time.Instant;

public final class ServerStatsService {
    private final long startTimeMillis;

    public ServerStatsService() {
        this.startTimeMillis = ManagementFactory.getRuntimeMXBean().getStartTime();
    }

    public ServerStats collect() {
        double[] tps = Bukkit.getServer().getTPS();
        double tps1m = tpsValue(tps, 0);
        double tps5m = tpsValue(tps, 1);
        double tps15m = tpsValue(tps, 2);

        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();

        int loadedChunks = 0;
        int entities = 0;
        for (World world : Bukkit.getWorlds()) {
            loadedChunks += world.getLoadedChunks().length;
            entities += world.getEntities().size();
        }

        return new ServerStats(
                tps1m,
                tps5m,
                tps15m,
                approximateMspt(tps1m),
                Bukkit.getOnlinePlayers().size(),
                Bukkit.getMaxPlayers(),
                usedMemory,
                maxMemory,
                loadedChunks,
                entities,
                System.currentTimeMillis() - startTimeMillis,
                Instant.now()
        );
    }

    private double tpsValue(double[] values, int index) {
        if (values == null || values.length <= index || values[index] <= 0.0D) {
            return 0.0D;
        }
        return Math.min(20.0D, values[index]);
    }

    private double approximateMspt(double tps) {
        if (tps <= 0.0D) {
            return 0.0D;
        }
        return 1000.0D / tps;
    }
}
