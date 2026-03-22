package io.zicteam.zeconomy.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import io.zicteam.zeconomy.CustomPlayerData;
import io.zicteam.zeconomy.ZEconomy;
import io.zicteam.zeconomy.ZEconomyEvents;
import io.zicteam.zeconomy.api.model.ApiErrorCode;
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
import io.zicteam.zeconomy.config.EconomyConfig;
import io.zicteam.zeconomy.currencies.BaseCurrency;
import io.zicteam.zeconomy.currencies.compat.VaultBridge;
import io.zicteam.zeconomy.currencies.data.CurrencyData;
import io.zicteam.zeconomy.currencies.data.CurrencyPlayerData;
import io.zicteam.zeconomy.system.EconomyOperationService;
import io.zicteam.zeconomy.system.ExtraEconomyData;
import io.zicteam.zeconomy.utils.CurrencyHelper;
import io.zicteam.zeconomy.utils.ErrorCodes;

final class ZEconomyApiImpl implements ZEconomyApi {
    private static final String API_VERSION = "1.0.0";

    @Override
    public ApiInfo getInfo() {
        return new ApiInfo(
            ZEconomy.MOD_ID,
            API_VERSION,
            ZEconomy.PRIMARY_CURRENCY_ID,
            ZEconomy.SECONDARY_CURRENCY_ID,
            ZEconomy.SERVER_ACCOUNT_ID.toString()
        );
    }

    @Override
    public ApiResult<List<CurrencyView>> getCurrencies() {
        List<CurrencyView> result = new ArrayList<>();
        for (BaseCurrency currency : CurrencyData.SERVER.currencies) {
            result.add(toCurrencyView(currency));
        }
        result.sort(Comparator.comparing(CurrencyView::id));
        return ApiResult.success(List.copyOf(result));
    }

    @Override
    public ApiResult<CurrencyView> getCurrency(String currencyId) {
        Optional<BaseCurrency> currency = findCurrency(currencyId);
        if (currency.isEmpty()) {
            return ApiResult.failure(ApiErrorCode.CURRENCY_NOT_FOUND, "Currency not found: " + currencyId);
        }
        return ApiResult.success(toCurrencyView(currency.get()));
    }

    @Override
    public ApiResult<PlayerEconomySnapshot> getPlayerSnapshot(UUID playerId) {
        if (playerId == null) {
            return ApiResult.failure(ApiErrorCode.PLAYER_NOT_FOUND, "Player id is null");
        }
        MinecraftServer server = currentServer();
        String playerName = playerName(server, playerId);
        CurrencyPlayerData.SERVER.newPlayer(playerId);
        Map<String, Double> wallet = walletBalances(playerId);
        Map<String, Double> bank = orderedMap(ZEconomy.EXTRA_DATA.getAllDeposits(playerId));
        Map<String, Double> vault = orderedMap(ZEconomy.EXTRA_DATA.getAllVaultBalances(playerId));
        boolean hasVaultPin = ZEconomy.EXTRA_DATA.hasVaultPin(playerId);
        PlayerEconomySnapshot snapshot = new PlayerEconomySnapshot(
            playerId,
            playerName,
            wallet,
            bank,
            vault,
            ZEconomy.EXTRA_DATA.pendingMailCount(playerId),
            ZEconomy.EXTRA_DATA.getDailyStreak(playerId),
            ZEconomy.EXTRA_DATA.hasClaimedDailyToday(playerId),
            hasVaultPin
        );
        return ApiResult.success(snapshot);
    }

    @Override
    public ApiResult<TreasurySnapshot> getTreasurySnapshot(String currencyId) {
        Optional<BaseCurrency> currency = findCurrency(currencyId);
        if (currency.isEmpty()) {
            return ApiResult.failure(ApiErrorCode.CURRENCY_NOT_FOUND, "Currency not found: " + currencyId);
        }
        return ApiResult.success(treasurySnapshot(currencyId));
    }

