package io.zicteam.zeconomy.system;

import java.util.Map;
import java.util.UUID;

final class VaultStateFacade {
    private final VaultLedgerState ledger = new VaultLedgerState();

    boolean setPin(UUID playerId, String pin) {
        return ledger.setPin(playerId, pin);
    }

    boolean hasPin(UUID playerId) {
        return ledger.hasPin(playerId);
    }

    boolean matchesPin(UUID playerId, String pin) {
        return ledger.matchesPin(playerId, pin);
    }

    double getBalance(UUID playerId, String currency) {
        return ledger.getBalance(playerId, currency);
    }

    Map<String, Double> getAllBalances(UUID playerId) {
        return ledger.getAllBalances(playerId);
    }

    void addBalance(UUID playerId, String currency, double amount) {
        if (playerId == null || currency == null || currency.isBlank() || amount == 0.0D) {
            return;
        }
        ledger.addBalance(playerId, currency, amount);
    }

    void setBalance(UUID playerId, String currency, double amount) {
        ledger.setBalance(playerId, currency, amount);
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
