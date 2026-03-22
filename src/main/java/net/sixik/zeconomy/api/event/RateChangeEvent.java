package net.sixik.zeconomy.api.event;

public final class RateChangeEvent extends ZEconomyApiEvent {
    public enum Action {
        SET,
        REMOVE,
        RESET
    }

    private final Action action;
    private final String fromCurrencyId;
    private final String toCurrencyId;
    private final double rate;

    public RateChangeEvent(Action action, String fromCurrencyId, String toCurrencyId, double rate) {
        this.action = action;
        this.fromCurrencyId = fromCurrencyId == null ? "" : fromCurrencyId;
        this.toCurrencyId = toCurrencyId == null ? "" : toCurrencyId;
        this.rate = rate;
    }

    public Action getAction() {
        return action;
    }

    public String getFromCurrencyId() {
        return fromCurrencyId;
    }

    public String getToCurrencyId() {
        return toCurrencyId;
    }

    public double getRate() {
        return rate;
    }
}
