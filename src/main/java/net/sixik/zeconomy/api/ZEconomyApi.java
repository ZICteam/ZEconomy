package net.sixik.zeconomy.api;

import java.util.List;
import java.util.UUID;
import net.sixik.zeconomy.api.model.ApiInfo;
import net.sixik.zeconomy.api.model.ApiResult;
import net.sixik.zeconomy.api.model.CurrencyView;
import net.sixik.zeconomy.api.model.DailyRewardView;
import net.sixik.zeconomy.api.model.PlayerEconomySnapshot;
import net.sixik.zeconomy.api.model.RateView;
import net.sixik.zeconomy.api.model.RichEntryView;
import net.sixik.zeconomy.api.model.RuntimeStatusView;
import net.sixik.zeconomy.api.model.TransactionLogView;
import net.sixik.zeconomy.api.model.TreasurySnapshot;

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
