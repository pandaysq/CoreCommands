package ru.core.commands.database;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import ru.core.commands.config.ConfigManager;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DatabaseManager {
    private final JavaPlugin plugin;
    private final ConfigManager config;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "CoreCommands-SQLite");
        thread.setDaemon(true);
        return thread;
    });
    private Connection connection;

    public DatabaseManager(JavaPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void initialize() {
        executor.execute(() -> {
            try {
                File file = new File(plugin.getDataFolder(), config.get().getString("database.file", "corecommands.db"));
                if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                    throw new SQLException("Cannot create plugin data folder");
                }
                connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("""
                            CREATE TABLE IF NOT EXISTS punishments (
                                id INTEGER PRIMARY KEY AUTOINCREMENT,
                                uuid TEXT NOT NULL,
                                name TEXT NOT NULL,
                                ip TEXT,
                                type TEXT NOT NULL,
                                reason TEXT NOT NULL,
                                operator TEXT NOT NULL,
                                created_at INTEGER NOT NULL,
                                expires_at INTEGER,
                                active INTEGER NOT NULL
                            )
                            """);
                    statement.executeUpdate("""
                            CREATE TABLE IF NOT EXISTS homes (
                                uuid TEXT NOT NULL,
                                name TEXT NOT NULL,
                                world TEXT NOT NULL,
                                x REAL NOT NULL,
                                y REAL NOT NULL,
                                z REAL NOT NULL,
                                yaw REAL NOT NULL,
                                pitch REAL NOT NULL,
                                PRIMARY KEY (uuid, name)
                            )
                            """);
                    statement.executeUpdate("""
                            CREATE TABLE IF NOT EXISTS ignores (
                                owner_uuid TEXT NOT NULL,
                                target_uuid TEXT NOT NULL,
                                PRIMARY KEY (owner_uuid, target_uuid)
                            )
                            """);
                }
            } catch (SQLException exception) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Не удалось открыть SQLite", exception);
            }
        });
    }

    public CompletableFuture<Void> insertPunishment(Punishment punishment) {
        return submit(() -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO punishments(uuid,name,ip,type,reason,operator,created_at,expires_at,active)
                    VALUES(?,?,?,?,?,?,?,?,1)
                    """)) {
                statement.setString(1, punishment.uuid().toString());
                statement.setString(2, punishment.name());
                statement.setString(3, punishment.ip());
                statement.setString(4, punishment.type());
                statement.setString(5, punishment.reason());
                statement.setString(6, punishment.operator());
                statement.setLong(7, punishment.createdAt());
                if (punishment.expiresAt() == null) statement.setNull(8, Types.INTEGER);
                else statement.setLong(8, punishment.expiresAt());
                statement.executeUpdate();
            }
            return null;
        });
    }

    public CompletableFuture<Punishment> activePunishment(String uuid, String type) {
        return submit(() -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT * FROM punishments
                    WHERE uuid=? AND type=? AND active=1
                    ORDER BY id DESC LIMIT 1
                    """)) {
                statement.setString(1, uuid);
                statement.setString(2, type);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) return null;
                    Long expires = result.getObject("expires_at", Long.class);
                    if (expires != null && expires <= System.currentTimeMillis()) {
                        deactivate(result.getInt("id"));
                        return null;
                    }
                    return new Punishment(UUID.fromString(result.getString("uuid")), result.getString("name"),
                            result.getString("ip"), result.getString("type"), result.getString("reason"),
                            result.getString("operator"), result.getLong("created_at"), expires);
                }
            }
        });
    }

    public CompletableFuture<Punishment> activeIpPunishment(String ip) {
        return submit(() -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT * FROM punishments
                    WHERE ip=? AND type='IPBAN' AND active=1
                    ORDER BY id DESC LIMIT 1
                    """)) {
                statement.setString(1, ip);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) return null;
                    Long expires = result.getObject("expires_at", Long.class);
                    if (expires != null && expires <= System.currentTimeMillis()) {
                        deactivate(result.getInt("id"));
                        return null;
                    }
                    return new Punishment(UUID.fromString(result.getString("uuid")), result.getString("name"),
                            result.getString("ip"), result.getString("type"), result.getString("reason"),
                            result.getString("operator"), result.getLong("created_at"), expires);
                }
            }
        });
    }

    public CompletableFuture<Void> removePunishment(String name, String type) {
        return submit(() -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE punishments SET active=0 WHERE lower(name)=lower(?) AND type=? AND active=1")) {
                statement.setString(1, name);
                statement.setString(2, type);
                statement.executeUpdate();
            }
            return null;
        });
    }

    private void deactivate(int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE punishments SET active=0 WHERE id=?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    public CompletableFuture<Void> saveHome(UUID uuid, String name, Location location) {
        return submit(() -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO homes(uuid,name,world,x,y,z,yaw,pitch) VALUES(?,?,?,?,?,?,?,?)
                    ON CONFLICT(uuid,name) DO UPDATE SET world=excluded.world,x=excluded.x,y=excluded.y,
                    z=excluded.z,yaw=excluded.yaw,pitch=excluded.pitch
                    """)) {
                statement.setString(1, uuid.toString()); statement.setString(2, name);
                statement.setString(3, location.getWorld().getName()); statement.setDouble(4, location.getX());
                statement.setDouble(5, location.getY()); statement.setDouble(6, location.getZ());
                statement.setFloat(7, location.getYaw()); statement.setFloat(8, location.getPitch());
                statement.executeUpdate();
            }
            return null;
        });
    }

    public CompletableFuture<Location> home(UUID uuid, String name) {
        return submit(() -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM homes WHERE uuid=? AND name=?")) {
                statement.setString(1, uuid.toString()); statement.setString(2, name);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) return null;
                    org.bukkit.World world = plugin.getServer().getWorld(result.getString("world"));
                    if (world == null) return null;
                    return new Location(world, result.getDouble("x"), result.getDouble("y"), result.getDouble("z"),
                            result.getFloat("yaw"), result.getFloat("pitch"));
                }
            }
        });
    }

    public CompletableFuture<List<String>> homes(UUID uuid) {
        return submit(() -> {
            List<String> names = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("SELECT name FROM homes WHERE uuid=? ORDER BY name")) {
                statement.setString(1, uuid.toString());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) names.add(result.getString("name"));
                }
            }
            return names;
        });
    }

    public CompletableFuture<Void> deleteHome(UUID uuid, String name) {
        return submit(() -> {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM homes WHERE uuid=? AND name=?")) {
                statement.setString(1, uuid.toString()); statement.setString(2, name); statement.executeUpdate();
            }
            return null;
        });
    }

    public CompletableFuture<Boolean> toggleIgnore(UUID owner, UUID target) {
        return submit(() -> {
            try (PreparedStatement exists = connection.prepareStatement(
                    "SELECT 1 FROM ignores WHERE owner_uuid=? AND target_uuid=?")) {
                exists.setString(1, owner.toString()); exists.setString(2, target.toString());
                try (ResultSet result = exists.executeQuery()) {
                    if (result.next()) {
                        try (PreparedStatement delete = connection.prepareStatement(
                                "DELETE FROM ignores WHERE owner_uuid=? AND target_uuid=?")) {
                            delete.setString(1, owner.toString()); delete.setString(2, target.toString()); delete.executeUpdate();
                        }
                        return false;
                    }
                }
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO ignores(owner_uuid,target_uuid) VALUES(?,?)")) {
                insert.setString(1, owner.toString()); insert.setString(2, target.toString()); insert.executeUpdate();
            }
            return true;
        });
    }

    public CompletableFuture<Boolean> isIgnored(UUID owner, UUID target) {
        return submit(() -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT 1 FROM ignores WHERE owner_uuid=? AND target_uuid=?")) {
                statement.setString(1, owner.toString()); statement.setString(2, target.toString());
                try (ResultSet result = statement.executeQuery()) { return result.next(); }
            }
        });
    }

    public CompletableFuture<Void> wipePlayer(UUID uuid) {
        return submit(() -> {
            try (PreparedStatement a = connection.prepareStatement("DELETE FROM homes WHERE uuid=?");
                 PreparedStatement b = connection.prepareStatement("DELETE FROM ignores WHERE owner_uuid=? OR target_uuid=?");
                 PreparedStatement c = connection.prepareStatement("DELETE FROM punishments WHERE uuid=?")) {
                a.setString(1, uuid.toString()); a.executeUpdate();
                b.setString(1, uuid.toString()); b.setString(2, uuid.toString()); b.executeUpdate();
                c.setString(1, uuid.toString()); c.executeUpdate();
            }
            return null;
        });
    }

    private <T> CompletableFuture<T> submit(SqlSupplier<T> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (connection == null || connection.isClosed()) throw new SQLException("Database is not ready");
                return supplier.get();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }, executor);
    }

    public void close() {
        executor.execute(() -> {
            try {
                if (connection != null && !connection.isClosed()) connection.close();
            } catch (SQLException exception) {
                plugin.getLogger().warning("Не удалось закрыть SQLite: " + exception.getMessage());
            }
        });
        executor.shutdown();
    }

    @FunctionalInterface
    private interface SqlSupplier<T> {
        T get() throws Exception;
    }

    public record Punishment(UUID uuid, String name, String ip, String type, String reason,
                             String operator, long createdAt, Long expiresAt) {}
}