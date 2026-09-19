package ru.core.commands.commands.teleport;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.core.commands.CoreCommandsPlugin;
import ru.core.commands.commands.BaseCommand;
import ru.core.commands.manager.PluginServices;
import ru.core.commands.util.CommandUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class RtpCommand extends BaseCommand {
    public RtpCommand(CoreCommandsPlugin plugin, PluginServices services) {
        super(plugin, services);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = CommandUtil.player(sender, services.messages());
        if (player == null || !CommandUtil.permission(player, services.messages(), "core.rtp")) return true;
        if (!services.cooldowns().ready(player, "rtp", services.config().cooldown("rtp"))) {
            sender.sendMessage(services.messages().withPrefix("cooldown",
                    Map.of("seconds", String.valueOf(services.cooldowns().remaining(player, "rtp")))));
            return true;
        }
        World world = Bukkit.getWorld(services.config().get().getString("rtp.world", player.getWorld().getName()));
        if (world == null) {
            sender.sendMessage(text("unsafe-location"));
            return true;
        }
        int min = services.config().get().getInt("rtp.min-radius", 100);
        int max = services.config().get().getInt("rtp.max-radius", 3000);
        int attempts = services.config().get().getInt("rtp.attempts", 12);
        int maxY = services.config().get().getInt("rtp.max-y", 320);
        Location found = null;
        for (int i = 0; i < attempts; i++) {
            double angle = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2);
            double radius = ThreadLocalRandom.current().nextDouble(min, Math.max(min + 1, max));
            int x = (int) Math.round(Math.cos(angle) * radius);
            int z = (int) Math.round(Math.sin(angle) * radius);
            int y = world.getHighestBlockYAt(x, z);
            if (y > 0 && y < maxY) {
                Block ground = world.getBlockAt(x, y - 1, z);
                Block feet = world.getBlockAt(x, y, z);
                Block head = world.getBlockAt(x, y + 1, z);
                if (ground.getType().isSolid() && feet.isEmpty() && head.isEmpty()
                        && ground.getType() != Material.LAVA && ground.getType() != Material.WATER) {
                    found = new Location(world, x + 0.5, y, z + 0.5, player.getYaw(), player.getPitch());
                    break;
                }
            }
        }
        if (found == null) {
            sender.sendMessage(text("unsafe-location"));
            return true;
        }
        Location destination = found;
        services.teleportRequests().warmup(player, () -> {
            player.teleport(destination);
            player.sendMessage(text("teleported"));
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}