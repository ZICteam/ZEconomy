package io.zicteam.zeconomy.system;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import io.zicteam.zeconomy.CustomPlayerData;
import io.zicteam.zeconomy.ZEconomy;
import io.zicteam.zeconomy.config.EconomyConfig;
import io.zicteam.zeconomy.currencies.data.CurrencyData;
import io.zicteam.zeconomy.currencies.data.CurrencyPlayerData;
import io.zicteam.zeconomy.utils.CurrencyHelper;

public final class DataStorageManager {
    private static final Gson GSON = new Gson();
    private static final String GLOBAL_ID = "global";
    private static final ExecutorService SAVE_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "ZEconomy-SaveWorker");
        thread.setDaemon(true);
        return thread;
    });
    private static final AtomicReference<PendingSave> PENDING_SAVE = new AtomicReference<>();
    private static final AtomicBoolean SAVE_WORKER_RUNNING = new AtomicBoolean(false);

    private DataStorageManager() {
    }

    public static void loadAll(MinecraftServer server) {
        StorageMode mode = StorageMode.fromConfig(EconomyConfig.STORAGE_MODE.get());
        try {
            switch (mode) {
                case JSON -> loadFromJson(server);
                case SQLITE -> loadFromSqlite(server);
                case MYSQL -> loadFromMysql(server);
                case NBT -> loadFromNbt(server);
            }
        } catch (Exception e) {
            ZEconomy.printStackTrace("Failed to load storage mode '" + mode.id + "', fallback to nbt", e);
            loadFromNbt(server);
        }
    }

    public static void saveAll(MinecraftServer server) {
        StorageMode mode = StorageMode.fromConfig(EconomyConfig.STORAGE_MODE.get());
        try {
            writeSnapshot(mode, createSnapshot(server));
        } catch (Exception e) {
            ZEconomy.printStackTrace("Failed to save storage mode '" + mode.id + "', fallback to nbt", e);
            try {
                writeSnapshot(StorageMode.NBT, createSnapshot(server));
            } catch (Exception fallbackError) {
                ZEconomy.printStackTrace("Failed to save nbt fallback", fallbackError);
            }
        }
    }

    public static void scheduleSave(MinecraftServer server) {
        StorageMode mode = StorageMode.fromConfig(EconomyConfig.STORAGE_MODE.get());
        try {
            PENDING_SAVE.set(new PendingSave(mode, createSnapshot(server)));
            ensureSaveWorker();
        } catch (Exception e) {
            ZEconomy.printStackTrace("Failed to prepare async save for storage mode '" + mode.id + "'", e);
        }
    }

    private static void loadFromNbt(MinecraftServer server) {
        CurrencyPlayerData.load(server);
        CustomPlayerData.SERVER = CustomPlayerData.Server.load(CurrencyHelper.customDataPath(server));
        CurrencyData.SERVER.reloadCurrenciesFromFile(CurrencyHelper.currencyDataPath(server));
        ZEconomy.EXTRA_DATA = ExtraEconomyData.load(CurrencyHelper.extraDataPath(server));
    }

    private static void saveToNbt(MinecraftServer server) {
        CurrencyHelper.saveCurrencyData();
        CurrencyPlayerData.save(server);
        CustomPlayerData.SERVER.save(CurrencyHelper.customDataPath(server));
        ZEconomy.EXTRA_DATA.save(CurrencyHelper.extraDataPath(server));
    }

    private static void loadFromJson(MinecraftServer server) throws Exception {
        Path path = rootDataDir(server).resolve(EconomyConfig.JSON_FILE_NAME.get());
        if (!Files.exists(path)) {
            resetEmpty(server);
            return;
        }
        String payload = Files.readString(path, StandardCharsets.UTF_8);
        applyPayload(server, payload);
    }

    private static void saveToJson(MinecraftServer server) throws IOException {
        Path path = rootDataDir(server).resolve(EconomyConfig.JSON_FILE_NAME.get());
        Files.createDirectories(path.getParent());
        Files.writeString(path, createPayload(server), StandardCharsets.UTF_8);
    }

    private static void loadFromSqlite(MinecraftServer server) throws Exception {
        Path dbPath = rootDataDir(server).resolve(EconomyConfig.SQLITE_FILE_NAME.get());
        Files.createDirectories(dbPath.getParent());
        String url = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        String payload = readSqlPayload(url);
        if (payload == null || payload.isBlank()) {
            resetEmpty(server);
            return;
        }
        applyPayload(server, payload);
    }

    private static void saveToSqlite(MinecraftServer server) throws Exception {
        Path dbPath = rootDataDir(server).resolve(EconomyConfig.SQLITE_FILE_NAME.get());
        Files.createDirectories(dbPath.getParent());
        String url = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        writeSqlPayload(url, createPayload(server));
    }

    private static void loadFromMysql(MinecraftServer server) throws Exception {
        String url = "jdbc:mysql://" + EconomyConfig.MYSQL_HOST.get() + ":" + EconomyConfig.MYSQL_PORT.get() + "/" + EconomyConfig.MYSQL_DATABASE.get() + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        String payload = readSqlPayload(url, EconomyConfig.MYSQL_USER.get(), EconomyConfig.MYSQL_PASSWORD.get());
        if (payload == null || payload.isBlank()) {
            resetEmpty(server);
            return;
        }
        applyPayload(server, payload);
    }

    private static void saveToMysql(MinecraftServer server) throws Exception {
        String url = "jdbc:mysql://" + EconomyConfig.MYSQL_HOST.get() + ":" + EconomyConfig.MYSQL_PORT.get() + "/" + EconomyConfig.MYSQL_DATABASE.get() + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        writeSqlPayload(url, EconomyConfig.MYSQL_USER.get(), EconomyConfig.MYSQL_PASSWORD.get(), createPayload(server));
    }

    private static String readSqlPayload(String url) throws Exception {
        return readSqlPayload(url, null, null);
    }

    private static String readSqlPayload(String url, String user, String password) throws Exception {
        try (Connection connection = openConnection(url, user, password)) {
            ensureTable(connection);
            String sql = "SELECT payload FROM " + tableName() + " WHERE id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, GLOBAL_ID);
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    return rs.getString("payload");
                }
            }
        }
    }

    private static void writeSqlPayload(String url, String payload) throws Exception {
        writeSqlPayload(url, null, null, payload);
    }

    private static void writeSqlPayload(String url, String user, String password, String payload) throws Exception {
        try (Connection connection = openConnection(url, user, password)) {
            ensureTable(connection);
            String sql = "INSERT INTO " + tableName() + " (id, payload, updated_at) VALUES (?, ?, ?) ON CONFLICT(id) DO UPDATE SET payload=excluded.payload, updated_at=excluded.updated_at";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, GLOBAL_ID);
                statement.setString(2, payload);
                statement.setLong(3, System.currentTimeMillis() / 1000L);
                statement.executeUpdate();
            } catch (Exception sqliteConflictSyntaxFailed) {
                String fallback = "REPLACE INTO " + tableName() + " (id, payload, updated_at) VALUES (?, ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(fallback)) {
                    statement.setString(1, GLOBAL_ID);
                    statement.setString(2, payload);
                    statement.setLong(3, System.currentTimeMillis() / 1000L);
                    statement.executeUpdate();
                }
            }
        }
    }

    private static Connection openConnection(String url, String user, String password) throws Exception {
        if (user == null || user.isBlank()) {
            return DriverManager.getConnection(url);
        }
        return DriverManager.getConnection(url, user, password == null ? "" : password);
    }

    private static void ensureTable(Connection connection) throws Exception {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName() + " (id VARCHAR(64) PRIMARY KEY, payload TEXT NOT NULL, updated_at BIGINT NOT NULL)";
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static String tableName() {
        String raw = EconomyConfig.MYSQL_TABLE.get();
        if (raw == null || raw.isBlank()) {
            return "zeconomy_storage";
        }
        String sanitized = raw.replaceAll("[^a-zA-Z0-9_]", "");
        return sanitized.isBlank() ? "zeconomy_storage" : sanitized;
    }

    private static String createPayload(MinecraftServer server) {
        return createPayload(createSnapshot(server));
    }

    private static String createPayload(StorageSnapshot snapshot) {
        JsonObject root = new JsonObject();
        root.addProperty("schema", 1);
        root.addProperty("updated_at_epoch", System.currentTimeMillis() / 1000L);
        root.addProperty("currencies_snbt", snapshot.currencies().toString());
        root.addProperty("players_snbt", snapshot.players().toString());
        root.addProperty("custom_snbt", snapshot.custom().toString());
        root.addProperty("extra_snbt", snapshot.extra().toString());
        return GSON.toJson(root);
    }

    private static StorageSnapshot createSnapshot(MinecraftServer server) {
        return new StorageSnapshot(
            server,
            CurrencyData.SERVER.serialize(),
            CurrencyPlayerData.SERVER.serialize(),
            CustomPlayerData.SERVER.serialize(),
            ZEconomy.EXTRA_DATA.serialize()
        );
    }

    private static void writeSnapshot(StorageMode mode, StorageSnapshot snapshot) throws Exception {
        switch (mode) {
            case JSON -> writeJsonSnapshot(snapshot);
            case SQLITE -> writeSqliteSnapshot(snapshot);
            case MYSQL -> writeMysqlSnapshot(snapshot);
            case NBT -> writeNbtSnapshot(snapshot);
        }
    }

    private static void writeNbtSnapshot(StorageSnapshot snapshot) throws IOException {
        Path currencyPath = CurrencyHelper.currencyDataPath(snapshot.server());
        Files.createDirectories(currencyPath.getParent());
        NbtIo.writeCompressed(snapshot.currencies(), currencyPath.toFile());

        Path playerPath = CurrencyHelper.playerDataPath(snapshot.server());
        Files.createDirectories(playerPath.getParent());
        NbtIo.writeCompressed(snapshot.players(), playerPath.toFile());

        Path customPath = CurrencyHelper.customDataPath(snapshot.server());
        Files.createDirectories(customPath.getParent());
        NbtIo.writeCompressed(snapshot.custom(), customPath.toFile());

        Path extraPath = CurrencyHelper.extraDataPath(snapshot.server());
        Files.createDirectories(extraPath.getParent());
        NbtIo.writeCompressed(snapshot.extra(), extraPath.toFile());
    }

    private static void writeJsonSnapshot(StorageSnapshot snapshot) throws IOException {
        Path path = rootDataDir(snapshot.server()).resolve(EconomyConfig.JSON_FILE_NAME.get());
        Files.createDirectories(path.getParent());
        Files.writeString(path, createPayload(snapshot), StandardCharsets.UTF_8);
    }

    private static void writeSqliteSnapshot(StorageSnapshot snapshot) throws Exception {
        Path dbPath = rootDataDir(snapshot.server()).resolve(EconomyConfig.SQLITE_FILE_NAME.get());
        Files.createDirectories(dbPath.getParent());
        String url = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        writeSqlPayload(url, createPayload(snapshot));
    }

    private static void writeMysqlSnapshot(StorageSnapshot snapshot) throws Exception {
        String url = "jdbc:mysql://" + EconomyConfig.MYSQL_HOST.get() + ":" + EconomyConfig.MYSQL_PORT.get() + "/" + EconomyConfig.MYSQL_DATABASE.get() + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        writeSqlPayload(url, EconomyConfig.MYSQL_USER.get(), EconomyConfig.MYSQL_PASSWORD.get(), createPayload(snapshot));
    }

    private static void ensureSaveWorker() {
        if (!SAVE_WORKER_RUNNING.compareAndSet(false, true)) {
            return;
        }
        SAVE_EXECUTOR.execute(() -> {
            try {
                PendingSave pending;
                while ((pending = PENDING_SAVE.getAndSet(null)) != null) {
                    try {
                        writeSnapshot(pending.mode(), pending.snapshot());
                    } catch (Exception e) {
                        ZEconomy.printStackTrace("Failed to write async snapshot for storage mode '" + pending.mode().id + "'", e);
                    }
                }
            } finally {
                SAVE_WORKER_RUNNING.set(false);
                if (PENDING_SAVE.get() != null) {
                    ensureSaveWorker();
                }
            }
        });
    }

    private static void applyPayload(MinecraftServer server, String payload) throws CommandSyntaxException {
        JsonObject root = JsonParser.parseString(payload).getAsJsonObject();
        CompoundTag currencies = parseCompound(root, "currencies_snbt");
        CompoundTag players = parseCompound(root, "players_snbt");
        CompoundTag custom = parseCompound(root, "custom_snbt");
        CompoundTag extra = parseCompound(root, "extra_snbt");

        CurrencyData.SERVER = CurrencyData.deserialize(currencies);
        CurrencyData.SERVER.server = server;
        CurrencyPlayerData.SERVER = CurrencyPlayerData.Server.deserialize(server, players);
        CustomPlayerData.SERVER = CustomPlayerData.Server.deserialize(custom);
        ZEconomy.EXTRA_DATA = ExtraEconomyData.deserialize(extra);
    }

    private static CompoundTag parseCompound(JsonObject root, String key) throws CommandSyntaxException {
        if (!root.has(key)) {
            return new CompoundTag();
        }
        String snbt = root.get(key).getAsString();
        return TagParser.parseTag(snbt);
    }

    private static void resetEmpty(MinecraftServer server) {
        CurrencyData.SERVER = new CurrencyData(new java.util.LinkedList<>());
        CurrencyData.SERVER.server = server;
        CurrencyPlayerData.SERVER = new CurrencyPlayerData.Server();
        CurrencyPlayerData.SERVER.server = server;
        CustomPlayerData.SERVER = new CustomPlayerData.Server();
        ZEconomy.EXTRA_DATA = new ExtraEconomyData();
    }

    private static Path rootDataDir(MinecraftServer server) {
        return CurrencyHelper.resolveDataRoot(server);
    }

    private record StorageSnapshot(
        MinecraftServer server,
        CompoundTag currencies,
        CompoundTag players,
        CompoundTag custom,
        CompoundTag extra
    ) {
    }

    private record PendingSave(StorageMode mode, StorageSnapshot snapshot) {
    }

    private enum StorageMode {
        NBT("nbt"),
        JSON("json"),
        SQLITE("sqlite"),
        MYSQL("mysql");

        private final String id;

        StorageMode(String id) {
            this.id = id;
        }

        private static StorageMode fromConfig(String value) {
            if (value == null) {
                return NBT;
            }
            String normalized = value.trim().toLowerCase();
            for (StorageMode mode : values()) {
                if (mode.id.equals(normalized)) {
                    return mode;
                }
            }
            return NBT;
        }
    }
}
