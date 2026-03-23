package io.zicteam.zeconomy.system;

import io.zicteam.zeconomy.ZEconomy;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class EconomySnapshotReadService {
    private EconomySnapshotReadService() {
    }

    public static PlayerSnapshot player(UUID playerId) {
        return new PlayerSnapshot(
            ZEconomy.EXTRA_DATA.pendingMailCount(playerId),
            ZEconomy.EXTRA_DATA.getDailyStreak(playerId),
            ZEconomy.EXTRA_DATA.hasVaultPin(playerId),
            ZEconomy.EXTRA_DATA.getAllDeposits(playerId),
            ZEconomy.EXTRA_DATA.getAllVaultBalances(playerId)
        );
    }

    public static RuntimeSnapshot runtime() {
        return new RuntimeSnapshot(
            ZEconomy.EXTRA_DATA.getLogCount(),
            ZEconomy.EXTRA_DATA.getLastExportEpochSec(),
            ZEconomy.EXTRA_DATA.getAllRates().size()
        );
    }

    public static double bankDeposited(UUID playerId, String currencyId) {
        return ZEconomy.EXTRA_DATA.getDeposited(playerId, currencyId);
    }

    public static Map<String, Double> bankAccountDeposits(UUID playerId) {
        return ZEconomy.EXTRA_DATA.getBankAccountDeposits(playerId);
    }

    public static long bankLastHourlyPayout(UUID playerId, long defaultValue) {
        return ZEconomy.EXTRA_DATA.getBankLastHourlyPayout(playerId, defaultValue);
    }

    public static double vaultBalance(UUID playerId, String currencyId) {
        return ZEconomy.EXTRA_DATA.getVaultBalance(playerId, currencyId);
    }

    public static boolean hasVaultPin(UUID playerId) {
        return ZEconomy.EXTRA_DATA.hasVaultPin(playerId);
    }

    public static boolean matchesVaultPin(UUID playerId, String pin) {
        return ZEconomy.EXTRA_DATA.matchesVaultPin(playerId, pin);
    }

    public static boolean hasClaimedDailyToday(UUID playerId) {
        return ZEconomy.EXTRA_DATA.hasClaimedDailyToday(playerId);
    }

    public static long dailyLastClaimDay(UUID playerId) {
        return ZEconomy.EXTRA_DATA.getDailyLastClaimDay(playerId);
    }

    public static int dailyStreak(UUID playerId) {
        return ZEconomy.EXTRA_DATA.getDailyStreak(playerId);
    }

    public static int pendingMail(UUID playerId) {
        return ZEconomy.EXTRA_DATA.pendingMailCount(playerId);
    }

    public static double exchangeRate(String fromCurrencyId, String toCurrencyId) {
        return ZEconomy.EXTRA_DATA.getRate(fromCurrencyId, toCurrencyId);
    }

    public static Map<String, Double> exchangeRates() {
        return ZEconomy.EXTRA_DATA.getAllRates();
    }

    public static int exchangeRateCount() {
        return ZEconomy.EXTRA_DATA.getAllRates().size();
    }

    public static double treasurySpendable(String currencyId) {
        return ZEconomy.EXTRA_DATA.getServerSpendable(currencyId);
    }

    public static List<ExtraEconomyData.TransactionRecord> recentLogs(int limit) {
        return ZEconomy.EXTRA_DATA.getRecentLogs(limit);
    }

    public record PlayerSnapshot(
        int pendingMail,
        int dailyStreak,
        boolean hasVaultPin,
        Map<String, Double> bankBalances,
        Map<String, Double> vaultBalances
    ) {
    }

    public record RuntimeSnapshot(
        int logCount,
        long lastExportEpochSec,
        int exchangeRateCount
    ) {
    }
}
