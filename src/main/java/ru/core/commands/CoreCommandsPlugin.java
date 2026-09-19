package ru.core.commands;

import org.bukkit.plugin.java.JavaPlugin;
import ru.core.commands.manager.PluginBootstrap;
import ru.core.commands.manager.PluginServices;

public final class CoreCommandsPlugin extends JavaPlugin {
    private PluginServices services;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
        services = new PluginBootstrap(this).register();
    }

    @Override
    public void onDisable() {
        if (services != null) {
            services.shutdown();
        }
    }

    public PluginServices services() {
        return services;
    }
}