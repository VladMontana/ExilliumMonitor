# ExilliumMonitor

Russian documentation: [README_RU.md](README_RU.md)

ExilliumMonitor is a standalone Minecraft Paper plugin for monitoring server performance and sending Discord alerts when the server starts lagging or memory usage becomes dangerous.

The plugin is intentionally passive in the MVP: it warns, logs, and informs administrators. It does not remove mobs, remove items, restart the server, run cleanup actions, use a Discord bot, or modify gameplay.

## What It Monitors

ExilliumMonitor collects these server metrics:

- TPS: 1 minute, 5 minute, and 15 minute values from Paper.
- Approximate MSPT: calculated from current TPS.
- RAM usage: used memory, max memory, and used percent.
- Online players: current and max player count.
- Loaded chunks: counted across all loaded worlds.
- Entities: counted across all loaded worlds.
- Uptime: server JVM uptime.

The plugin can send alerts when:

- TPS is below the warning or critical threshold.
- RAM usage is above the warning or critical threshold.

Default alert rules:

```text
TPS <= 17.0  -> WARNING alert
TPS <= 10.0  -> CRITICAL alert
RAM >= 85%   -> WARNING alert
RAM >= 95%   -> CRITICAL alert
```

Critical alerts can ping Discord roles or users. Warning alerts do not ping by default.

## Requirements

- Java 21+
- Paper 1.21.x
- Maven

## Build

```bash
mvn clean package
```

The built plugin jar is created at:

```text
target/ExilliumMonitor-1.0.0.jar
```

Copy the jar into your Paper server `plugins/` folder and restart the server.

## Commands

Main command:

```text
/exilliummonitor <status|reload|test>
```

Aliases:

```text
/emonitor
/emon
```

Subcommands:

```text
/exilliummonitor status
/exilliummonitor reload
/exilliummonitor test
/exilliummonitor test ping
/exilliummonitor test mentions
```

- `status`: shows TPS, MSPT, RAM, online players, loaded chunks, entities, and uptime.
- `reload`: reloads `config.yml` and updates thresholds, Discord settings, ping IDs, and monitoring interval.
- `test`: sends a test Discord alert and writes a test alert log entry. It does not ping roles or users by default.
- `test ping` or `test mentions`: sends the same test alert with configured role/user mentions, so admins can verify ping setup intentionally.

Permissions:

```text
exilliummonitor.admin
exilliummonitor.status
exilliummonitor.reload
exilliummonitor.test
```

`exilliummonitor.admin` grants access to all subcommands.

## Configuration File

After the first server start, edit:

```text
plugins/ExilliumMonitor/config.yml
```

After changing the config, run:

```text
/exilliummonitor reload
```

or restart the server.

## Monitoring Configuration

```yaml
monitoring:
  enabled: true

  tps:
    enabled: true
    check-interval-seconds: 60
    warning-threshold: 17.0
    critical-threshold: 10.0
    alert-cooldown-seconds: 300

  memory:
    enabled: true
    warning-used-percent: 85
    critical-used-percent: 95

  stats:
    include-online: true
    include-memory: true
    include-chunks: true
    include-entities: true
    include-uptime: true
```

Settings:

- `monitoring.enabled`: enables or disables all monitoring.
- `monitoring.tps.enabled`: enables TPS checks.
- `check-interval-seconds`: how often the scheduled monitor runs.
- `warning-threshold`: sends a WARNING alert when TPS is less than or equal to this value.
- `critical-threshold`: sends a CRITICAL alert when TPS is less than or equal to this value.
- `alert-cooldown-seconds`: prevents repeated Discord spam while the same problem continues. A WARNING cooldown does not suppress a later CRITICAL alert, but repeated CRITICAL alerts are still cooled down.
- `monitoring.memory.enabled`: enables RAM checks.
- `warning-used-percent`: sends a WARNING alert when RAM usage reaches this percent.
- `critical-used-percent`: sends a CRITICAL alert when RAM usage reaches this percent.
- `monitoring.stats.*`: controls which optional stats are included in `/exilliummonitor status`, Discord alert payloads, performance logs, and alert logs.

Recommended defaults:

```yaml
warning-threshold: 17.0
critical-threshold: 10.0
alert-cooldown-seconds: 300
warning-used-percent: 85
critical-used-percent: 95
```

## Discord Configuration

```yaml
discord:
  enabled: true
  webhook-url: "PASTE_ALERTS_WEBHOOK_HERE"
  username: "Exillium Monitor"
  avatar-url: ""
  use-embeds: true

  ping:
    enabled: true
    ping-on-warning: false
    ping-on-critical: true
    role-ids:
      - "123456789012345678"
    user-ids: []
```

Settings:

