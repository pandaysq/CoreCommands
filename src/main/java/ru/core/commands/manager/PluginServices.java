package ru.core.commands.manager;

import ru.core.commands.config.ConfigManager;
import ru.core.commands.config.MessageManager;
import ru.core.commands.database.DatabaseManager;
import ru.core.commands.util.CooldownManager;
import ru.core.commands.util.TeleportRequestManager;
import ru.core.commands.util.VanishManager;

public final class PluginServices {
    private final ConfigManager config;
    private final MessageManager messages;
    private final DatabaseManager database;
    private final CooldownManager cooldowns;
    private final TeleportRequestManager teleportRequests;
    private final VanishManager vanish;

    public PluginServices(ConfigManager config, MessageManager messages, DatabaseManager database,
                          CooldownManager cooldowns, TeleportRequestManager teleportRequests,
                          VanishManager vanish) {
        this.config = config;
        this.messages = messages;
        this.database = database;
        this.cooldowns = cooldowns;
        this.teleportRequests = teleportRequests;
        this.vanish = vanish;
    }

    public ConfigManager config() { return config; }
    public MessageManager messages() { return messages; }
    public DatabaseManager database() { return database; }
    public CooldownManager cooldowns() { return cooldowns; }
    public TeleportRequestManager teleportRequests() { return teleportRequests; }
    public VanishManager vanish() { return vanish; }

    public void shutdown() {
        database.close();
    }
}