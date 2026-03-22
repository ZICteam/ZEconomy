package io.zicteam.zeconomy.api.event;

import java.util.UUID;

public final class BankTransactionEvent extends ZEconomyApiEvent {
    public enum Action {
        DEPOSIT,
        WITHDRAW,
        INTEREST
    }

    private final Action action;
    private final UUID playerId;
    private final String currencyId;
    private final double amount;
    private final double bankBalanceAfter;

    public BankTransactionEvent(Action action, UUID playerId, String currencyId, double amount, double bankBalanceAfter) {
        this.action = action;
        this.playerId = playerId;
        this.currencyId = currencyId;
        this.amount = amount;
        this.bankBalanceAfter = bankBalanceAfter;
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

    public double getBankBalanceAfter() {
        return bankBalanceAfter;
    }
}
