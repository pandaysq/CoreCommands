package ru.core.commands.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import ru.core.commands.config.ConfigManager;
import ru.core.commands.config.MessageManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TeleportRequestManager {
    public record Request(UUID from, UUID to, long expiresAt) {}

    private final JavaPlugin plugin;
    private final ConfigManager config;
    private final MessageManager messages;
    private final Map<UUID, Request> requests = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> warmups = new ConcurrentHashMap<>();

    public TeleportRequestManager(JavaPlugin plugin, ConfigManager config, MessageManager messages) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
    }

    public void put(Player from, Player to) {
        requests.put(to.getUniqueId(), new Request(from.getUniqueId(), to.getUniqueId(),
                System.currentTimeMillis() + config.get().getLong("teleport.request-timeout", 60) * 1000));
    }

    public Request take(Player target) {
        Request request = requests.remove(target.getUniqueId());
        if (request == null || request.expiresAt() < System.currentTimeMillis()) {
            return null;
        }
        return request;
    }

    public void deny(Player target) {
        Request request = requests.remove(target.getUniqueId());
        if (request != null) {
            Player from = Bukkit.getPlayer(request.from());
            if (from != null) {
                from.sendMessage(messages.withPrefix("teleport-denied", Map.of()));
            }
        }
    }

    public void warmup(Player player, Runnable action) {
        cancelWarmup(player);
        int seconds = config.get().getInt("teleport.warmup-seconds", 3);
        if (seconds <= 0 || player.hasPermission("core.bypass.cooldown")) {
            action.run();
            return;
        }
        player.sendMessage(messages.withPrefix("teleport-warmup", Map.of("seconds", String.valueOf(seconds))));
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, action, seconds * 20L);
        warmups.put(player.getUniqueId(), task);
    }

    public void cancelWarmup(Player player) {
        BukkitTask task = warmups.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }
}