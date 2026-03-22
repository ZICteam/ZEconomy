package io.zicteam.zeconomy.api.event;

import java.util.UUID;

public final class VaultTransactionEvent extends ZEconomyApiEvent {
    public enum Action {
        PIN_SET,
        DEPOSIT,
        WITHDRAW
    }

    private final Action action;
    private final UUID playerId;
    private final String currencyId;
    private final double amount;
    private final double vaultBalanceAfter;

    public VaultTransactionEvent(Action action, UUID playerId, String currencyId, double amount, double vaultBalanceAfter) {
        this.action = action;
        this.playerId = playerId;
        this.currencyId = currencyId;
        this.amount = amount;
        this.vaultBalanceAfter = vaultBalanceAfter;
    }

    public Action getAction() {
        return action;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getCurrencyId() {
        return currencyId;
    }

    public double getAmount() {
        return amount;
    }

    public double getVaultBalanceAfter() {
        return vaultBalanceAfter;
    }
}
