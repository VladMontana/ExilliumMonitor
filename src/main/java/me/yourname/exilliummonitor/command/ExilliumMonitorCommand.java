package me.yourname.exilliummonitor.command;

import me.yourname.exilliummonitor.ExilliumMonitorPlugin;
import me.yourname.exilliummonitor.config.MonitorConfig;
import me.yourname.exilliummonitor.model.ServerStats;
import me.yourname.exilliummonitor.monitor.PerformanceAlertService;
import me.yourname.exilliummonitor.monitor.ServerStatsService;
import me.yourname.exilliummonitor.util.FormatUtil;
import me.yourname.exilliummonitor.util.TimeUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ExilliumMonitorCommand implements CommandExecutor, TabCompleter {
    private final ExilliumMonitorPlugin plugin;
    private final ServerStatsService serverStatsService;
    private final PerformanceAlertService performanceAlertService;

    public ExilliumMonitorCommand(
            ExilliumMonitorPlugin plugin,
            ServerStatsService serverStatsService,
            PerformanceAlertService performanceAlertService
    ) {
        this.plugin = plugin;
        this.serverStatsService = serverStatsService;
        this.performanceAlertService = performanceAlertService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "status" -> status(sender);
            case "reload" -> reload(sender);
            case "test" -> test(sender, args);
            default -> {
                sendUsage(sender, label);
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 2 && "test".equalsIgnoreCase(args[0])) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return List.of("ping", "mentions").stream()
                    .filter(option -> option.startsWith(prefix))
                    .toList();
        }
        if (args.length != 1) {
            return List.of();
        }

        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> suggestions = new ArrayList<>();
        for (String option : List.of("status", "reload", "test")) {
            if (option.startsWith(prefix)) {
                suggestions.add(option);
            }
        }
        return suggestions;
    }

    private boolean status(CommandSender sender) {
        if (!hasPermission(sender, "exilliummonitor.status")) {
            return true;
        }

        MonitorConfig config = plugin.getMonitorConfig();
        ServerStats stats = serverStatsService.collect();
        sender.sendMessage(ChatColor.GOLD + "ExilliumMonitor status:");
        if (config.isShowTps()) {
            sender.sendMessage(ChatColor.YELLOW + "TPS: " + ChatColor.WHITE
                    + FormatUtil.decimal(stats.tps1m(), 2) + " / "
                    + FormatUtil.decimal(stats.tps5m(), 2) + " / "
                    + FormatUtil.decimal(stats.tps15m(), 2));
            sender.sendMessage(ChatColor.YELLOW + "MSPT: " + ChatColor.WHITE + FormatUtil.decimal(stats.mspt(), 1) + " ms");
        }
        if (config.isShowMemory() && config.isIncludeMemory()) {
            sender.sendMessage(ChatColor.YELLOW + "RAM: " + ChatColor.WHITE
                    + FormatUtil.memory(stats.usedMemoryBytes(), stats.maxMemoryBytes())
                    + " (" + FormatUtil.decimal(stats.usedMemoryPercent(), 0) + "%)");
        }
        if (config.isShowOnline() && config.isIncludeOnline()) {
            sender.sendMessage(ChatColor.YELLOW + "Online: " + ChatColor.WHITE + stats.onlinePlayers() + "/" + stats.maxPlayers());
        }
        if (config.isShowChunks() && config.isIncludeChunks()) {
            sender.sendMessage(ChatColor.YELLOW + "Chunks: " + ChatColor.WHITE + stats.loadedChunks());
        }
        if (config.isShowEntities() && config.isIncludeEntities()) {
            sender.sendMessage(ChatColor.YELLOW + "Entities: " + ChatColor.WHITE + stats.entities());
        }
        if (config.isShowUptime() && config.isIncludeUptime()) {
            sender.sendMessage(ChatColor.YELLOW + "Uptime: " + ChatColor.WHITE + TimeUtil.formatDuration(stats.uptimeMillis()));
        }
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!hasPermission(sender, "exilliummonitor.reload")) {
            return true;
        }

        plugin.reloadMonitor();
        sender.sendMessage(ChatColor.GREEN + "ExilliumMonitor config reloaded.");
        return true;
    }

    private boolean test(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "exilliummonitor.test")) {
            return true;
        }

        boolean includeMentions = args.length >= 2
                && ("ping".equalsIgnoreCase(args[1]) || "mentions".equalsIgnoreCase(args[1]));
        ServerStats stats = serverStatsService.collect();
        performanceAlertService.sendTestAlert(stats, includeMentions);
        sender.sendMessage(ChatColor.GREEN + "ExilliumMonitor test alert queued"
                + (includeMentions ? " with configured mentions." : "."));
        return true;
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission) || sender.hasPermission("exilliummonitor.admin")) {
            return true;
        }
        sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
        return false;
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " <status|reload|test [ping]>");
    }
}
