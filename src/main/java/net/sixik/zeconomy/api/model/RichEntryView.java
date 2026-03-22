package net.sixik.zeconomy.api.model;

import java.util.UUID;

public record RichEntryView(UUID playerId, String playerName, String currencyId, double amount) {
}
