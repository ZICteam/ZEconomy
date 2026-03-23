package io.zicteam.zeconomy.system;

import io.zicteam.zeconomy.CustomPlayerData;
import io.zicteam.zeconomy.ZEconomy;
import io.zicteam.zeconomy.ZEconomyEvents;
import io.zicteam.zeconomy.config.EconomyConfig;
import io.zicteam.zeconomy.currencies.data.CurrencyData;
import io.zicteam.zeconomy.currencies.data.CurrencyPlayerData;
import io.zicteam.zeconomy.currencies.compat.VaultBridge;
import io.zicteam.zeconomy.utils.CurrencyHelper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

public final class AdminReportService {
    private static final DateTimeFormatter EXPORT_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private AdminReportService() {
    }

    public static StatusReport status(MinecraftServer server) {
        EconomySnapshotReadService.RuntimeSnapshot runtime = EconomySnapshotReadService.runtime();
        return new StatusReport(
            EconomyConfig.STORAGE_MODE.get(),
            storageTarget(server),
            CurrencyHelper.currencyDataPath(server),
            CurrencyHelper.playerDataPath(server),
            CurrencyHelper.customDataPath(server),
            CurrencyHelper.extraDataPath(server),
            EconomyConfig.ENABLE_VAULT_BRIDGE.get() ? "on" : "off",
            VaultBridge.isAvailable() ? "available" : "offline",
            VaultBridge.getProviderName(),
            EconomyConfig.VAULT_SYNC_CURRENCY_ID.get(),
            server.getPlayerList().getPlayerCount(),
            ZEconomyEvents.pendingVaultSyncCount(),
            runtime.exchangeRateCount(),
            CurrencyData.SERVER.currencies.size()
        );
    }

    public static ExportStatusReport exportStatus(MinecraftServer server) {
        Path exportPath = CurrencyHelper.exportJsonPath(server);
        boolean exists = Files.exists(exportPath);
        String size = exists ? Long.toString(safeFileSize(exportPath)) : "0";
        String modified = exists ? EXPORT_TS.format(Instant.ofEpochMilli(safeLastModified(exportPath))) : "never";
        return new ExportStatusReport(exportPath, yesNo(exists), size, modified, EconomyConfig.EXPORT_INTERVAL_SECONDS.get());
    }

    public static DoctorReport doctor(MinecraftServer server) {
        boolean hasZ = CurrencyData.SERVER.currencies.stream().anyMatch(c -> "z_coin".equals(c.getName()));
        boolean hasB = CurrencyData.SERVER.currencies.stream().anyMatch(c -> "b_coin".equals(c.getName()));
        boolean currencyFile = Files.exists(CurrencyHelper.currencyDataPath(server));
        boolean playerFile = Files.exists(CurrencyHelper.playerDataPath(server));
        boolean customFile = Files.exists(CurrencyHelper.customDataPath(server));
        boolean extraFile = Files.exists(CurrencyHelper.extraDataPath(server));
        String vaultHealth = !EconomyConfig.ENABLE_VAULT_BRIDGE.get()
            ? "disabled"
            : (VaultBridge.isAvailable() ? "ok" : "offline");
        return new DoctorReport(
            hasZ ? "ok" : "missing",
            hasB ? "ok" : "missing",
            EconomyConfig.STORAGE_MODE.get(),
            storageHealth(server),
            yesNo(currencyFile),
            yesNo(playerFile),
            yesNo(customFile),
            yesNo(extraFile),
            vaultHealth,
            ZEconomyEvents.pendingVaultSyncCount(),
            VaultBridge.getProviderName()
        );
    }

