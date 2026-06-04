# ExilliumMonitor

ExilliumMonitor - отдельный Minecraft Paper-плагин для мониторинга производительности сервера и отправки предупреждений в Discord, когда сервер начинает лагать или потребление RAM становится опасным.

В MVP плагин работает только как безопасный мониторинг: предупреждает, пишет логи и информирует администрацию. Он не удаляет мобов, не удаляет предметы, не перезапускает сервер, не выполняет авто-очистку, не использует Discord-бота и не вмешивается в игровой процесс.

## Что отслеживает плагин

ExilliumMonitor собирает такие метрики:

- TPS: значения за 1, 5 и 15 минут из Paper.
- Примерный MSPT: рассчитывается по текущему TPS.
- RAM: использованная память, максимальная память и процент использования.
- Онлайн: текущий онлайн и максимальное количество игроков.
- Загруженные чанки: суммарно по всем загруженным мирам.
- Сущности: суммарно по всем загруженным мирам.
- Uptime: время работы JVM сервера.

Плагин отправляет alerts, если:

- TPS упал ниже warning или critical порога.
- RAM поднялась выше warning или critical порога.

Также плагин может вести одно ежедневное Discord status-сообщение. Он создает такое сообщение один раз в день и в течение дня редактирует это же сообщение, обновляя TPS, RAM, онлайн, чанки, сущности и uptime.

Логика по умолчанию:

```text
TPS <= 17.0  -> WARNING alert
TPS <= 10.0  -> CRITICAL alert
RAM >= 85%   -> WARNING alert
RAM >= 95%   -> CRITICAL alert
```

CRITICAL alerts могут пинговать Discord-роли или пользователей. WARNING alerts по умолчанию никого не пингуют.

## Требования

- Java 21+
- Paper 1.21.x
- Maven

## Сборка

```bash
mvn clean package
```

Готовый jar появится здесь:

```text
target/ExilliumMonitor-1.0.1.jar
```

Скопируй jar в папку `plugins/` Paper-сервера и перезапусти сервер.

## Команды

Основная команда:

```text
/exilliummonitor <status|reload|test>
```

Алиасы:

```text
/emonitor
/emon
```

Подкоманды:

```text
/exilliummonitor status
/exilliummonitor reload
/exilliummonitor test
/exilliummonitor test ping
/exilliummonitor test mentions
```

- `status`: показывает TPS, MSPT, RAM, онлайн, загруженные чанки, сущности и uptime.
- `reload`: перезагружает `config.yml` и обновляет thresholds, Discord-настройки, ping IDs и интервал мониторинга.
- `test`: отправляет тестовый Discord alert и пишет запись в alert log. По умолчанию роли и пользователи не пингуются.
- `test ping` или `test mentions`: отправляет тестовый alert с настроенными role/user mentions, чтобы администратор мог явно проверить ping-настройки.

Permissions:

```text
exilliummonitor.admin
exilliummonitor.status
exilliummonitor.reload
exilliummonitor.test
```

`exilliummonitor.admin` дает доступ ко всем подкомандам.

## Файл конфигурации

После первого запуска сервера редактируй:

```text
plugins/ExilliumMonitor/config.yml
```

После изменения конфига выполни:

```text
/exilliummonitor reload
```

или перезапусти сервер.

## Настройка мониторинга

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

Параметры:

- `monitoring.enabled`: включает или выключает весь мониторинг.
- `monitoring.tps.enabled`: включает проверки TPS.
- `check-interval-seconds`: как часто запускается мониторинг.
- `warning-threshold`: WARNING alert, если TPS меньше или равен этому значению.
- `critical-threshold`: CRITICAL alert, если TPS меньше или равен этому значению.
- `alert-cooldown-seconds`: защита от спама alerts и повторных ping. Cooldown от WARNING не блокирует последующий CRITICAL alert, но повторные CRITICAL alerts все равно проходят через cooldown.
- `monitoring.memory.enabled`: включает проверки RAM.
- `warning-used-percent`: WARNING alert, если RAM достигла этого процента.
- `critical-used-percent`: CRITICAL alert, если RAM достигла этого процента.
- `monitoring.stats.*`: настройки включения опциональных метрик в `/exilliummonitor status`, Discord alert payloads, performance logs и alert logs.

