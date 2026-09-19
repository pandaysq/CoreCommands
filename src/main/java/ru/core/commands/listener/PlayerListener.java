package ru.core.commands.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import ru.core.commands.manager.PluginServices;

public final class PlayerListener implements Listener {
    private final JavaPlugin plugin;
    private final PluginServices services;

    public PlayerListener(JavaPlugin plugin, PluginServices services) {
        this.plugin = plugin;
        this.services = services;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        for (org.bukkit.entity.Player vanished : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (services.vanish().isVanished(vanished) && !event.getPlayer().hasPermission("core.vanish.see")
                    && !vanished.equals(event.getPlayer())) {
                event.getPlayer().hidePlayer(plugin, vanished);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        services.teleportRequests().cancelWarmup(event.getPlayer());
        if (services.vanish().isVanished(event.getPlayer())) {
            services.vanish().disable(event.getPlayer(), false);
            services.vanish().mark(event.getPlayer(), false);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!services.config().get().getBoolean("teleport.cancel-on-move", true)
                || event.getTo() == null || event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;
        services.teleportRequests().cancelWarmup(event.getPlayer());
    }
}