package ru.core.commands.commands.wipe;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import ru.core.commands.CoreCommandsPlugin;
import ru.core.commands.commands.BaseCommand;
import ru.core.commands.manager.PluginServices;
import ru.core.commands.util.CommandUtil;
import ru.core.commands.util.WiperRegistry;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WipePlayerCommand extends BaseCommand {
    private final WiperRegistry wipers;
    private final Map<UUID, Confirmation> confirmations = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public WipePlayerCommand(CoreCommandsPlugin plugin, PluginServices services, WiperRegistry wipers) {
        super(plugin, services);
        this.wipers = wipers;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!CommandUtil.permission(sender, services.messages(), "core.wipeplayer")) return true;
        if (args.length != 1 && args.length != 3) {
            sender.sendMessage("§cИспользование: /wipeplayer <игрок> [confirm <ключ>]");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (args.length == 1) {
            String key = String.valueOf(100000 + random.nextInt(900000));
            UUID senderId = sender instanceof org.bukkit.entity.Player player ? player.getUniqueId() : UUID.nameUUIDFromBytes(sender.getName().getBytes());
            long expires = System.currentTimeMillis() + services.config().get().getLong("wipeplayer.confirmation-seconds", 60) * 1000L;
            confirmations.put(senderId, new Confirmation(target.getUniqueId(), key, expires));
            sender.sendMessage(services.messages().withPrefix("wipe-key",
                    Map.of("player", target.getName() == null ? args[0] : target.getName(), "key", key)));
            return true;
        }
        if (!args[1].equalsIgnoreCase("confirm")) {
            sender.sendMessage("§cИспользование: /wipeplayer <игрок> confirm <ключ>");
            return true;
        }
        UUID senderId = sender instanceof org.bukkit.entity.Player player ? player.getUniqueId() : UUID.nameUUIDFromBytes(sender.getName().getBytes());
        Confirmation confirmation = confirmations.get(senderId);
        if (confirmation == null || !confirmation.player().equals(target.getUniqueId())
                || confirmation.expiresAt() < System.currentTimeMillis() || !confirmation.key().equals(args[2])) {
            sender.sendMessage(text("wipe-invalid-key"));
            return true;
        }
        confirmations.remove(senderId);
        wipers.wipe(target).thenRun(() -> Bukkit.getScheduler().runTask(plugin, () ->
                sender.sendMessage(services.messages().withPrefix("wipe-complete",
                        Map.of("player", target.getName() == null ? args[0] : target.getName()))))).exceptionally(error -> {
            plugin.getLogger().warning("Не удалось стереть данные игрока: " + error.getMessage());
            return null;
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream().map(org.bukkit.entity.Player::getName).toList() : List.of();
    }

    private record Confirmation(UUID player, String key, long expiresAt) {}
}