    @Override
    public ApiResult<List<TreasurySnapshot>> getTreasurySnapshots() {
        List<TreasurySnapshot> snapshots = new ArrayList<>();
        for (BaseCurrency currency : CurrencyData.SERVER.currencies) {
            snapshots.add(treasurySnapshot(currency.getName()));
        }
        snapshots.sort(Comparator.comparing(TreasurySnapshot::currencyId));
        return ApiResult.success(List.copyOf(snapshots));
    }

    @Override
    public ApiResult<List<RateView>> getRates() {
        List<RateView> rates = new ArrayList<>();
        for (Map.Entry<String, Double> entry : ZEconomy.EXTRA_DATA.getAllRates().entrySet()) {
            String[] parts = entry.getKey().split("->", 2);
            if (parts.length == 2) {
                rates.add(new RateView(parts[0], parts[1], entry.getValue()));
            }
        }
        rates.sort(Comparator.comparing(RateView::fromCurrencyId).thenComparing(RateView::toCurrencyId));
        return ApiResult.success(List.copyOf(rates));
    }

    @Override
    public ApiResult<RateView> getRate(String fromCurrencyId, String toCurrencyId) {
        if (!hasCurrency(fromCurrencyId) || !hasCurrency(toCurrencyId)) {
            return ApiResult.failure(ApiErrorCode.CURRENCY_NOT_FOUND, "Currency not found");
        }
        double rate = ZEconomy.EXTRA_DATA.getRate(fromCurrencyId, toCurrencyId);
        if (rate <= 0.0D) {
            return ApiResult.failure(ApiErrorCode.RATE_NOT_FOUND, "Rate not found");
        }
        return ApiResult.success(new RateView(fromCurrencyId, toCurrencyId, rate));
    }

    @Override
    public ApiResult<List<RichEntryView>> getTopRich(String currencyId, int limit, boolean includeVault) {
        if (!hasCurrency(currencyId)) {
            return ApiResult.failure(ApiErrorCode.CURRENCY_NOT_FOUND, "Currency not found: " + currencyId);
        }
        MinecraftServer server = currentServer();
        if (server == null) {
            return ApiResult.failure(ApiErrorCode.SERVER_UNAVAILABLE, "Server unavailable");
        }
        List<RichEntryView> result = new ArrayList<>();
        for (ExtraEconomyData.RichEntry entry : ZEconomy.EXTRA_DATA.getTopRich(server, currencyId, Math.max(1, limit), includeVault)) {
            result.add(new RichEntryView(entry.playerId(), entry.playerName(), entry.currency(), entry.amount()));
        }
        return ApiResult.success(List.copyOf(result));
    }

    @Override
    public ApiResult<List<TransactionLogView>> getRecentLogs(int limit) {
        List<TransactionLogView> result = new ArrayList<>();
        for (ExtraEconomyData.TransactionRecord record : ZEconomy.EXTRA_DATA.getRecentLogs(Math.max(1, limit))) {
            result.add(new TransactionLogView(
                record.epochSec(),
                record.type(),
                record.actor(),
                record.target(),
                record.currency(),
                record.amount(),
                record.fee(),
                record.note()
            ));
        }
        return ApiResult.success(List.copyOf(result));
    }

    @Override
    public ApiResult<RuntimeStatusView> getRuntimeStatus() {
        MinecraftServer server = currentServer();
        return ApiResult.success(new RuntimeStatusView(
            EconomyConfig.ENABLE_VAULT_BRIDGE.get(),
            VaultBridge.isAvailable(),
            VaultBridge.getProviderName(),
            EconomyConfig.VAULT_SYNC_CURRENCY_ID.get(),
            ZEconomyEvents.pendingVaultSyncCount(),
            EconomyConfig.STORAGE_MODE.get(),
            server == null ? 0 : server.getPlayerList().getPlayerCount(),
            ZEconomy.EXTRA_DATA.getLogCount(),
            ZEconomy.EXTRA_DATA.getLastExportEpochSec()
        ));
    }