Рекомендуемые значения по умолчанию:

```yaml
warning-threshold: 17.0
critical-threshold: 10.0
alert-cooldown-seconds: 300
warning-used-percent: 85
critical-used-percent: 95
```

## Lag Diagnostics Configuration

```yaml
diagnostics:
  enabled: true
  run-on-warning: true
  run-on-critical: true
  top-chunks: 5
  max-scanned-chunks: 300
  nearby-player-radius-blocks: 128
  include-block-entities: true
  include-entity-breakdown: true
```

When enabled, WARNING and CRITICAL alerts include a `Possible lag sources` section. This is a safe heuristic report, not an exact CPU profiler: ExilliumMonitor scans loaded chunks, scores entity and block-entity counts, and shows nearby players for the most suspicious chunks.

Settings:

- `enabled`: enables lag diagnostics for alerts.
- `run-on-warning`: includes diagnostics in WARNING alerts.
- `run-on-critical`: includes diagnostics in CRITICAL alerts.
- `top-chunks`: maximum suspicious chunks shown in Discord and alert logs.
- `max-scanned-chunks`: maximum loaded chunks scanned per alert to avoid heavy work.
- `nearby-player-radius-blocks`: radius used to list nearby players. Nearby players are an attribution hint, not proof of ownership.
- `include-block-entities`: counts block entities such as hoppers, spawners, containers, furnaces, droppers, dispensers, and crafters.
- `include-entity-breakdown`: includes top entity types such as minecarts, dropped items, villagers, mobs, and animals.

## Настройка Discord

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

  daily-status:
    enabled: true
    update-interval-minutes: 5
    title: "ExilliumMonitor Daily Status"
```

Параметры:

- `discord.enabled`: включает или выключает отправку alerts в Discord.
- `webhook-url`: URL Discord webhook нужного канала.
- `username`: имя webhook-сообщений.
- `avatar-url`: необязательная ссылка на аватар webhook.
- `use-embeds`: включает красивые embed-сообщения.

Один и тот же webhook используется и для alert-сообщений, и для ежедневного status-сообщения.

### webhook-url

Замени placeholder:

```yaml
webhook-url: "PASTE_ALERTS_WEBHOOK_HERE"
```

на реальный Discord webhook URL:

```yaml
webhook-url: "https://discord.com/api/webhooks/1234567890/xxxxxxxx"
```

Webhook URL - это секрет. Не публикуй его в GitHub, публичных скриншотах, чатах или примерах конфигов. Любой человек с этим URL сможет отправлять сообщения через webhook.

Если webhook пустой, неправильный или оставлен как placeholder, плагин просто пропустит отправку в Discord и не уронит сервер.

### username

Имя webhook-сообщений:

```yaml
username: "Exillium Monitor"
```

### avatar-url

Необязательный аватар webhook:

```yaml
avatar-url: ""
```

Оставь пустым, если нужен стандартный аватар Discord webhook.

### use-embeds

Рекомендуется оставить включенным:

```yaml
use-embeds: true
```

Когда включено, alerts отправляются как Discord embeds с полями TPS, MSPT, RAM, онлайн, чанки, сущности и uptime. Поля, выключенные через `monitoring.stats.include-*`, не добавляются.

## Настройка Discord ping

```yaml
ping:
  enabled: true
  ping-on-warning: false
  ping-on-critical: true
  role-ids:
    - "123456789012345678"
  user-ids: []
```

Параметры:

- `enabled`: включает или выключает все Discord ping.
- `ping-on-warning`: пингует роли/пользователей при WARNING alerts, если стоит `true`.
- `ping-on-critical`: пингует роли/пользователей при CRITICAL alerts, если стоит `true`.
- `role-ids`: Discord role IDs для ping.
- `user-ids`: Discord user IDs для ping.

Рекомендуемая настройка:

```yaml
ping-on-warning: false
ping-on-critical: true
```

Так модераторов не будет дергать по warning-событиям, но при критической проблеме они получат ping.

### role-ids

Указывай чистые Discord role IDs без `<@&` и `>`.

Правильно:

```yaml
role-ids:
  - "987654321098765432"
