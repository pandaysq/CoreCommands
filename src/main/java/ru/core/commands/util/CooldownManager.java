package ru.core.commands.util;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CooldownManager {
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    public boolean ready(Player player, String key, int seconds) {
        if (player.hasPermission("core.bypass.cooldown") || seconds <= 0) {
            return true;
        }
        String id = player.getUniqueId() + ":" + key;
        long now = System.currentTimeMillis();
        Long until = cooldowns.get(id);
        if (until != null && until > now) {
            return false;
        }
        cooldowns.put(id, now + seconds * 1000L);
        return true;
    }

    public long remaining(Player player, String key) {
        long left = cooldowns.getOrDefault(player.getUniqueId() + ":" + key, 0L)
                - System.currentTimeMillis();
        return Math.max(0, (left + 999) / 1000);
    }
}