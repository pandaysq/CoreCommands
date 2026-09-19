package ru.core.commands.commands.teleport;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.core.commands.CoreCommandsPlugin;
import ru.core.commands.commands.BaseCommand;
import ru.core.commands.manager.PluginServices;
import ru.core.commands.util.CommandUtil;

import java.util.List;
import java.util.Map;

public final class TeleportCommand extends BaseCommand {
    public enum Type { TPA, ACCEPT, DENY, TP, TPHERE }
    private final Type type;

    public TeleportCommand(CoreCommandsPlugin plugin, PluginServices services, Type type) {
        super(plugin, services);
        this.type = type;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = CommandUtil.player(sender, services.messages());
        if (player == null) return true;
        if (!CommandUtil.permission(player, services.messages(), permission())) return true;
        if (type == Type.ACCEPT) {
            TeleportAccept(player);
            return true;
        }
        if (type == Type.DENY) {
            services.teleportRequests().deny(player);
            player.sendMessage(text("teleport-denied"));
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(services.messages().withPrefix("teleport-usage", Map.of("command", label)));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(services.messages().withPrefix("unknown-player", Map.of("player", args[0])));
            return true;
        }
        if (type == Type.TPA) {
            if (!services.cooldowns().ready(player, "tpa", services.config().cooldown("tpa"))) {
                player.sendMessage(services.messages().withPrefix("cooldown",
                        Map.of("seconds", String.valueOf(services.cooldowns().remaining(player, "tpa")))));
                return true;
            }
            services.teleportRequests().put(player, target);
            player.sendMessage(services.messages().withPrefix("teleport-request-sent",
                    Map.of("player", target.getName())));
            target.sendMessage(services.messages().withPrefix("teleport-request-received",
                    Map.of("player", player.getName())));
            return true;
        }
        Runnable action = () -> {
            if (type == Type.TP) player.teleport(target);
            else target.teleport(player);
            player.sendMessage(text("teleported"));
        };
        services.teleportRequests().warmup(player, action);
        return true;
    }

    private void TeleportAccept(Player target) {
        var request = services.teleportRequests().take(target);
        if (request == null) {
            target.sendMessage(text("teleport-expired"));
            return;
        }
        Player from = Bukkit.getPlayer(request.from());
        if (from == null) {
            target.sendMessage(text("teleport-expired"));
            return;
        }
        services.teleportRequests().warmup(from, () -> {
            from.teleport(target);
            from.sendMessage(text("teleported"));
            target.sendMessage(text("teleport-accepted"));
        });
    }

    private String permission() {
        return switch (type) {
            case TPA -> "core.tpa";
            case ACCEPT -> "core.tpaccept";
            case DENY -> "core.tpdeny";
            case TP -> "core.tp";
            case TPHERE -> "core.tphere";
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 && (type == Type.TPA || type == Type.TP || type == Type.TPHERE)
                ? Bukkit.getOnlinePlayers().stream().map(Player::getName).toList() : List.of();
    }
}