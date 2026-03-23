package io.zicteam.zeconomy.system;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.item.ItemStack;
import io.zicteam.zeconomy.ZEconomy;
import io.zicteam.zeconomy.utils.CurrencyHelper;

public class ExtraEconomyData {
    public static final int MAX_LOGS = 500;

    private final EconomyStateRoot state = new EconomyStateRoot();

    public static ExtraEconomyData load(Path path) {
        ExtraEconomyData data = new ExtraEconomyData();
        if (path == null || !Files.exists(path)) {
            return data;
        }
        try {
            CompoundTag root = NbtIo.readCompressed(path.toFile());
            if (root != null) {
                data.state.read(root);
            }
        } catch (IOException e) {
            ZEconomy.printStackTrace("Failed to load extra economy data", e);
        }
        return data;
    }

    public void save(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.createDirectories(path.getParent());
            NbtIo.writeCompressed(state.write(), path.toFile());
        } catch (IOException e) {
            ZEconomy.printStackTrace("Failed to save extra economy data", e);
        }
    }

    public CompoundTag serialize() {
        return state.write();
    }

    public static ExtraEconomyData deserialize(CompoundTag root) {
        ExtraEconomyData data = new ExtraEconomyData();
        if (root != null) {
            data.state.read(root);
        }
        return data;
    }

    public void ensureDefaultRates() {
        state.ensureDefaultRates();
    }

    public double getRate(String from, String to) {
        return state.getRate(from, to);
    }

    public void setRate(String from, String to, double rate) {
        state.setRate(from, to, rate);
    }

    public boolean removeRate(String from, String to) {
        return state.removeRate(from, to);
    }

    public Map<String, Double> getAllRates() {
        return state.getAllRates();
    }

    public void clearRates() {
        state.clearRates();
    }

    public boolean setVaultPin(UUID playerId, String pin) {
        return state.setVaultPin(playerId, pin);
    }

    public boolean hasVaultPin(UUID playerId) {
        return state.hasVaultPin(playerId);
    }

    public boolean matchesVaultPin(UUID playerId, String pin) {
        return state.matchesVaultPin(playerId, pin);
    }

    public double getVaultBalance(UUID playerId, String currency) {
        return state.getVaultBalance(playerId, currency);
    }

    public Map<String, Double> getAllVaultBalances(UUID playerId) {
        return state.getAllVaultBalances(playerId);
    }

    public int getDailyStreak(UUID playerId) {
        return state.getDailyStreak(playerId);
    }

    public long getDailyLastClaimDay(UUID playerId) {
        return state.getDailyLastClaimDay(playerId);
    }

    public void addBankDepositState(UUID playerId, String currency, double amount) {
        state.addBankDepositState(playerId, currency, amount);
    }

    public void setBankDepositState(UUID playerId, String currency, double amount) {
        state.setBankDepositState(playerId, currency, amount);
    }

    public void addVaultDepositState(UUID playerId, String currency, double amount) {
        state.addVaultDepositState(playerId, currency, amount);
    }

    public void setVaultDepositState(UUID playerId, String currency, double amount) {
        state.setVaultDepositState(playerId, currency, amount);
    }

    public DailyClaimResult applyDailyClaimState(UUID playerId, long claimDay, int nextStreak, double zReward, double bReward) {
        return state.applyDailyClaimState(playerId, claimDay, nextStreak, zReward, bReward);
    }

    public boolean beginInterestSweep(long now) {
        return state.beginInterestSweep(now);
    }

    public Map<String, Double> getBankAccountDeposits(UUID playerId) {
        return state.getBankAccountDeposits(playerId);
    }

    public long getBankLastHourlyPayout(UUID playerId, long defaultValue) {
        return state.getBankLastHourlyPayout(playerId, defaultValue);
    }

    public void setBankLastHourlyPayout(UUID playerId, long epochSec) {
        state.setBankLastHourlyPayout(playerId, epochSec);
    }

    public void sendMail(UUID target, ItemStack stack) {
        state.sendMail(target, stack);
    }

    public List<ItemStack> claimMail(UUID playerId) {
        return state.claimMail(playerId);
    }

    public int pendingMailCount(UUID playerId) {
        return state.pendingMailCount(playerId);
    }

    public boolean hasClaimedDailyToday(UUID playerId) {
        return state.hasClaimedDailyToday(playerId);
    }

    public long getLastExportEpochSec() {
        return state.getLastExportEpochSec();
    }

    public boolean shouldExport(long now, int intervalSeconds) {
        return state.shouldExport(now, intervalSeconds);
    }

    public void markExported(long now) {
        state.markExported(now);
    }

    public int getLogCount() {
        return state.getLogCount();
    }

    public List<TransactionRecord> getRecentLogs(int limit) {
        return state.getRecentLogs(limit);
    }

    public double getDeposited(UUID playerId, String currency) {
        return state.getDeposited(playerId, currency);
    }

    public Map<String, Double> getAllDeposits(UUID playerId) {
        return state.getAllDeposits(playerId);
    }

    public double getTotalDeposited(String currency) {
        return state.getTotalDeposited(currency);
    }

    public double getServerSpendable(String currency) {
        return getServerSpendableBalance(currency);
    }

    private double getServerSpendableBalance(String currency) {
        double serverBalance = CurrencyHelper.getPlayerCurrencyServerData().getBalance(CurrencyHelper.getServerAccountUUID(), currency).value;
        return state.getServerSpendable(serverBalance, currency);
    }

    public void recordTransaction(String type, UUID actor, UUID target, String currency, double amount, double fee, String note) {
        state.recordTransaction(type, actor, target, currency, amount, fee, note);
    }

    public record DailyClaimResult(boolean success, double zReward, double bReward, int streak) {
    }

    public record RichEntry(UUID playerId, String playerName, String currency, double amount) {
    }

    public record TransactionRecord(long epochSec, String type, UUID actor, UUID target, String currency, double amount, double fee, String note) {
    }

}
