package ru.core.commands.commands.ignore;

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

public final class IgnoreCommand extends BaseCommand {
    public IgnoreCommand(CoreCommandsPlugin plugin, PluginServices services) {
        super(plugin, services);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = CommandUtil.player(sender, services.messages());
        if (player == null || !CommandUtil.permission(player, services.messages(), "core.ignore")) return true;
        if (args.length != 1) {
            sender.sendMessage(text("ignore-usage"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(services.messages().withPrefix("unknown-player", Map.of("player", args[0])));
            return true;
        }
        if (target.equals(player)) {
            sender.sendMessage(text("ignore-self"));
            return true;
        }
        if (!services.cooldowns().ready(player, "ignore", services.config().cooldown("ignore"))) {
            sender.sendMessage(services.messages().withPrefix("cooldown",
                    Map.of("seconds", String.valueOf(services.cooldowns().remaining(player, "ignore")))));
            return true;
        }
        services.database().toggleIgnore(player.getUniqueId(), target.getUniqueId()).thenAccept(enabled ->
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(services.messages().withPrefix(
                        enabled ? "ignore-enabled" : "ignore-disabled", Map.of("player", target.getName())))));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream().map(Player::getName).toList() : List.of();
    }
}