    @Override
    public ApiResult<Double> addBalance(UUID playerId, String currencyId, double amount) {
        if (playerId == null) {
            return ApiResult.failure(ApiErrorCode.PLAYER_NOT_FOUND, "Player id is null");
        }
        if (amount == 0.0D) {
            return ApiResult.failure(ApiErrorCode.INVALID_AMOUNT, "Amount must not be zero");
        }
        if (!hasCurrency(currencyId)) {
            return ApiResult.failure(ApiErrorCode.CURRENCY_NOT_FOUND, "Currency not found: " + currencyId);
        }
        CurrencyPlayerData.SERVER.newPlayer(playerId);
        double current = CurrencyHelper.getPlayerCurrencyServerData().getBalance(playerId, currencyId).value;
        if (amount < 0.0D && current + amount < 0.0D) {
            return ApiResult.failure(ApiErrorCode.INSUFFICIENT_FUNDS, "Insufficient funds");
        }
        ErrorCodes result = CurrencyHelper.getPlayerCurrencyServerData().addCurrencyValue(playerId, currencyId, amount);
        if (result != ErrorCodes.SUCCESS) {
            return ApiResult.failure(ApiErrorCode.OPERATION_FAILED, "Failed to update balance");
        }
        syncIfOnline(playerId);
        return ApiResult.success(CurrencyHelper.getPlayerCurrencyServerData().getBalance(playerId, currencyId).value);
    }

    @Override
    public ApiResult<Double> setBalance(UUID playerId, String currencyId, double amount) {
        if (playerId == null) {
            return ApiResult.failure(ApiErrorCode.PLAYER_NOT_FOUND, "Player id is null");
        }
        if (amount < 0.0D) {
            return ApiResult.failure(ApiErrorCode.INVALID_AMOUNT, "Amount must be non-negative");
        }
        if (!hasCurrency(currencyId)) {
            return ApiResult.failure(ApiErrorCode.CURRENCY_NOT_FOUND, "Currency not found: " + currencyId);
        }
        CurrencyPlayerData.SERVER.newPlayer(playerId);
        ErrorCodes result = CurrencyHelper.getPlayerCurrencyServerData().setCurrencyValue(playerId, currencyId, amount);
        if (result != ErrorCodes.SUCCESS) {
            return ApiResult.failure(ApiErrorCode.OPERATION_FAILED, "Failed to set balance");
        }
        syncIfOnline(playerId);
        return ApiResult.success(CurrencyHelper.getPlayerCurrencyServerData().getBalance(playerId, currencyId).value);
    }

    @Override
    public ApiResult<Double> transfer(UUID fromPlayerId, UUID toPlayerId, String currencyId, double amount) {
        if (fromPlayerId == null || toPlayerId == null) {
            return ApiResult.failure(ApiErrorCode.PLAYER_NOT_FOUND, "Player id is null");
        }
        if (amount <= 0.0D) {
            return ApiResult.failure(ApiErrorCode.INVALID_AMOUNT, "Amount must be positive");
        }
        if (!hasCurrency(currencyId)) {
            return ApiResult.failure(ApiErrorCode.CURRENCY_NOT_FOUND, "Currency not found: " + currencyId);
        }
        ServerPlayer from = onlinePlayer(fromPlayerId);
        ServerPlayer to = onlinePlayer(toPlayerId);
        if (from == null || to == null) {
            return ApiResult.failure(ApiErrorCode.PLAYER_OFFLINE, "Transfer requires online players");
        }
        if (!EconomyOperationService.transfer(from, to, currencyId, amount)) {
            return ApiResult.failure(ApiErrorCode.INSUFFICIENT_FUNDS, "Transfer failed");
        }
        return ApiResult.success(CurrencyHelper.getPlayerCurrencyServerData().getBalance(fromPlayerId, currencyId).value);
    }