```

Если роли пинговать не нужно:

```yaml
role-ids: []
```

### user-ids

Указывай чистые Discord user IDs без `<@` и `>`.

Правильно:

```yaml
user-ids:
  - "123456789012345678"
```

Если пользователей пинговать не нужно:

```yaml
user-ids: []
```

Пустые или неправильные ID игнорируются.

## Ежедневный Discord status

```yaml
daily-status:
  enabled: true
  update-interval-minutes: 5
  title: "ExilliumMonitor Daily Status"
```

Когда включено, ExilliumMonitor создает одно Discord status-сообщение на каждый локальный день сервера и редактирует это же сообщение каждые `update-interval-minutes`. Так в Discord остается живой status-панелью без спама новыми сообщениями.

Параметры:

- `enabled`: включает ежедневное редактируемое status-сообщение.
- `update-interval-minutes`: как часто редактировать существующее status-сообщение. По умолчанию `5`.
- `title`: заголовок ежедневного status-сообщения.

ID сообщения и дата сохраняются здесь:

```text
plugins/ExilliumMonitor/daily-status.yml
```

Если сохраненный message ID отсутствует, дата сменилась или Discord отклонил редактирование, плагин создаст новое daily status-сообщение. Daily status никогда не пингует роли или пользователей. Проблемы TPS/RAM по-прежнему создают отдельные alert-сообщения, и именно эти alerts используют настройки `discord.ping.*`.

Discord webhook должен иметь возможность создавать и редактировать свои webhook-сообщения. ExilliumMonitor использует Discord Webhook API напрямую и не использует JDA или Discord Bot.

## Настройка локальных логов

```yaml
performance-log:
  enabled: true
  folder: "logs"
  split-by-date: true
  write-regular-status: true
  write-interval-minutes: 5
  keep-days: 30
```

Параметры:

- `enabled`: включает локальные performance и alert logs.
- `folder`: папка логов внутри `plugins/ExilliumMonitor/`. Относительные пути считаются от папки данных плагина.
- `split-by-date`: пишет файлы по датам, например `performance-YYYY-MM-DD.log`.
- `write-regular-status`: пишет регулярные snapshots производительности.
- `write-interval-minutes`: минимальный интервал между регулярными log-записями.
- `keep-days`: зарезервировано под retention policy; будущие версии могут использовать это для удаления старых логов.

Логи пишутся сюда:

```text
plugins/ExilliumMonitor/logs/
```

Папка логов ограничена директорией данных плагина. Абсолютные пути и path traversal значения вроде `..` не используются для записи вне `plugins/ExilliumMonitor/`; при неправильном значении плагин откатывается к `logs`.

Примеры файлов:

```text
performance-YYYY-MM-DD.log
alerts-YYYY-MM-DD.log
```

Пример performance log:

```text
[21:40:00] TPS=19.92 MSPT=50.2 Online=12/100 RAM=4.1GB / 8.0GB Chunks=5120 Entities=1300
```

Пример alert log:

```text
[21:46:00] WARNING TPS Warning: TPS=15.40 Online=24 RAM=6.8GB / 8.0GB
```

Если локальные логи не записались, Discord alert всё равно попробует отправиться. Если Discord недоступен, локальные логи всё равно попробуют записаться.

## Отображение команды status

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

Эти параметры управляют выводом команды:

```text
/exilliummonitor status
```

`display.status-command.*` управляет видимостью строк в status-команде. `monitoring.stats.include-*` управляет включением опциональных метрик в status output, Discord payloads и logs.

## Debug

```yaml
debug:
  enabled: false
```

Для обычного использования оставляй `false`. Включай только для диагностики поведения плагина.

## Безопасность и ограничения MVP

- Discord messages отправляются асинхронно через `HttpClient.sendAsync`.
- Полный Discord webhook URL не логируется.
- Cooldown защищает от повторных moderator ping.
- В MVP нет JDA и Discord Bot.
- В MVP нет SQLite и MySQL.
- В MVP нет automatic cleanup, удаления мобов, удаления предметов и automatic restarts.
