package ru.core.commands.commands.messaging;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LastMessageStore {
    private static final Map<UUID, UUID> LAST = new ConcurrentHashMap<>();

    private LastMessageStore() {}

    public static void remember(Player player, Player other) {
        LAST.put(player.getUniqueId(), other.getUniqueId());
    }

    public static Player get(Player player) {
        UUID id = LAST.get(player.getUniqueId());
        return id == null ? null : org.bukkit.Bukkit.getPlayer(id);
    }
}