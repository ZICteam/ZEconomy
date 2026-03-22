package io.zicteam.zeconomy.api.event;

import java.util.UUID;

public final class BalanceChangeEvent extends ZEconomyApiEvent {
    private final UUID playerId;
    private final String currencyId;
    private final double oldBalance;
    private final double newBalance;
    private final String reason;

    public BalanceChangeEvent(UUID playerId, String currencyId, double oldBalance, double newBalance, String reason) {
        this.playerId = playerId;
        this.currencyId = currencyId;
        this.oldBalance = oldBalance;
        this.newBalance = newBalance;
        this.reason = reason == null ? "" : reason;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getCurrencyId() {
        return currencyId;
    }

    public double getOldBalance() {
        return oldBalance;
    }

    public double getNewBalance() {
        return newBalance;
    }

    public double getDelta() {
        return newBalance - oldBalance;
    }

    public String getReason() {
        return reason;
    }
}
