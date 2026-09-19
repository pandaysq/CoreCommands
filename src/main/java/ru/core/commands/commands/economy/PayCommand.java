package ru.core.commands.commands.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
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

public final class PayCommand extends BaseCommand {
    private final Economy economy;

    public PayCommand(CoreCommandsPlugin plugin, PluginServices services, Economy economy) {
        super(plugin, services);
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player payer = CommandUtil.player(sender, services.messages());
        if (payer == null || !CommandUtil.permission(payer, services.messages(), "core.pay")) return true;
        if (args.length != 2) {
            sender.sendMessage(text("message-usage").replace("/msg", "/pay <игрок>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(services.messages().withPrefix("unknown-player", Map.of("player", args[0])));
            return true;
        }
        if (target.equals(payer)) {
            sender.sendMessage(text("payment-self"));
            return true;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(text("invalid-number"));
            return true;
        }
        double minimum = services.config().get().getDouble("economy.minimum-payment", 0.01);
        if (amount <= 0) {
            sender.sendMessage(text("payment-positive"));
            return true;
        }
        if (amount < minimum) {
            sender.sendMessage(services.messages().withPrefix("payment-minimum",
                    Map.of("amount", economy.format(minimum))));
            return true;
        }
        if (!services.cooldowns().ready(payer, "pay", services.config().cooldown("pay"))) {
            sender.sendMessage(services.messages().withPrefix("cooldown",
                    Map.of("seconds", String.valueOf(services.cooldowns().remaining(payer, "pay")))));
            return true;
        }
        EconomyResponse withdraw = economy.withdrawPlayer(payer, amount);
        if (!withdraw.transactionSuccess()) {
            sender.sendMessage(text("payment-too-much"));
            return true;
        }
        EconomyResponse deposit = economy.depositPlayer(target, amount);
        if (!deposit.transactionSuccess()) {
            economy.depositPlayer(payer, amount);
            sender.sendMessage(text("internal-error"));
            return true;
        }
        Map<String, String> values = Map.of("player", target.getName(), "amount", economy.format(amount));
        payer.sendMessage(services.messages().withPrefix("payment-sent", values));
        target.sendMessage(services.messages().withPrefix("payment-received",
                Map.of("player", payer.getName(), "amount", economy.format(amount))));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream().map(Player::getName).toList() : List.of();
    }
}