- `discord.enabled`: enables or disables Discord alert sending.
- `webhook-url`: Discord channel webhook URL.
- `username`: display name used by webhook messages.
- `avatar-url`: optional image URL for the webhook avatar.
- `use-embeds`: sends cleaner embed-style alerts when enabled.

### webhook-url

Replace the placeholder:

```yaml
webhook-url: "PASTE_ALERTS_WEBHOOK_HERE"
```

with a real Discord webhook URL:

```yaml
webhook-url: "https://discord.com/api/webhooks/1234567890/xxxxxxxx"
```

Keep the webhook URL private. Do not publish it in GitHub, public screenshots, chat, or shared config examples. Anyone with this URL can send messages through the webhook.

If the webhook is empty, invalid, or still set to the placeholder, the plugin skips Discord sending and does not crash the server.

### username

Webhook display name:

```yaml
username: "Exillium Monitor"
```

### avatar-url

Optional webhook avatar:

```yaml
avatar-url: ""
```

Leave it empty to use Discord's default webhook avatar.

### use-embeds

Recommended:

```yaml
use-embeds: true
```

When enabled, alerts are sent as Discord embeds with fields for TPS, MSPT, RAM, online players, chunks, entities, and uptime. Fields disabled through `monitoring.stats.include-*` are omitted.

## Discord Ping Configuration

```yaml
ping:
  enabled: true
  ping-on-warning: false
  ping-on-critical: true
  role-ids:
    - "123456789012345678"
  user-ids: []
```

Settings:

- `enabled`: enables or disables all configured Discord pings.
- `ping-on-warning`: pings roles/users for WARNING alerts when set to `true`.
- `ping-on-critical`: pings roles/users for CRITICAL alerts when set to `true`.
- `role-ids`: Discord role IDs to ping.
- `user-ids`: Discord user IDs to ping.

Recommended default:

```yaml
ping-on-warning: false
ping-on-critical: true
```

This avoids bothering moderators for warning-level alerts while still pinging them for critical issues.

### role-ids

Use raw Discord role IDs without `<@&` and `>`.

Correct:

```yaml
role-ids:
  - "987654321098765432"
```

Not needed:

```yaml
role-ids: []
```

### user-ids

Use raw Discord user IDs without `<@` and `>`.

Correct:

```yaml
user-ids:
  - "123456789012345678"
```

Not needed:

```yaml
user-ids: []
```

Blank or invalid IDs are ignored.

## Performance Log Configuration

```yaml
performance-log:
  enabled: true
  folder: "logs"
  split-by-date: true
  write-regular-status: true
  write-interval-minutes: 5
  keep-days: 30
```

Settings:

- `enabled`: enables local performance and alert logs.
- `folder`: log folder inside `plugins/ExilliumMonitor/`. Relative paths are resolved under the plugin data folder.
- `split-by-date`: writes dated files such as `performance-YYYY-MM-DD.log`.
- `write-regular-status`: writes regular performance snapshots.
- `write-interval-minutes`: minimum interval between regular performance log entries.
- `keep-days`: reserved for retention policy; future versions may use it to remove old logs.

Log files are written under:

```text
plugins/ExilliumMonitor/logs/
```

The folder is constrained to the plugin data directory. Absolute paths or path traversal values such as `..` are not used to write outside `plugins/ExilliumMonitor/`; invalid values fall back to `logs`.

Example files:

```text
performance-YYYY-MM-DD.log
alerts-YYYY-MM-DD.log
```

Example performance line:

```text
[21:40:00] TPS=19.92 MSPT=50.2 Online=12/100 RAM=4.1GB / 8.0GB Chunks=5120 Entities=1300
```

Example alert line:

```text
[21:46:00] WARNING TPS Warning: TPS=15.40 Online=24 RAM=6.8GB / 8.0GB
```

If local logging fails, Discord alerts still try to send. If Discord sending fails, local logs still try to write.

## Status Command Display

```yaml
display:
  status-command:
    show-tps: true
    show-memory: true
    show-online: true
    show-chunks: true
    show-entities: true
    show-uptime: true
```

These settings control which sections are shown by:

```text
/exilliummonitor status
```

`display.status-command.*` controls status command visibility. `monitoring.stats.include-*` controls whether optional stats are included across status output, Discord payloads, and logs.

## Debug Configuration

```yaml
debug:
  enabled: false
```

Keep debug disabled for normal use. Enable it only when troubleshooting plugin behavior.

## Safety Notes

- Discord messages are sent asynchronously with `HttpClient.sendAsync`.
- The full Discord webhook URL is not logged.
- Alert cooldown prevents repeated moderator pings.
- The plugin does not use JDA or a Discord bot.
- The plugin does not use SQLite or MySQL in the MVP.
- The plugin does not perform automatic cleanup or automatic restarts.
