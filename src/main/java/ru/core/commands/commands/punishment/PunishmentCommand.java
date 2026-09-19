package ru.core.commands.commands.punishment;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.core.commands.CoreCommandsPlugin;
import ru.core.commands.commands.BaseCommand;
import ru.core.commands.database.DatabaseManager;
import ru.core.commands.manager.PluginServices;
import ru.core.commands.util.CommandUtil;
import ru.core.commands.util.TimeParser;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

public final class PunishmentCommand extends BaseCommand {
    public enum Type { BAN, UNBAN, IPBAN, MUTE, UNMUTE, KICK }

    private final Type type;

    public PunishmentCommand(CoreCommandsPlugin plugin, PluginServices services, Type type) {
        super(plugin, services);
        this.type = type;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (type == Type.BAN || type == Type.IPBAN || type == Type.MUTE) {
            if (tier(sender, type) == null) {
                sender.sendMessage(text("no-permission").replace("{permission}", permission()));
                return true;
            }
        } else if (!CommandUtil.permission(sender, services.messages(), permission())) {
            return true;
        }
        if (type == Type.UNBAN || type == Type.UNMUTE) return remove(sender, args);
        if (args.length == 0 || (type == Type.KICK && args.length > 3)) {
            sender.sendMessage(services.messages().withPrefix("punishment-usage",
                    Map.of("command", label)));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(services.messages().withPrefix("unknown-player", Map.of("player", args[0])));
            return true;
        }
        Long duration = null;
        int reasonStart = 1;
        if (args.length > 1) {
            try {
                duration = TimeParser.parse(args[1]);
                reasonStart = 2;
            } catch (IllegalArgumentException ignored) {
                if (type == Type.KICK) reasonStart = 1;
                else if (args[1].matches("\\d+.*")) {
                    sender.sendMessage(text("invalid-time"));
                    return true;
                }
            }
        }
        if (type == Type.KICK) {
            target.kickPlayer(services.messages().color(args.length > 1
                    ? CommandUtil.join(args, 1) : services.messages().get("punishment-reason")));
            sender.sendMessage(text("punishment-kick"));
            return true;
        }
        Tier tier = tier(sender, type);
        if (tier == null || !allowed(tier.maxDuration(), duration)) {
            sender.sendMessage(text("punishment-denied-duration"));
            return true;
        }
        if (tier.cooldown() > 0 && sender instanceof Player player
                && !services.cooldowns().ready(player, "punish-" + type.name().toLowerCase(), tier.cooldown())) {
            sender.sendMessage(services.messages().withPrefix("cooldown",
                    Map.of("seconds", String.valueOf(services.cooldowns().remaining(player,
                            "punish-" + type.name().toLowerCase())))));
            return true;
        }
        Long expires = duration == null ? null : System.currentTimeMillis() + duration;
        String reason = reasonStart < args.length ? CommandUtil.join(args, reasonStart)
                : services.messages().get("punishment-reason");
        String punishmentType = type == Type.MUTE ? "MUTE" : type == Type.IPBAN ? "IPBAN" : "BAN";
        String ip = target.getAddress() == null ? null : target.getAddress().getAddress().getHostAddress();
        DatabaseManager.Punishment punishment = new DatabaseManager.Punishment(target.getUniqueId(), target.getName(), ip,
                punishmentType, reason, sender.getName(), System.currentTimeMillis(), expires);
        services.database().insertPunishment(punishment).thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
            String expiresText = expires == null ? "навсегда" : TimeParser.format(duration);
            if (type == Type.MUTE) {
                sender.sendMessage(services.messages().withPrefix("punishment-applied",
                        Map.of("player", target.getName(), "expires", expiresText)));
                return;
            }
            if (type == Type.IPBAN) {
                Bukkit.getBanList(BanList.Type.IP).addBan(ip, reason,
                        expires == null ? null : new java.util.Date(expires), sender.getName());
                target.kickPlayer(services.messages().withPrefix("ipban-screen",
                        Map.of("reason", reason, "expires", expiresText)));
            } else {
                Bukkit.getBanList(BanList.Type.NAME).addBan(target.getName(), reason,
                        expires == null ? null : new java.util.Date(expires), sender.getName());
                target.kickPlayer(services.messages().withPrefix("ban-screen",
                        Map.of("reason", reason, "expires", expiresText)));
            }
            sender.sendMessage(services.messages().withPrefix("punishment-applied",
                    Map.of("player", target.getName(), "expires", expiresText)));
        })).exceptionally(error -> {
            plugin.getLogger().warning("Не удалось сохранить наказание: " + error.getMessage());
            return null;
        });
        return true;
    }

    private boolean remove(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(services.messages().withPrefix("punishment-usage",
                    Map.of("command", type == Type.UNBAN ? "unban" : "unmute")));
            return true;
        }
        String name = args[0];
        String storedType = type == Type.UNMUTE ? "MUTE" : "BAN";
        services.database().removePunishment(name, storedType).thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (type == Type.UNBAN) Bukkit.getBanList(BanList.Type.NAME).pardon(name);
            sender.sendMessage(services.messages().withPrefix("punishment-removed", Map.of("player", name)));
        }));
        return true;
    }

    private String permission() {
        return switch (type) {
            case BAN -> "core.ban.helper";
            case IPBAN -> "core.ipban.helper";
            case MUTE -> "core.mute.helper";
            case UNBAN -> "core.unban";
            case UNMUTE -> "core.unmute";
            case KICK -> "core.kick";
        };
    }

    private Tier tier(CommandSender sender, Type type) {
        String path = "punishments." + switch (type) {
            case BAN -> "ban-tiers";
            case IPBAN -> "ipban-tiers";
            case MUTE -> "mute-tiers";
            default -> "";
        };
        var section = services.config().section(path);
        if (section == null) return null;
        Tier selected = null;
        for (String key : section.getKeys(false)) {
            String permission = section.getString(key + ".permission", "");
            if (sender.hasPermission(permission)) {
                selected = new Tier(section.getString(key + ".max-duration", "0"),
                        section.getInt(key + ".cooldown", 0));
            }
        }
        return selected;
    }

    private boolean allowed(String max, Long requested) {
        if (max.equalsIgnoreCase("permanent")) return true;
        Long maximum;
        try {
            maximum = TimeParser.parse(max);
        } catch (IllegalArgumentException exception) {
            return false;
        }
        return requested != null && maximum != null && requested <= maximum;
    }

    private record Tier(String maxDuration, int cooldown) {}

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        return List.of();
    }
}