package ru.core.commands.commands;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import ru.core.commands.CoreCommandsPlugin;
import ru.core.commands.manager.PluginServices;

public abstract class BaseCommand implements CommandExecutor, TabCompleter {
    protected final CoreCommandsPlugin plugin;
    protected final PluginServices services;

    protected BaseCommand(CoreCommandsPlugin plugin, PluginServices services) {
        this.plugin = plugin;
        this.services = services;
    }

    protected String text(String path) {
        return services.messages().withPrefix(path, java.util.Map.of());
    }
}