package ru.core.commands.util;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import ru.core.commands.database.DatabaseManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class WiperRegistry {
    private final List<PlayerDataWiper> wipers = new ArrayList<>();

    public WiperRegistry(DatabaseManager database, Economy economy) {
        wipers.add(player -> database.wipePlayer(player.getUniqueId()));
        if (economy != null) {
            wipers.add(player -> CompletableFuture.runAsync(() -> {
                if (economy.hasAccount(player)) {
                    economy.withdrawPlayer(player, economy.getBalance(player));
                }
            }));
        }
    }

    public CompletableFuture<Void> wipe(OfflinePlayer player) {
        CompletableFuture<Void> result = CompletableFuture.completedFuture(null);
        for (PlayerDataWiper wiper : wipers) {
            result = result.thenCompose(ignored -> wiper.wipe(player));
        }
        return result;
    }

    public interface PlayerDataWiper {
        CompletableFuture<Void> wipe(OfflinePlayer player);
    }
}