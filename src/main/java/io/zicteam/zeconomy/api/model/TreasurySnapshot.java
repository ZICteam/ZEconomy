package io.zicteam.zeconomy.api.model;

public record TreasurySnapshot(String currencyId, double totalBalance, double reservedBalance, double spendableBalance) {
}
