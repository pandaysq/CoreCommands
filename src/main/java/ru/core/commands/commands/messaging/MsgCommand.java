package ru.core.commands.commands.messaging;

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

public final class MsgCommand extends BaseCommand {
    public MsgCommand(CoreCommandsPlugin plugin, PluginServices services) {
        super(plugin, services);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player from = CommandUtil.player(sender, services.messages());
        if (from == null || !CommandUtil.permission(from, services.messages(), "core.msg")) return true;
        if (args.length < 2) {
            sender.sendMessage(text("message-usage"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(services.messages().withPrefix("unknown-player", Map.of("player", args[0])));
            return true;
        }
        if (target.equals(from)) {
            sender.sendMessage(services.messages().withPrefix("message-self", Map.of()));
            return true;
        }
        if (!services.cooldowns().ready(from, "msg", services.config().cooldown("msg"))) {
            sender.sendMessage(services.messages().withPrefix("cooldown",
                    Map.of("seconds", String.valueOf(services.cooldowns().remaining(from, "msg")))));
            return true;
        }
        String message = CommandUtil.join(args, 1);
        services.database().isIgnored(target.getUniqueId(), from.getUniqueId()).thenAccept(ignored ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (ignored) {
                        from.sendMessage(text("message-ignored"));
                        return;
                    }
                    from.sendMessage(services.messages().withPrefix("message-sent",
                            Map.of("player", target.getName(), "message", message)));
                    target.sendMessage(services.messages().withPrefix("message-received",
                            Map.of("player", from.getName(), "message", message)));
                    plugin.getServer().getPluginManager().callEvent(new PrivateMessageEvent(from, target));
                }));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream().map(Player::getName).toList() : List.of();
    }
}