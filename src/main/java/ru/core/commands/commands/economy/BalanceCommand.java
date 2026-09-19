package ru.core.commands.commands.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.core.commands.CoreCommandsPlugin;
import ru.core.commands.commands.BaseCommand;
import ru.core.commands.manager.PluginServices;
import ru.core.commands.util.CommandUtil;

import java.util.List;
import java.util.Map;

public final class BalanceCommand extends BaseCommand {
    private final Economy economy;

    public BalanceCommand(CoreCommandsPlugin plugin, PluginServices services, Economy economy) {
        super(plugin, services);
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!CommandUtil.permission(sender, services.messages(), "core.balance")) return true;
        OfflinePlayer target;
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(text("player-only"));
                return true;
            }
            target = player;
        } else {
            if (!CommandUtil.permission(sender, services.messages(), "core.balance.others")) return true;
            target = Bukkit.getOfflinePlayer(args[0]);
        }
        sender.sendMessage(services.messages().withPrefix("balance",
                Map.of("player", target.getName() == null ? args.length > 0 ? args[0] : "unknown" : target.getName(),
                        "amount", economy.format(economy.getBalance(target)))));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 && sender.hasPermission("core.balance.others")
                ? Bukkit.getOnlinePlayers().stream().map(Player::getName).toList() : List.of();
    }
}