package ru.core.commands.config;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConfigManager {
    private final JavaPlugin plugin;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public FileConfiguration get() {
        return plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
    }

    public int cooldown(String key) {
        return get().getInt("cooldowns." + key, 0);
    }

    public Location spawn() {
        if (!get().isConfigurationSection("spawn.location")) {
            return null;
        }
        return get().getLocation("spawn.location");
    }

    public void setSpawn(Location location) {
        get().set("spawn.location", location);
        plugin.saveConfig();
    }

    public ConfigurationSection section(String path) {
        return get().getConfigurationSection(path);
    }
}