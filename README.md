# CoreCommands

Лёгкий плагин команд для Paper 1.21.11 и Java 21. Плагин не использует исходники EssentialsX.

## Сборка

```bash
./gradlew shadowJar
```

Готовый файл будет создан в `build/libs/CoreCommands-1.0.0.jar`. Скопируйте его в `plugins/` сервера Paper. Для экономических команд установите Vault и совместимый economy-плагин.

## Команды

| Команда | Назначение |
| --- | --- |
| `/balance [игрок]`, `/bal` | Баланс через Vault |
| `/pay <игрок> <сумма>` | Перевод денег |
| `/msg`, `/tell`, `/w`, `/reply`, `/r` | Личные сообщения |
| `/ban`, `/unban`, `/ipban`, `/mute`, `/unmute`, `/kick` | Наказания |
| `/tpa`, `/tpaccept`, `/tpdeny`, `/tp`, `/tphere` | Телепортация |
| `/spawn`, `/setspawn`, `/rtp` | Спавн и случайная телепортация |
| `/sethome`, `/home`, `/delhome`, `/homes` | Дома в SQLite |
| `/ignore <игрок>` | Игнор игрока в чате и личных сообщениях |
| `/tps` | TPS и MSPT |
| `/vanish` | Невидимость с уровнями времени |
| `/wipeplayer <игрок> [confirm <ключ>]` | Подтверждаемое удаление данных |
| `/corecommands reload` | Перезагрузка `config.yml` и `messages.yml` |

## Права

Основные права: `core.balance`, `core.balance.others`, `core.pay`, `core.msg`, `core.reply`, `core.tpa`, `core.tpaccept`, `core.tpdeny`, `core.tp`, `core.tphere`, `core.spawn`, `core.setspawn`, `core.rtp`, `core.sethome`, `core.home`, `core.delhome`, `core.homes`, `core.ignore`, `core.tps`, `core.wipeplayer`, `corecommands.reload`.

Уровни наказаний: `core.ban.helper`, `core.ban.moderator`, `core.ban.permanent`, аналогичные `core.ipban.*` и `core.mute.*`. Уровни невидимости: `core.vanish.short`, `core.vanish.long`, `core.vanish.unlimited`, просмотр невидимых: `core.vanish.see`. Обход кулдаунов: `core.bypass.cooldown`.

Все права, тексты, кулдауны, лимиты домов, сроки наказаний, параметры RTP и расположение базы вынесены в `config.yml` и `messages.yml`.