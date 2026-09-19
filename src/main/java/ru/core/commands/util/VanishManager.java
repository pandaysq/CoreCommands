package ru.core.commands.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.java.JavaPlugin;
import ru.core.commands.config.ConfigManager;
import ru.core.commands.config.MessageManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VanishManager {
    private final JavaPlugin plugin;
    private final ConfigManager config;
    private final MessageManager messages;
    private final Map<UUID, BukkitTask> expiry = new ConcurrentHashMap<>();

    public VanishManager(JavaPlugin plugin, ConfigManager config, MessageManager messages) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
    }

    public void enable(Player player, long seconds) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(player) && !online.hasPermission("core.vanish.see")) {
                online.hidePlayer(plugin, player);
            }
        }
        if (seconds > 0) {
            expiry.put(player.getUniqueId(), Bukkit.getScheduler().runTaskLater(plugin,
                    () -> disable(player, true), seconds * 20L));
        }
    }

    public void disable(Player player, boolean expired) {
        BukkitTask task = expiry.remove(player.getUniqueId());
        if (task != null) task.cancel();
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(plugin, player);
        }
        if (expired && player.isOnline()) {
            player.sendMessage(messages.withPrefix("vanish-expired", Map.of()));
        }
    }

    public boolean isVanished(Player player) {
        return expiry.containsKey(player.getUniqueId()) || player.hasMetadata("corecommands_vanished");
    }

    public void mark(Player player, boolean value) {
        if (value) player.setMetadata("corecommands_vanished", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
        else player.removeMetadata("corecommands_vanished", plugin);
    }

    public long availableSeconds(Player player) {
        if (player.hasPermission("core.vanish.unlimited")) return -1;
        if (player.hasPermission("core.vanish.long")) return parse(config.get().getString("vanish.tiers.long.max-time", "1h"));
        if (player.hasPermission("core.vanish.short")) return parse(config.get().getString("vanish.tiers.short.max-time", "5m"));
        return 0;
    }

    private long parse(String value) {
        Long millis = TimeParser.parse(value);
        return millis == null ? -1 : millis / 1000;
    }
}