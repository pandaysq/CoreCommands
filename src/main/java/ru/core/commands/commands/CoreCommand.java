package ru.core.commands.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.core.commands.CoreCommandsPlugin;
import ru.core.commands.manager.PluginServices;
import ru.core.commands.util.CommandUtil;

import java.util.List;

public final class CoreCommand extends BaseCommand {
    public CoreCommand(CoreCommandsPlugin plugin, PluginServices services) {
        super(plugin, services);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!CommandUtil.permission(sender, services.messages(), "corecommands.reload")) return true;
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            services.config().reload();
            services.messages().reload();
            sender.sendMessage(text("reloaded"));
            return true;
        }
        sender.sendMessage("§cИспользование: /corecommands reload");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 && sender.hasPermission("corecommands.reload") ? List.of("reload") : List.of();
    }
}