    public static PlayerInspectReport inspect(ServerPlayer player) {
        String syncCurrency = EconomyConfig.VAULT_SYNC_CURRENCY_ID.get();
        double syncBalance = CurrencyHelper.getPlayerCurrencyServerData().getBalance(player, syncCurrency).value;
        CompoundTag custom = CustomPlayerData.SERVER.getPlayerCustomData(player).nbt;
        EconomySnapshotReadService.PlayerSnapshot snapshot = EconomySnapshotReadService.player(player.getUUID());
        LinkedList<PlayerWalletLine> wallet = new LinkedList<>();
        for (CurrencyPlayerData.PlayerCurrency currency : CurrencyHelper.getPlayerCurrencyServerData().getPlayersCurrency(player)) {
            wallet.add(new PlayerWalletLine(currency.currency.getName(), currency.balance, yesNo(currency.isLocked)));
        }
        return new PlayerInspectReport(
            player.getName().getString(),
            player.getUUID().toString(),
            snapshot.pendingMail(),
            snapshot.dailyStreak(),
            yesNo(snapshot.hasVaultPin()),
            syncCurrency,
            syncBalance,
            wallet,
            sortedCopy(snapshot.bankBalances()),
            sortedCopy(snapshot.vaultBalances()),
            custom.getDouble("bank_z_coin"),
            custom.getDouble("bank_b_coin"),
            custom.getDouble("vault_z_coin"),
            custom.getDouble("vault_b_coin"),
            custom.getInt("mail_pending"),
            custom.getInt("daily_streak")
        );
    }

    public static String storageTarget(MinecraftServer server) {
        String mode = EconomyConfig.STORAGE_MODE.get();
        if ("json".equalsIgnoreCase(mode)) {
            return server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve("zeconomy").resolve(EconomyConfig.JSON_FILE_NAME.get()).toString();
        }
        if ("sqlite".equalsIgnoreCase(mode)) {
            return server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve("zeconomy").resolve(EconomyConfig.SQLITE_FILE_NAME.get()).toString();
        }
        if ("mysql".equalsIgnoreCase(mode)) {
            return EconomyConfig.MYSQL_HOST.get() + ":" + EconomyConfig.MYSQL_PORT.get() + "/" + EconomyConfig.MYSQL_DATABASE.get() + " [" + EconomyConfig.MYSQL_TABLE.get() + "]";
        }
        return server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve("zeconomy").toString();
    }

    private static String storageHealth(MinecraftServer server) {
        String mode = EconomyConfig.STORAGE_MODE.get();
        if ("json".equalsIgnoreCase(mode)) {
            return Files.exists(server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve("zeconomy").resolve(EconomyConfig.JSON_FILE_NAME.get())) ? "ok" : "pending";
        }
        if ("sqlite".equalsIgnoreCase(mode)) {
            return Files.exists(server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve("zeconomy").resolve(EconomyConfig.SQLITE_FILE_NAME.get())) ? "ok" : "pending";
        }
        if ("mysql".equalsIgnoreCase(mode)) {
            return "external";
        }
        return Files.exists(CurrencyHelper.currencyDataPath(server)) ? "ok" : "pending";
    }

    private static long safeFileSize(Path path) {
        try {
            return Files.size(path);
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static long safeLastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }

    private static Map<String, Double> sortedCopy(Map<String, Double> values) {
        return values.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), LinkedHashMap::putAll);
    }

    public record StatusReport(
        String storageMode,
        String storageTarget,
        Path currencyPath,
        Path playerPath,
        Path customPath,
        Path extraPath,
        String vaultToggle,
        String vaultAvailability,
        String vaultProvider,
        String vaultSyncCurrency,
        int onlinePlayers,
        int pendingVaultSyncs,
        int exchangeRates,
        int currencyCount
    ) {
    }

    public record ExportStatusReport(Path exportPath, String exists, String sizeBytes, String modifiedAt, int intervalSeconds) {
    }

    public record DoctorReport(
        String zCoinStatus,
        String bCoinStatus,
        String storageMode,
        String storageHealth,
        String currencyFile,
        String playerFile,
        String customFile,
        String extraFile,
        String vaultHealth,
        int pendingVaultSyncs,
        String vaultProvider
    ) {
    }

    public record PlayerWalletLine(String currencyId, double balance, String locked) {
    }

    public record PlayerInspectReport(
        String playerName,
        String playerId,
        int pendingMail,
        int dailyStreak,
        String hasVaultPin,
        String syncCurrency,
        double syncBalance,
        LinkedList<PlayerWalletLine> wallet,
        Map<String, Double> bankBalances,
        Map<String, Double> vaultBalances,
        double mirrorBankZ,
        double mirrorBankB,
        double mirrorVaultZ,
        double mirrorVaultB,
        int mirrorMailPending,
        int mirrorDailyStreak
    ) {
    }
}
