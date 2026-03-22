package net.sixik.zeconomy.api.model;

public record CurrencyView(String id, String symbol, double defaultValue, boolean canDelete) {
}
