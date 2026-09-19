package ru.core.commands.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import ru.core.commands.commands.messaging.LastMessageStore;
import ru.core.commands.manager.PluginServices;

import java.util.Map;

public final class ChatListener implements Listener {
    private final JavaPlugin plugin;
    private final PluginServices services;

    public ChatListener(JavaPlugin plugin, PluginServices services) {
        this.plugin = plugin;
        this.services = services;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (services.database().activePunishment(event.getPlayer().getUniqueId().toString(), "MUTE").join() != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(services.messages().withPrefix("muted", Map.of()));
            return;
        }
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        for (org.bukkit.entity.Player recipient : event.viewers().stream()
                .filter(viewer -> viewer instanceof org.bukkit.entity.Player)
                .map(viewer -> (org.bukkit.entity.Player) viewer).toList()) {
            if (services.database().isIgnored(recipient.getUniqueId(), event.getPlayer().getUniqueId()).join()) {
                event.viewers().remove(recipient);
            }
        }
    }
}