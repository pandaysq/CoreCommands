package ru.core.commands.config;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;

public final class MessageManager {
    private final JavaPlugin plugin;
    private FileConfiguration messages;

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        messages = YamlConfiguration.loadConfiguration(file);
    }

    public String get(String path) {
        return messages.getString(path, path);
    }

    public String text(String path, Map<String, String> placeholders) {
        String value = get(path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            value = value.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    public String text(String path) {
        return text(path, Map.of());
    }

    public String prefix() {
        return color(messages.getString("prefix", ""));
    }

    public String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    public String withPrefix(String path, Map<String, String> placeholders) {
        return prefix() + text(path, placeholders);
    }
}