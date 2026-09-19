package ru.core.commands.manager;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import ru.core.commands.CoreCommandsPlugin;
import ru.core.commands.commands.CoreCommand;
import ru.core.commands.commands.economy.BalanceCommand;
import ru.core.commands.commands.economy.PayCommand;
import ru.core.commands.commands.homes.HomeCommand;
import ru.core.commands.commands.ignore.IgnoreCommand;
import ru.core.commands.commands.messaging.MsgCommand;
import ru.core.commands.commands.messaging.ReplyCommand;
import ru.core.commands.commands.punishment.PunishmentCommand;
import ru.core.commands.commands.service.TpsCommand;
import ru.core.commands.commands.service.VanishCommand;
import ru.core.commands.commands.teleport.RtpCommand;
import ru.core.commands.commands.teleport.SpawnCommand;
import ru.core.commands.commands.teleport.TeleportCommand;
import ru.core.commands.commands.wipe.WipePlayerCommand;
import ru.core.commands.config.ConfigManager;
import ru.core.commands.config.MessageManager;
import ru.core.commands.database.DatabaseManager;
import ru.core.commands.listener.ChatListener;
import ru.core.commands.listener.LoginListener;
import ru.core.commands.listener.PlayerListener;
import ru.core.commands.util.CooldownManager;
import ru.core.commands.util.TeleportRequestManager;
import ru.core.commands.util.VanishManager;
import ru.core.commands.util.WiperRegistry;

public final class PluginBootstrap {
    private final CoreCommandsPlugin plugin;

    public PluginBootstrap(CoreCommandsPlugin plugin) {
        this.plugin = plugin;
    }

    public PluginServices register() {
        ConfigManager config = new ConfigManager(plugin);
        MessageManager messages = new MessageManager(plugin);
        DatabaseManager database = new DatabaseManager(plugin, config);
        CooldownManager cooldowns = new CooldownManager();
        TeleportRequestManager requests = new TeleportRequestManager(plugin, config, messages);
        VanishManager vanish = new VanishManager(plugin, config, messages);
        PluginServices services = new PluginServices(config, messages, database, cooldowns, requests, vanish);
        Economy economy = setupEconomy();
        WiperRegistry wipers = new WiperRegistry(database, economy);

        registerCommands(services, economy, wipers);
        Bukkit.getPluginManager().registerEvents(new LoginListener(plugin, services), plugin);
        Bukkit.getPluginManager().registerEvents(new ChatListener(plugin, services), plugin);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(plugin, services), plugin);
        database.initialize();
        return services;
    }

    private Economy setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault не найден. Экономические команды отключены.");
            return null;
        }
        RegisteredServiceProvider<Economy> registration =
                Bukkit.getServicesManager().getRegistration(Economy.class);
        if (registration == null) {
            plugin.getLogger().warning("Провайдер экономики не найден. Экономические команды отключены.");
            return null;
        }
        return registration.getProvider();
    }

    private void registerCommands(PluginServices services, Economy economy, WiperRegistry wipers) {
        register("corecommands", new CoreCommand(plugin, services));
        if (economy != null) {
            register("balance", new BalanceCommand(plugin, services, economy));
            register("pay", new PayCommand(plugin, services, economy));
        }
        register("msg", new MsgCommand(plugin, services));
        register("reply", new ReplyCommand(plugin, services));
        register("ban", new PunishmentCommand(plugin, services, PunishmentCommand.Type.BAN));
        register("unban", new PunishmentCommand(plugin, services, PunishmentCommand.Type.UNBAN));
        register("ipban", new PunishmentCommand(plugin, services, PunishmentCommand.Type.IPBAN));
        register("mute", new PunishmentCommand(plugin, services, PunishmentCommand.Type.MUTE));
        register("unmute", new PunishmentCommand(plugin, services, PunishmentCommand.Type.UNMUTE));
        register("kick", new PunishmentCommand(plugin, services, PunishmentCommand.Type.KICK));
        register("tpa", new TeleportCommand(plugin, services, TeleportCommand.Type.TPA));
        register("tpaccept", new TeleportCommand(plugin, services, TeleportCommand.Type.ACCEPT));
        register("tpdeny", new TeleportCommand(plugin, services, TeleportCommand.Type.DENY));
        register("tp", new TeleportCommand(plugin, services, TeleportCommand.Type.TP));
        register("tphere", new TeleportCommand(plugin, services, TeleportCommand.Type.TPHERE));
        register("spawn", new SpawnCommand(plugin, services, SpawnCommand.Type.SPAWN));
        register("setspawn", new SpawnCommand(plugin, services, SpawnCommand.Type.SETSPAWN));
        register("rtp", new RtpCommand(plugin, services));
        register("sethome", new HomeCommand(plugin, services, HomeCommand.Type.SET));
        register("home", new HomeCommand(plugin, services, HomeCommand.Type.GO));
        register("delhome", new HomeCommand(plugin, services, HomeCommand.Type.DELETE));
        register("homes", new HomeCommand(plugin, services, HomeCommand.Type.LIST));
        register("ignore", new IgnoreCommand(plugin, services));
        register("tps", new TpsCommand(plugin, services));
        register("vanish", new VanishCommand(plugin, services));
        register("wipeplayer", new WipePlayerCommand(plugin, services, wipers));
    }

    private void register(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = plugin.getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
            if (executor instanceof org.bukkit.command.TabCompleter completer) {
                command.setTabCompleter(completer);
            }
        }
    }
}