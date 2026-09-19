package ru.core.commands.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;
import ru.core.commands.database.DatabaseManager;
import ru.core.commands.manager.PluginServices;
import ru.core.commands.util.TimeParser;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class LoginListener implements Listener {
    private final JavaPlugin plugin;
    private final PluginServices services;

    public LoginListener(JavaPlugin plugin, PluginServices services) {
        this.plugin = plugin;
        this.services = services;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        String address = event.getAddress().getHostAddress();
        DatabaseManager.Punishment ipBan = services.database().activeIpPunishment(address).join();
        DatabaseManager.Punishment ban = services.database().activePunishment(event.getUniqueId().toString(), "BAN").join();
        DatabaseManager.Punishment found = ipBan != null ? ipBan : ban;
        if (found == null) return;
        String expires = found.expiresAt() == null ? "навсегда" : TimeParser.format(found.expiresAt() - System.currentTimeMillis());
        String message = services.messages().text(ipBan != null ? "ipban-screen" : "ban-screen",
                Map.of("reason", found.reason(), "expires", expires));
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, message);
    }
}