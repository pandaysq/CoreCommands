package ru.core.commands.commands.teleport;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.core.commands.CoreCommandsPlugin;
import ru.core.commands.commands.BaseCommand;
import ru.core.commands.manager.PluginServices;
import ru.core.commands.util.CommandUtil;

import java.util.List;
import java.util.Map;

public final class SpawnCommand extends BaseCommand {
    public enum Type { SPAWN, SETSPAWN }
    private final Type type;

    public SpawnCommand(CoreCommandsPlugin plugin, PluginServices services, Type type) {
        super(plugin, services);
        this.type = type;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = CommandUtil.player(sender, services.messages());
        if (player == null || !CommandUtil.permission(player, services.messages(), permission())) return true;
        if (type == Type.SETSPAWN) {
            services.config().setSpawn(player.getLocation());
            sender.sendMessage(text("spawn-set"));
            return true;
        }
        Location spawn = services.config().spawn();
        if (spawn == null || spawn.getWorld() == null) {
            sender.sendMessage(text("spawn-missing"));
            return true;
        }
        if (!services.cooldowns().ready(player, "spawn", services.config().cooldown("spawn"))) {
            sender.sendMessage(services.messages().withPrefix("cooldown",
                    Map.of("seconds", String.valueOf(services.cooldowns().remaining(player, "spawn")))));
            return true;
        }
        services.teleportRequests().warmup(player, () -> {
            player.teleport(spawn);
            player.sendMessage(text("teleported"));
        });
        return true;
    }

    private String permission() {
        return type == Type.SETSPAWN ? "core.setspawn" : "core.spawn";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}