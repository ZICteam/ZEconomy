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
import io.zicteam.zeconomy.system.AdminOperationService;
import io.zicteam.zeconomy.system.EconomyOperationService;
import io.zicteam.zeconomy.system.EconomyReadService;
import io.zicteam.zeconomy.system.EconomySnapshotReadService;
import io.zicteam.zeconomy.system.ExtraEconomyData;
import io.zicteam.zeconomy.utils.CurrencyHelper;

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
        EconomySnapshotReadService.PlayerSnapshot snapshotRead = EconomySnapshotReadService.player(playerId);
        Map<String, Double> wallet = walletBalances(playerId);
        Map<String, Double> bank = orderedMap(snapshotRead.bankBalances());
        Map<String, Double> vault = orderedMap(snapshotRead.vaultBalances());
        PlayerEconomySnapshot snapshot = new PlayerEconomySnapshot(
            playerId,
            playerName,
            wallet,
            bank,
            vault,
            snapshotRead.pendingMail(),
            snapshotRead.dailyStreak(),
            ZEconomy.EXTRA_DATA.hasClaimedDailyToday(playerId),
            snapshotRead.hasVaultPin()
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
        for (Map.Entry<String, Double> entry : EconomySnapshotReadService.exchangeRates().entrySet()) {
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
        for (ExtraEconomyData.RichEntry entry : EconomyReadService.getTopRich(server, currencyId, Math.max(1, limit), includeVault)) {
            result.add(new RichEntryView(entry.playerId(), entry.playerName(), entry.currency(), entry.amount()));
        }
        return ApiResult.success(List.copyOf(result));
    }

    @Override
    public ApiResult<List<TransactionLogView>> getRecentLogs(int limit) {
        List<TransactionLogView> result = new ArrayList<>();
        for (ExtraEconomyData.TransactionRecord record : EconomySnapshotReadService.recentLogs(Math.max(1, limit))) {
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
        EconomySnapshotReadService.RuntimeSnapshot runtime = EconomySnapshotReadService.runtime();
        return ApiResult.success(new RuntimeStatusView(
            EconomyConfig.ENABLE_VAULT_BRIDGE.get(),
            VaultBridge.isAvailable(),
            VaultBridge.getProviderName(),
            EconomyConfig.VAULT_SYNC_CURRENCY_ID.get(),
            ZEconomyEvents.pendingVaultSyncCount(),
            EconomyConfig.STORAGE_MODE.get(),
            server == null ? 0 : server.getPlayerList().getPlayerCount(),
            runtime.logCount(),
            runtime.lastExportEpochSec()
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
        EconomyOperationService.BalanceMutationResult result = EconomyOperationService.addBalance(playerId, currencyId, amount);
        if (!result.success()) {
            return operationFailure(result, "Failed to update balance");
        }
        return ApiResult.success(result.balanceAfter());
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
        EconomyOperationService.BalanceMutationResult result = EconomyOperationService.setBalance(playerId, currencyId, amount);
        if (!result.success()) {
            return operationFailure(result, "Failed to set balance");
        }
        return ApiResult.success(result.balanceAfter());
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
        EconomyOperationService.TransferResult result = EconomyOperationService.transfer(from, to, currencyId, amount);
        if (!result.success()) {
            return operationFailure(result.operation(), "Transfer failed");
        }
        return ApiResult.success(result.senderBalanceAfter());
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
        EconomyOperationService.BankResult result = EconomyOperationService.depositBank(player, currencyId, amount);
        if (!result.success()) {
            return operationFailure(result.operation(), "Bank deposit failed");
        }
        return ApiResult.success(result.depositedAfter());
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
        EconomyOperationService.BankResult result = EconomyOperationService.withdrawBank(player, currencyId, amount);
        if (!result.success()) {
            return operationFailure(result.operation(), "Bank withdraw failed");
        }
        return ApiResult.success(result.depositedAfter());
    }

    @Override
    public ApiResult<Boolean> setVaultPin(UUID playerId, String pin) {
        if (playerId == null) {
            return ApiResult.failure(ApiErrorCode.PLAYER_NOT_FOUND, "Player id is null");
        }
        if (pin == null || pin.isBlank()) {
            return ApiResult.failure(ApiErrorCode.INVALID_PIN, "Pin is empty");
        }
        EconomyOperationService.OperationResult result = EconomyOperationService.setVaultPin(playerId, pin);
        if (!result.success()) {
            return operationFailure(result, "Invalid pin");
        }
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
        EconomyOperationService.VaultResult result = EconomyOperationService.depositVault(player, pin, currencyId, amount);
        if (!result.success()) {
            return operationFailure(result.operation(), "Vault deposit failed");
        }
        return ApiResult.success(result.vaultBalanceAfter());
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
        EconomyOperationService.VaultResult result = EconomyOperationService.withdrawVault(player, pin, currencyId, amount);
        if (!result.success()) {
            return operationFailure(result.operation(), "Vault withdraw failed");
        }
        return ApiResult.success(result.vaultBalanceAfter());
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
        EconomyOperationService.DailyOperationResult result = EconomyOperationService.claimDaily(player);
        if (!result.success()) {
            return ApiResult.failure(ApiErrorCode.TREASURY_INSUFFICIENT_FUNDS, "Daily reward not available");
        }
        return ApiResult.success(new DailyRewardView(result.reward().success(), result.reward().zReward(), result.reward().bReward(), result.reward().streak()));
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
        EconomyOperationService.ExchangeResult result = EconomyOperationService.exchange(player, fromCurrencyId, toCurrencyId, amount);
        if (!result.success()) {
            return operationFailure(result.operation(), "Exchange failed");
        }
        return ApiResult.success(result.targetBalanceAfter());
    }

    @Override
    public ApiResult<Double> setTreasuryBalance(String currencyId, double amount) {
        if (!hasCurrency(currencyId)) {
            return ApiResult.failure(ApiErrorCode.CURRENCY_NOT_FOUND, "Currency not found: " + currencyId);
        }
        if (amount < 0.0D) {
            return ApiResult.failure(ApiErrorCode.INVALID_AMOUNT, "Amount must be non-negative");
        }
        AdminOperationService.TreasuryMutationResult result = AdminOperationService.setServerBalance(currencyId, amount);
        if (!result.success()) {
            return adminOperationFailure(result.failure(), "Failed to set treasury balance");
        }
        return ApiResult.success(result.balanceAfter());
    }

    @Override
    public ApiResult<Double> addTreasuryBalance(String currencyId, double amount) {
        if (!hasCurrency(currencyId)) {
            return ApiResult.failure(ApiErrorCode.CURRENCY_NOT_FOUND, "Currency not found: " + currencyId);
        }
        if (amount <= 0.0D) {
            return ApiResult.failure(ApiErrorCode.INVALID_AMOUNT, "Amount must be positive");
        }
        AdminOperationService.TreasuryMutationResult result = AdminOperationService.giveServerBalance(currencyId, amount);
        if (!result.success()) {
            return adminOperationFailure(result.failure(), "Failed to add treasury balance");
        }
        return ApiResult.success(result.balanceAfter());
    }

    @Override
    public ApiResult<Double> takeTreasuryBalance(String currencyId, double amount) {
        if (amount <= 0.0D) {
            return ApiResult.failure(ApiErrorCode.INVALID_AMOUNT, "Amount must be positive");
        }
        if (!hasCurrency(currencyId)) {
            return ApiResult.failure(ApiErrorCode.CURRENCY_NOT_FOUND, "Currency not found: " + currencyId);
        }
        AdminOperationService.TreasuryMutationResult result = AdminOperationService.takeServerBalance(currencyId, amount);
        if (!result.success()) {
            if (result.failure() == AdminOperationService.OperationFailure.INSUFFICIENT_FUNDS) {
                return ApiResult.failure(ApiErrorCode.TREASURY_INSUFFICIENT_FUNDS, "Treasury spendable balance is too low");
            }
            return adminOperationFailure(result.failure(), "Failed to take treasury balance");
        }
        return ApiResult.success(result.balanceAfter());
    }

    @Override
    public ApiResult<Boolean> setRate(String fromCurrencyId, String toCurrencyId, double rate) {
        if (rate <= 0.0D) {
            return ApiResult.failure(ApiErrorCode.INVALID_AMOUNT, "Rate must be positive");
        }
        if (!hasCurrency(fromCurrencyId) || !hasCurrency(toCurrencyId)) {
            return ApiResult.failure(ApiErrorCode.CURRENCY_NOT_FOUND, "Currency not found");
        }
        EconomyOperationService.RateMutationResult result = EconomyOperationService.setRate(fromCurrencyId, toCurrencyId, rate);
        if (!result.success()) {
            return operationFailure(result.operation(), "Failed to set rate");
        }
        return ApiResult.success(Boolean.TRUE);
    }

    @Override
    public ApiResult<Boolean> removeRate(String fromCurrencyId, String toCurrencyId) {
        EconomyOperationService.RateMutationResult result = EconomyOperationService.clearRate(fromCurrencyId, toCurrencyId);
        if (!result.success()) {
            return ApiResult.failure(ApiErrorCode.RATE_NOT_FOUND, "Rate not found");
        }
        return ApiResult.success(Boolean.TRUE);
    }

    @Override
    public ApiResult<Boolean> resetRates() {
        EconomyOperationService.resetRates();
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

    private <T> ApiResult<T> operationFailure(EconomyOperationService.OperationResult result, String fallbackMessage) {
        if (result == null || result.failure() == null) {
            return ApiResult.failure(ApiErrorCode.OPERATION_FAILED, fallbackMessage);
        }
        return switch (result.failure()) {
            case INVALID_PLAYER -> ApiResult.failure(ApiErrorCode.PLAYER_NOT_FOUND, "Player is unavailable");
            case INVALID_AMOUNT -> ApiResult.failure(ApiErrorCode.INVALID_AMOUNT, "Invalid amount");
            case INVALID_PIN -> ApiResult.failure(ApiErrorCode.INVALID_PIN, "Invalid pin");
            case INSUFFICIENT_FUNDS -> ApiResult.failure(ApiErrorCode.INSUFFICIENT_FUNDS, "Insufficient funds");
            case FEATURE_DISABLED -> ApiResult.failure(ApiErrorCode.FEATURE_DISABLED, "Feature is disabled");
            case SELF_TARGET, EMPTY_HAND, EMPTY_MAILBOX, RATE_NOT_FOUND, ALREADY_CLAIMED -> ApiResult.failure(ApiErrorCode.OPERATION_FAILED, fallbackMessage);
            case OPERATION_FAILED -> ApiResult.failure(ApiErrorCode.OPERATION_FAILED, fallbackMessage);
        };
    }

    private <T> ApiResult<T> operationFailure(EconomyOperationService.BalanceMutationResult result, String fallbackMessage) {
        if (result == null || result.failure() == null) {
            return ApiResult.failure(ApiErrorCode.OPERATION_FAILED, fallbackMessage);
        }
        return switch (result.failure()) {
            case INVALID_PLAYER -> ApiResult.failure(ApiErrorCode.PLAYER_NOT_FOUND, "Player is unavailable");
            case INVALID_AMOUNT -> ApiResult.failure(ApiErrorCode.INVALID_AMOUNT, "Invalid amount");
            case INVALID_PIN -> ApiResult.failure(ApiErrorCode.INVALID_PIN, "Invalid pin");
            case INSUFFICIENT_FUNDS -> ApiResult.failure(ApiErrorCode.INSUFFICIENT_FUNDS, "Insufficient funds");
            case FEATURE_DISABLED -> ApiResult.failure(ApiErrorCode.FEATURE_DISABLED, "Feature is disabled");
            case SELF_TARGET, EMPTY_HAND, EMPTY_MAILBOX, RATE_NOT_FOUND, ALREADY_CLAIMED -> ApiResult.failure(ApiErrorCode.OPERATION_FAILED, fallbackMessage);
            case OPERATION_FAILED -> ApiResult.failure(ApiErrorCode.OPERATION_FAILED, fallbackMessage);
        };
    }

    private <T> ApiResult<T> adminOperationFailure(AdminOperationService.OperationFailure failure, String fallbackMessage) {
        if (failure == null) {
            return ApiResult.failure(ApiErrorCode.OPERATION_FAILED, fallbackMessage);
        }
        return switch (failure) {
            case INVALID_PLAYER -> ApiResult.failure(ApiErrorCode.PLAYER_NOT_FOUND, "Player is unavailable");
            case INVALID_AMOUNT -> ApiResult.failure(ApiErrorCode.INVALID_AMOUNT, "Invalid amount");
            case INSUFFICIENT_FUNDS -> ApiResult.failure(ApiErrorCode.TREASURY_INSUFFICIENT_FUNDS, "Treasury spendable balance is too low");
            case FEATURE_DISABLED -> ApiResult.failure(ApiErrorCode.FEATURE_DISABLED, "Feature is disabled");
            case SERVICE_UNAVAILABLE -> ApiResult.failure(ApiErrorCode.SERVER_UNAVAILABLE, "Required service is unavailable");
            case OPERATION_FAILED -> ApiResult.failure(ApiErrorCode.OPERATION_FAILED, fallbackMessage);
        };
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
