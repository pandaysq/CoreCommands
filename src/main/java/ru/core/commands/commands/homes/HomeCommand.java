package ru.core.commands.commands.homes;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.core.commands.CoreCommandsPlugin;
import ru.core.commands.commands.BaseCommand;
import ru.core.commands.manager.PluginServices;
import ru.core.commands.util.CommandUtil;

import java.util.List;
import java.util.Map;

public final class HomeCommand extends BaseCommand {
    public enum Type { SET, GO, DELETE, LIST }
    private final Type type;

    public HomeCommand(CoreCommandsPlugin plugin, PluginServices services, Type type) {
        super(plugin, services);
        this.type = type;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = CommandUtil.player(sender, services.messages());
        if (player == null || !CommandUtil.permission(player, services.messages(), permission())) return true;
        if (type == Type.LIST) {
            services.database().homes(player.getUniqueId()).thenAccept(homes ->
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage(homes.isEmpty()
                                ? text("homes-empty")
                                : services.messages().withPrefix("homes-list", Map.of("homes", String.join(", ", homes))));
                    }));
            return true;
        }
        String name = args.length == 0 ? services.config().get().getString("homes.default-name", "home") : args[0].toLowerCase();
        if (!name.matches("[a-z0-9_-]{1,24}")) {
            sender.sendMessage(text("invalid-number"));
            return true;
        }
        if (type == Type.SET) {
            if (!services.cooldowns().ready(player, "sethome", services.config().cooldown("sethome"))) {
                sender.sendMessage(services.messages().withPrefix("cooldown",
                        Map.of("seconds", String.valueOf(services.cooldowns().remaining(player, "sethome")))));
                return true;
            }
            services.database().homes(player.getUniqueId()).thenAccept(homes -> {
                int limit = homeLimit(player);
                if (!homes.contains(name) && homes.size() >= limit) {
                    player.sendMessage(services.messages().withPrefix("homes-limit", Map.of("limit", String.valueOf(limit))));
                    return;
                }
                services.database().saveHome(player.getUniqueId(), name, player.getLocation()).thenRun(() ->
                        org.bukkit.Bukkit.getScheduler().runTask(plugin, () ->
                                player.sendMessage(services.messages().withPrefix("home-set", Map.of("name", name)))));
            });
            return true;
        }
        if (type == Type.DELETE) {
            services.database().deleteHome(player.getUniqueId(), name).thenRun(() ->
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () ->
                            player.sendMessage(services.messages().withPrefix("home-deleted", Map.of("name", name)))));
            return true;
        }
        if (!services.cooldowns().ready(player, "home", services.config().cooldown("home"))) {
            sender.sendMessage(services.messages().withPrefix("cooldown",
                    Map.of("seconds", String.valueOf(services.cooldowns().remaining(player, "home")))));
            return true;
        }
        services.database().home(player.getUniqueId(), name).thenAccept(location ->
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    if (location == null) {
                        player.sendMessage(services.messages().withPrefix("home-not-found", Map.of("name", name)));
                        return;
                    }
                    services.teleportRequests().warmup(player, () -> {
                        player.teleport(location);
                        player.sendMessage(text("teleported"));
                    });
                }));
        return true;
    }

    private int homeLimit(Player player) {
        int limit = 0;
        var section = services.config().section("homes.limits");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                if (player.hasPermission(section.getString(key + ".permission", ""))) {
                    limit = Math.max(limit, section.getInt(key + ".limit", 0));
                }
            }
        }
        return limit;
    }

    private String permission() {
        return switch (type) {
            case SET -> "core.sethome";
            case GO -> "core.home";
            case DELETE -> "core.delhome";
            case LIST -> "core.homes";
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}