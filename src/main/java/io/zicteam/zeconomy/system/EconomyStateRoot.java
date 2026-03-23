package io.zicteam.zeconomy.system;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

final class EconomyStateRoot {
    private final BankStateFacade bankState = new BankStateFacade();
    private final MailboxSubsystem mailboxSubsystem = new MailboxSubsystem();
    private final RateSubsystem rateSubsystem = new RateSubsystem();
    private final VaultStateFacade vaultState = new VaultStateFacade();
    private final DailyRewardFacade dailyState = new DailyRewardFacade();
    private final TransactionLogSubsystem transactionLogSubsystem = new TransactionLogSubsystem();
    private final ExportRuntimeState exportRuntimeState = new ExportRuntimeState();
    private long nextInterestSweepEpochSec = 0L;

    void ensureDefaultRates() {
        rateSubsystem.ensureDefaultRates();
    }

    double getRate(String from, String to) {
        return rateSubsystem.getRate(from, to);
    }

    void setRate(String from, String to, double rate) {
        rateSubsystem.setRate(from, to, rate);
    }

    boolean removeRate(String from, String to) {
        return rateSubsystem.removeRate(from, to);
    }

    Map<String, Double> getAllRates() {
        return rateSubsystem.getAllRates();
    }

    void clearRates() {
        rateSubsystem.clearRates();
    }

    boolean setVaultPin(UUID playerId, String pin) {
        return vaultState.setPin(playerId, pin);
    }

    boolean hasVaultPin(UUID playerId) {
        return vaultState.hasPin(playerId);
    }

    boolean matchesVaultPin(UUID playerId, String pin) {
        return vaultState.matchesPin(playerId, pin);
    }

    double getVaultBalance(UUID playerId, String currency) {
        return vaultState.getBalance(playerId, currency);
    }

    Map<String, Double> getAllVaultBalances(UUID playerId) {
        return vaultState.getAllBalances(playerId);
    }

    int getDailyStreak(UUID playerId) {
        return dailyState.getStreak(playerId);
    }

    long getDailyLastClaimDay(UUID playerId) {
        return dailyState.getLastClaimDay(playerId);
    }

    void addBankDepositState(UUID playerId, String currency, double amount) {
        bankState.addDepositState(playerId, currency, amount);
    }

    void setBankDepositState(UUID playerId, String currency, double amount) {
        bankState.setDepositState(playerId, currency, amount);
    }

    void addVaultDepositState(UUID playerId, String currency, double amount) {
        vaultState.addBalance(playerId, currency, amount);
    }

    void setVaultDepositState(UUID playerId, String currency, double amount) {
        vaultState.setBalance(playerId, currency, amount);
    }

    ExtraEconomyData.DailyClaimResult applyDailyClaimState(UUID playerId, long claimDay, int nextStreak, double zReward, double bReward) {
        return dailyState.applyClaim(playerId, claimDay, nextStreak, zReward, bReward);
    }

    boolean beginInterestSweep(long now) {
        if (now < nextInterestSweepEpochSec) {
            return false;
        }
        nextInterestSweepEpochSec = now + 30L;
        return true;
    }

    Map<String, Double> getBankAccountDeposits(UUID playerId) {
        return bankState.getAccountDeposits(playerId);
    }

    long getBankLastHourlyPayout(UUID playerId, long defaultValue) {
        return bankState.getLastHourlyPayout(playerId, defaultValue);
    }

    void setBankLastHourlyPayout(UUID playerId, long epochSec) {
        bankState.setLastHourlyPayout(playerId, epochSec);
    }

    void sendMail(UUID target, ItemStack stack) {
        mailboxSubsystem.send(target, stack);
    }

    java.util.List<ItemStack> claimMail(UUID playerId) {
        return mailboxSubsystem.claim(playerId);
    }

    int pendingMailCount(UUID playerId) {
        return mailboxSubsystem.pendingCount(playerId);
    }

    boolean hasClaimedDailyToday(UUID playerId) {
        return dailyState.hasClaimedToday(playerId);
    }

    long getLastExportEpochSec() {
        return exportRuntimeState.getLastExportEpochSec();
    }

    boolean shouldExport(long now, int intervalSeconds) {
        return exportRuntimeState.shouldExport(now, intervalSeconds);
    }

    void markExported(long now) {
        exportRuntimeState.markExported(now);
    }

    int getLogCount() {
        return transactionLogSubsystem.size();
    }

    java.util.List<ExtraEconomyData.TransactionRecord> getRecentLogs(int limit) {
        return transactionLogSubsystem.recent(limit);
    }

    double getDeposited(UUID playerId, String currency) {
        return bankState.getDeposited(playerId, currency);
    }

    Map<String, Double> getAllDeposits(UUID playerId) {
        return bankState.getAllDeposits(playerId);
    }

    double getTotalDeposited(String currency) {
        return bankState.getTotalDeposited(currency);
    }

    double getServerSpendable(double serverBalance, String currency) {
        return bankState.getServerSpendable(serverBalance, currency);
    }

    void recordTransaction(String type, UUID actor, UUID target, String currency, double amount, double fee, String note) {
        transactionLogSubsystem.record(type, actor, target, currency, amount, fee, note);
    }

    CompoundTag write() {
        CompoundTag root = new CompoundTag();
        exportRuntimeState.writeTo(root);
        bankState.writeTo(root);
        vaultState.writeTo(root);
        rateSubsystem.writeTo(root);
        mailboxSubsystem.writeTo(root);
        transactionLogSubsystem.writeTo(root);
        dailyState.writeTo(root);
        return root;
    }

    void read(CompoundTag root) {
        bankState.clear();
        mailboxSubsystem.clear();
        rateSubsystem.clear();
        vaultState.clear();
        transactionLogSubsystem.clear();
        dailyState.clear();
        exportRuntimeState.readFrom(root);
        bankState.readFrom(root);
        vaultState.readFrom(root);
        rateSubsystem.readFrom(root);
        mailboxSubsystem.readFrom(root);
        transactionLogSubsystem.readFrom(root);
        dailyState.readFrom(root);
    }
}
