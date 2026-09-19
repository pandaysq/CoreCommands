package ru.core.commands.commands.messaging;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.core.commands.CoreCommandsPlugin;
import ru.core.commands.commands.BaseCommand;
import ru.core.commands.manager.PluginServices;
import ru.core.commands.util.CommandUtil;

import java.util.List;
import java.util.Map;

public final class ReplyCommand extends BaseCommand {
    public ReplyCommand(CoreCommandsPlugin plugin, PluginServices services) {
        super(plugin, services);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player from = CommandUtil.player(sender, services.messages());
        if (from == null || !CommandUtil.permission(from, services.messages(), "core.reply")) return true;
        if (args.length == 0) {
            sender.sendMessage(text("reply-empty"));
            return true;
        }
        Player target = LastMessageStore.get(from);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(text("reply-empty"));
            return true;
        }
        String message = CommandUtil.join(args, 0);
        services.database().isIgnored(target.getUniqueId(), from.getUniqueId()).thenAccept(ignored ->
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    if (ignored) {
                        from.sendMessage(text("message-ignored"));
                        return;
                    }
                    from.sendMessage(services.messages().withPrefix("message-sent",
                            Map.of("player", target.getName(), "message", message)));
                    target.sendMessage(services.messages().withPrefix("message-received",
                            Map.of("player", from.getName(), "message", message)));
                    LastMessageStore.remember(from, target);
                    LastMessageStore.remember(target, from);
                }));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}