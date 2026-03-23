package io.zicteam.zeconomy.system;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

final class BankStateFacade {
    private final BankLedgerState ledger = new BankLedgerState();

    void addDepositState(UUID playerId, String currency, double amount) {
        if (playerId == null || currency == null || currency.isBlank() || amount == 0.0D) {
            return;
        }
        ledger.addDeposit(playerId, currency, amount);
        if (amount > 0.0D) {
            ledger.setLastHourlyPayoutEpochSec(
                playerId,
                Math.min(ledger.getLastHourlyPayoutEpochSec(playerId, Long.MAX_VALUE), Instant.now().getEpochSecond())
            );
        }
    }

    void setDepositState(UUID playerId, String currency, double amount) {
        ledger.setDeposit(playerId, currency, amount);
    }

    Map<String, Double> getAccountDeposits(UUID playerId) {
        return ledger.getAccountDeposits(playerId);
    }

    long getLastHourlyPayout(UUID playerId, long defaultValue) {
        return ledger.getLastHourlyPayoutEpochSec(playerId, defaultValue);
    }

    void setLastHourlyPayout(UUID playerId, long epochSec) {
        ledger.setLastHourlyPayoutEpochSec(playerId, epochSec);
    }

    double getDeposited(UUID playerId, String currency) {
        return ledger.getDeposited(playerId, currency);
    }

    Map<String, Double> getAllDeposits(UUID playerId) {
        return ledger.getAllDeposits(playerId);
    }

    double getTotalDeposited(String currency) {
        return ledger.getTotalDeposited(currency);
    }

    double getServerSpendable(double serverBalance, String currency) {
        return ledger.getServerSpendable(serverBalance, currency);
    }

    void clear() {
        ledger.clear();
    }

    void readFrom(net.minecraft.nbt.CompoundTag root) {
        ledger.readFrom(root);
    }

    void writeTo(net.minecraft.nbt.CompoundTag root) {
        ledger.writeTo(root);
    }
}