    @Override
    public ApiResult<Double> depositBank(UUID playerId, String currencyId, double amount) {
        if (playerId == null) {
            return ApiResult.failure(ApiErrorCode.PLAYER_NOT_FOUND, "Player id is null");
        }
        if (amount <= 0.0D) {
            return ApiResult.failure(ApiErrorCode.INVALID_AMOUNT, "Amount must be positive");
        }
        if (!hasCurrency(currencyId)) {
            return ApiResult.failure(ApiErrorCode.CURRENCY_NOT_FOUND, "Currency not found: " + currencyId);
        }
        ServerPlayer player = onlinePlayer(playerId);
        if (player == null) {
            return ApiResult.failure(ApiErrorCode.PLAYER_OFFLINE, "Bank operations require online player");
        }
        if (!EconomyOperationService.depositBank(player, currencyId, amount)) {
            return ApiResult.failure(ApiErrorCode.INSUFFICIENT_FUNDS, "Bank deposit failed");
        }
        return ApiResult.success(ZEconomy.EXTRA_DATA.getDeposited(playerId, currencyId));
    }

    @Override
    public ApiResult<Double> withdrawBank(UUID playerId, String currencyId, double amount) {
        if (playerId == null) {
            return ApiResult.failure(ApiErrorCode.PLAYER_NOT_FOUND, "Player id is null");
        }
        if (amount <= 0.0D) {
            return ApiResult.failure(ApiErrorCode.INVALID_AMOUNT, "Amount must be positive");
        }
        if (!hasCurrency(currencyId)) {
            return ApiResult.failure(ApiErrorCode.CURRENCY_NOT_FOUND, "Currency not found: " + currencyId);
        }
        ServerPlayer player = onlinePlayer(playerId);
        if (player == null) {
            return ApiResult.failure(ApiErrorCode.PLAYER_OFFLINE, "Bank operations require online player");
        }
        if (!EconomyOperationService.withdrawBank(player, currencyId, amount)) {
            return ApiResult.failure(ApiErrorCode.INSUFFICIENT_FUNDS, "Bank withdraw failed");
        }
        return ApiResult.success(ZEconomy.EXTRA_DATA.getDeposited(playerId, currencyId));
    }

    @Override
    public ApiResult<Boolean> setVaultPin(UUID playerId, String pin) {
        if (playerId == null) {
            return ApiResult.failure(ApiErrorCode.PLAYER_NOT_FOUND, "Player id is null");
        }
        if (pin == null || pin.isBlank()) {
            return ApiResult.failure(ApiErrorCode.INVALID_PIN, "Pin is empty");
        }
        if (!ZEconomy.EXTRA_DATA.setVaultPin(playerId, pin)) {
            return ApiResult.failure(ApiErrorCode.INVALID_PIN, "Invalid pin");
        }
        syncIfOnline(playerId);
        return ApiResult.success(Boolean.TRUE);
    }

    @Override
    public ApiResult<Double> depositVault(UUID playerId, String pin, String currencyId, double amount) {
        if (playerId == null) {
            return ApiResult.failure(ApiErrorCode.PLAYER_NOT_FOUND, "Player id is null");
        }
        if (amount <= 0.0D) {
            return ApiResult.failure(ApiErrorCode.INVALID_AMOUNT, "Amount must be positive");
        }
        if (!hasCurrency(currencyId)) {
            return ApiResult.failure(ApiErrorCode.CURRENCY_NOT_FOUND, "Currency not found: " + currencyId);
        }
        ServerPlayer player = onlinePlayer(playerId);
        if (player == null) {
            return ApiResult.failure(ApiErrorCode.PLAYER_OFFLINE, "Vault operations require online player");
        }
        if (!EconomyOperationService.depositVault(player, pin, currencyId, amount)) {
            return ApiResult.failure(ApiErrorCode.OPERATION_FAILED, "Vault deposit failed");
        }
        return ApiResult.success(ZEconomy.EXTRA_DATA.getVaultBalance(playerId, currencyId));
    }

