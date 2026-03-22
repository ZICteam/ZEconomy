package io.zicteam.zeconomy.api;

import java.util.List;
import java.util.UUID;
import io.zicteam.zeconomy.api.model.ApiInfo;
import io.zicteam.zeconomy.api.model.ApiResult;
import io.zicteam.zeconomy.api.model.CurrencyView;
import io.zicteam.zeconomy.api.model.DailyRewardView;
import io.zicteam.zeconomy.api.model.PlayerEconomySnapshot;
import io.zicteam.zeconomy.api.model.RateView;
import io.zicteam.zeconomy.api.model.RichEntryView;
import io.zicteam.zeconomy.api.model.RuntimeStatusView;
import io.zicteam.zeconomy.api.model.TransactionLogView;
import io.zicteam.zeconomy.api.model.TreasurySnapshot;

public interface ZEconomyApi {
    ApiInfo getInfo();
    ApiResult<List<CurrencyView>> getCurrencies();
    ApiResult<CurrencyView> getCurrency(String currencyId);
    ApiResult<PlayerEconomySnapshot> getPlayerSnapshot(UUID playerId);
    ApiResult<TreasurySnapshot> getTreasurySnapshot(String currencyId);
    ApiResult<List<TreasurySnapshot>> getTreasurySnapshots();
    ApiResult<List<RateView>> getRates();
    ApiResult<RateView> getRate(String fromCurrencyId, String toCurrencyId);
    ApiResult<List<RichEntryView>> getTopRich(String currencyId, int limit, boolean includeVault);
    ApiResult<List<TransactionLogView>> getRecentLogs(int limit);
    ApiResult<RuntimeStatusView> getRuntimeStatus();
    ApiResult<Double> addBalance(UUID playerId, String currencyId, double amount);
    ApiResult<Double> setBalance(UUID playerId, String currencyId, double amount);
    ApiResult<Double> transfer(UUID fromPlayerId, UUID toPlayerId, String currencyId, double amount);
    ApiResult<Double> depositBank(UUID playerId, String currencyId, double amount);
    ApiResult<Double> withdrawBank(UUID playerId, String currencyId, double amount);
    ApiResult<Boolean> setVaultPin(UUID playerId, String pin);
    ApiResult<Double> depositVault(UUID playerId, String pin, String currencyId, double amount);
    ApiResult<Double> withdrawVault(UUID playerId, String pin, String currencyId, double amount);
    ApiResult<DailyRewardView> claimDaily(UUID playerId);
    ApiResult<Double> exchange(UUID playerId, String fromCurrencyId, String toCurrencyId, double amount);
    ApiResult<Double> setTreasuryBalance(String currencyId, double amount);
    ApiResult<Double> addTreasuryBalance(String currencyId, double amount);
    ApiResult<Double> takeTreasuryBalance(String currencyId, double amount);
    ApiResult<Boolean> setRate(String fromCurrencyId, String toCurrencyId, double rate);
    ApiResult<Boolean> removeRate(String fromCurrencyId, String toCurrencyId);
    ApiResult<Boolean> resetRates();
}
