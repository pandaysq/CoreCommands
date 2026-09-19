package ru.core.commands.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.core.commands.config.MessageManager;

import java.util.Map;

public final class CommandUtil {
    private CommandUtil() {
    }

    public static boolean permission(CommandSender sender, MessageManager messages, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        sender.sendMessage(messages.withPrefix("no-permission", Map.of("permission", permission)));
        return false;
    }

    public static Player player(CommandSender sender, MessageManager messages) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(messages.withPrefix("player-only", Map.of()));
        return null;
    }

    public static Player online(CommandSender sender, MessageManager messages, String name) {
        Player player = Bukkit.getPlayerExact(name);
        if (player == null) {
            sender.sendMessage(messages.withPrefix("unknown-player", Map.of("player", name)));
        }
        return player;
    }

    public static OfflinePlayer offline(String name) {
        return Bukkit.getOfflinePlayer(name);
    }

    public static String join(String[] args, int start) {
        return String.join(" ", java.util.Arrays.copyOfRange(args, start, args.length));
    }
}