    @Override
    public ApiResult<Double> withdrawVault(UUID playerId, String pin, String currencyId, double amount) {
        if (playerId == null) {
            return ApiResult.failure(ApiErrorCode.PLAYER_NOT_FOUND, "Player id is null");
        }
        if (amount <= 0.0D) {
            return ApiResult.failure(ApiErrorCode.INVALID_AMOUNT, "Amount must be positive");
        }
        if (!hasCurrency(currencyId)) {
            return ApiResult.failure(ApiErrorCode.CURRENCY_NOT_FOUND, "Currency not found: " + currencyId);
        }
        ServerPlayer player = onlinePlayer(playerId);
        if (player == null) {
            return ApiResult.failure(ApiErrorCode.PLAYER_OFFLINE, "Vault operations require online player");
        }
        if (!EconomyOperationService.withdrawVault(player, pin, currencyId, amount)) {
            return ApiResult.failure(ApiErrorCode.OPERATION_FAILED, "Vault withdraw failed");
        }
        return ApiResult.success(ZEconomy.EXTRA_DATA.getVaultBalance(playerId, currencyId));
    }

    @Override
    public ApiResult<DailyRewardView> claimDaily(UUID playerId) {
        if (playerId == null) {
            return ApiResult.failure(ApiErrorCode.PLAYER_NOT_FOUND, "Player id is null");
        }
        ServerPlayer player = onlinePlayer(playerId);
        if (player == null) {
            return ApiResult.failure(ApiErrorCode.PLAYER_OFFLINE, "Daily reward requires online player");
        }
        ExtraEconomyData.DailyClaimResult result = EconomyOperationService.claimDaily(player);
        if (!result.success()) {
            return ApiResult.failure(ApiErrorCode.TREASURY_INSUFFICIENT_FUNDS, "Daily reward not available");
        }
        return ApiResult.success(new DailyRewardView(result.success(), result.zReward(), result.bReward(), result.streak()));
    }

    @Override
    public ApiResult<Double> exchange(UUID playerId, String fromCurrencyId, String toCurrencyId, double amount) {
        if (playerId == null) {
            return ApiResult.failure(ApiErrorCode.PLAYER_NOT_FOUND, "Player id is null");
        }
        if (amount <= 0.0D) {
            return ApiResult.failure(ApiErrorCode.INVALID_AMOUNT, "Amount must be positive");
        }
        if (!hasCurrency(fromCurrencyId) || !hasCurrency(toCurrencyId)) {
            return ApiResult.failure(ApiErrorCode.CURRENCY_NOT_FOUND, "Currency not found");
        }
        if (!EconomyConfig.ENABLE_BCOIN_EXCHANGE.get()) {
            return ApiResult.failure(ApiErrorCode.FEATURE_DISABLED, "Exchange is disabled");
        }
        ServerPlayer player = onlinePlayer(playerId);
        if (player == null) {
            return ApiResult.failure(ApiErrorCode.PLAYER_OFFLINE, "Exchange requires online player");
        }
        if (!EconomyOperationService.exchange(player, fromCurrencyId, toCurrencyId, amount)) {
            return ApiResult.failure(ApiErrorCode.OPERATION_FAILED, "Exchange failed");
        }
        return ApiResult.success(CurrencyHelper.getPlayerCurrencyServerData().getBalance(playerId, toCurrencyId).value);
    }

    @Override
    public ApiResult<Double> setTreasuryBalance(String currencyId, double amount) {
        if (amount < 0.0D) {
            return ApiResult.failure(ApiErrorCode.INVALID_AMOUNT, "Amount must be non-negative");
        }
        return setBalance(CurrencyHelper.getServerAccountUUID(), currencyId, amount);
    }

    @Override
    public ApiResult<Double> addTreasuryBalance(String currencyId, double amount) {
        if (amount <= 0.0D) {
            return ApiResult.failure(ApiErrorCode.INVALID_AMOUNT, "Amount must be positive");
        }
        return addBalance(CurrencyHelper.getServerAccountUUID(), currencyId, amount);
    }

    @Override
    public ApiResult<Double> takeTreasuryBalance(String currencyId, double amount) {
        if (amount <= 0.0D) {
            return ApiResult.failure(ApiErrorCode.INVALID_AMOUNT, "Amount must be positive");
        }
        if (!hasCurrency(currencyId)) {
            return ApiResult.failure(ApiErrorCode.CURRENCY_NOT_FOUND, "Currency not found: " + currencyId);
        }
        double spendable = ZEconomy.EXTRA_DATA.getServerSpendable(currencyId);
        if (spendable < amount) {
            return ApiResult.failure(ApiErrorCode.TREASURY_INSUFFICIENT_FUNDS, "Treasury spendable balance is too low");
        }
        return addBalance(CurrencyHelper.getServerAccountUUID(), currencyId, -amount);
    }

