package net.sixik.zeconomy.api.event;

import java.util.UUID;

public final class ExchangeEvent extends ZEconomyApiEvent {
    private final UUID playerId;
    private final String fromCurrencyId;
    private final String toCurrencyId;
    private final double sourceAmount;
    private final double targetAmount;
    private final double fee;

    public ExchangeEvent(UUID playerId, String fromCurrencyId, String toCurrencyId, double sourceAmount, double targetAmount, double fee) {
        this.playerId = playerId;
        this.fromCurrencyId = fromCurrencyId;
        this.toCurrencyId = toCurrencyId;
        this.sourceAmount = sourceAmount;
        this.targetAmount = targetAmount;
        this.fee = fee;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getFromCurrencyId() {
        return fromCurrencyId;
    }

    public String getToCurrencyId() {
        return toCurrencyId;
    }

    public double getSourceAmount() {
        return sourceAmount;
    }

    public double getTargetAmount() {
        return targetAmount;
    }

    public double getFee() {
        return fee;
    }
}
