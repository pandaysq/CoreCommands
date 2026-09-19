package ru.core.commands.commands.service;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import ru.core.commands.CoreCommandsPlugin;
import ru.core.commands.commands.BaseCommand;
import ru.core.commands.manager.PluginServices;
import ru.core.commands.util.CommandUtil;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TpsCommand extends BaseCommand {
    public TpsCommand(CoreCommandsPlugin plugin, PluginServices services) {
        super(plugin, services);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!CommandUtil.permission(sender, services.messages(), "core.tps")) return true;
        double[] tps = Bukkit.getServer().getTPS();
        double mspt = Bukkit.getServer().getAverageTickTime();
        sender.sendMessage(services.messages().withPrefix("performance", Map.of(
                "tps", String.format(Locale.US, "%.2f", Math.min(20, tps[0])),
                "mspt", String.format(Locale.US, "%.2f", mspt))));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}