package io.zicteam.zeconomy.api.model;

import java.util.UUID;

public record TransactionLogView(long epochSec, String type, UUID actor, UUID target, String currencyId, double amount, double fee, String note) {
}