    @Override
    public ApiResult<Boolean> setRate(String fromCurrencyId, String toCurrencyId, double rate) {
        if (rate <= 0.0D) {
            return ApiResult.failure(ApiErrorCode.INVALID_AMOUNT, "Rate must be positive");
        }
        if (!hasCurrency(fromCurrencyId) || !hasCurrency(toCurrencyId)) {
            return ApiResult.failure(ApiErrorCode.CURRENCY_NOT_FOUND, "Currency not found");
        }
        ZEconomy.EXTRA_DATA.setRate(fromCurrencyId, toCurrencyId, rate);
        return ApiResult.success(Boolean.TRUE);
    }

    @Override
    public ApiResult<Boolean> removeRate(String fromCurrencyId, String toCurrencyId) {
        if (!ZEconomy.EXTRA_DATA.removeRate(fromCurrencyId, toCurrencyId)) {
            return ApiResult.failure(ApiErrorCode.RATE_NOT_FOUND, "Rate not found");
        }
        return ApiResult.success(Boolean.TRUE);
    }

    @Override
    public ApiResult<Boolean> resetRates() {
        ZEconomy.EXTRA_DATA.clearRates();
        ZEconomy.EXTRA_DATA.ensureDefaultRates();
        return ApiResult.success(Boolean.TRUE);
    }

    private MinecraftServer currentServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    private ServerPlayer onlinePlayer(UUID playerId) {
        MinecraftServer server = currentServer();
        if (server == null || playerId == null) {
            return null;
        }
        return server.getPlayerList().getPlayer(playerId);
    }

    private Optional<BaseCurrency> findCurrency(String currencyId) {
        if (currencyId == null || currencyId.isBlank()) {
            return Optional.empty();
        }
        return CurrencyData.SERVER.currencies.stream().filter(currency -> currency.getName().equals(currencyId)).findFirst();
    }

    private boolean hasCurrency(String currencyId) {
        return findCurrency(currencyId).isPresent();
    }

    private CurrencyView toCurrencyView(BaseCurrency currency) {
        return new CurrencyView(
            currency.getName(),
            currency.symbol == null ? "" : currency.symbol.value,
            currency.getDefaultValue(),
            currency.canDelete
        );
    }

    private Map<String, Double> walletBalances(UUID playerId) {
        Map<String, Double> balances = new LinkedHashMap<>();
        for (CurrencyPlayerData.PlayerCurrency currency : CurrencyHelper.getPlayerCurrencyServerData().getPlayersCurrency(playerId)) {
            balances.put(currency.currency.getName(), currency.balance);
        }
        return Map.copyOf(balances);
    }

    private Map<String, Double> orderedMap(Map<String, Double> source) {
        Map<String, Double> result = new LinkedHashMap<>();
        source.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
        return Map.copyOf(result);
    }

    private TreasurySnapshot treasurySnapshot(String currencyId) {
        double total = CurrencyHelper.getPlayerCurrencyServerData().getBalance(CurrencyHelper.getServerAccountUUID(), currencyId).value;
        double reserved = ZEconomy.EXTRA_DATA.getTotalDeposited(currencyId);
        double spendable = ZEconomy.EXTRA_DATA.getServerSpendable(currencyId);
        return new TreasurySnapshot(currencyId, total, reserved, spendable);
    }

    private void syncIfOnline(UUID playerId) {
        EconomyOperationService.syncIfOnline(playerId);
    }

    private String playerName(MinecraftServer server, UUID playerId) {
        if (server == null) {
            return playerId.toString();
        }
        ServerPlayer online = server.getPlayerList().getPlayer(playerId);
        if (online != null) {
            return online.getName().getString();
        }
        return playerId.toString();
    }
}
