package ru.core.commands.commands.service;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.core.commands.CoreCommandsPlugin;
import ru.core.commands.commands.BaseCommand;
import ru.core.commands.manager.PluginServices;
import ru.core.commands.util.CommandUtil;
import ru.core.commands.util.TimeParser;

import java.util.List;
import java.util.Map;

public final class VanishCommand extends BaseCommand {
    public VanishCommand(CoreCommandsPlugin plugin, PluginServices services) {
        super(plugin, services);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = CommandUtil.player(sender, services.messages());
        if (player == null) return true;
        long available = services.vanish().availableSeconds(player);
        if (available == 0) {
            sender.sendMessage(text("vanish-no-tier"));
            return true;
        }
        if (services.vanish().isVanished(player)) {
            services.vanish().disable(player, false);
            services.vanish().mark(player, false);
            sender.sendMessage(text("vanish-disabled"));
            return true;
        }
        if (!services.cooldowns().ready(player, "vanish", services.config().cooldown("vanish"))) {
            sender.sendMessage(services.messages().withPrefix("cooldown",
                    Map.of("seconds", String.valueOf(services.cooldowns().remaining(player, "vanish")))));
            return true;
        }
        services.vanish().mark(player, true);
        services.vanish().enable(player, available);
        sender.sendMessage(text("vanish-enabled"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}