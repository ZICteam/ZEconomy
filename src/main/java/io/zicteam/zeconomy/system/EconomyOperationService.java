package io.zicteam.zeconomy.system;

import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import io.zicteam.zeconomy.ZEconomy;
import io.zicteam.zeconomy.api.event.BankTransactionEvent;
import io.zicteam.zeconomy.api.event.DailyRewardEvent;
import io.zicteam.zeconomy.api.event.ExchangeEvent;
import io.zicteam.zeconomy.api.event.RateChangeEvent;
import io.zicteam.zeconomy.api.event.VaultTransactionEvent;
import io.zicteam.zeconomy.api.event.ZEconomyApiEvents;
import io.zicteam.zeconomy.utils.CurrencyHelper;

public final class EconomyOperationService {
    private EconomyOperationService() {
    }

    public static TransferResult transfer(ServerPlayer from, ServerPlayer to, String currencyId, double amount) {
        OperationFailure failure = EconomyPolicyService.canTransfer(from, to, currencyId, amount);
        if (failure != null) {
            return TransferResult.failed(failure);
        }
        TransferPlan plan = buildTransferPlan(from.getUUID(), to.getUUID(), currencyId, amount, io.zicteam.zeconomy.config.EconomyConfig.TRANSFER_FEE_RATE.get());
        if (!applyBalanceDeltas(plan.deltas())) {
            return TransferResult.failed(OperationFailure.INSUFFICIENT_FUNDS);
        }
        EconomyOperationEffects effects = new EconomyOperationEffects()
            .touchPlayers(from, to)
            .requestSave()
            .afterCommit(() -> EconomyLogService.record("TRANSFER", from.getUUID(), to.getUUID(), currencyId, amount, plan.quote().fee(), "pay"));
        effects.dispatch();
        return TransferResult.fromPlan(plan, CurrencyHelper.getPlayerCurrencyServerData().getBalance(from, currencyId).value, effects);
    }

    public static BankResult depositBank(ServerPlayer player, String currencyId, double amount) {
        OperationFailure failure = EconomyPolicyService.canDepositBank(player, currencyId, amount);
        if (failure != null) {
            return BankResult.failed(failure);
        }
        BankDepositPlan plan = buildBankDepositPlan(player.getUUID(), currencyId, amount);
        if (!applyBalanceDeltas(plan.deltas())) {
            return BankResult.failed(OperationFailure.INSUFFICIENT_FUNDS);
        }
        double depositedAfter = EconomyStateMutationService.addBankDeposit(player.getUUID(), currencyId, plan.amount());
        EconomyOperationEffects effects = new EconomyOperationEffects().touchPlayer(player).requestSave();
        effects
            .afterCommit(() -> EconomyLogService.record("BANK_DEPOSIT", player.getUUID(), null, currencyId, plan.amount(), 0.0D, ""))
            .afterCommit(() -> ZEconomyApiEvents.post(new BankTransactionEvent(BankTransactionEvent.Action.DEPOSIT, player.getUUID(), currencyId, plan.amount(), depositedAfter)));
        effects.dispatch();
        return BankResult.fromDepositPlan(plan, depositedAfter, effects);
    }

    public static BankResult withdrawBank(ServerPlayer player, String currencyId, double amount) {
        OperationFailure failure = EconomyPolicyService.canWithdrawBank(player, currencyId, amount);
        if (failure != null) {
            return BankResult.failed(failure, amount);
        }
        double depositedBefore = EconomySnapshotReadService.bankDeposited(player.getUUID(), currencyId);
        BankWithdrawPlan plan = buildBankWithdrawPlan(player.getUUID(), currencyId, depositedBefore, amount);
        EconomyStateMutationService.setBankDeposit(player.getUUID(), currencyId, plan.depositedAfter());
        if (!applyBalanceDeltas(plan.deltas())) {
            EconomyStateMutationService.setBankDeposit(player.getUUID(), currencyId, depositedBefore);
            return BankResult.failed(OperationFailure.INSUFFICIENT_FUNDS, amount);
        }
        EconomyOperationEffects effects = new EconomyOperationEffects().touchPlayer(player).requestSave();
        effects
            .afterCommit(() -> EconomyLogService.record("BANK_WITHDRAW", player.getUUID(), null, currencyId, amount, 0.0D, ""))
            .afterCommit(() -> ZEconomyApiEvents.post(new BankTransactionEvent(BankTransactionEvent.Action.WITHDRAW, player.getUUID(), currencyId, amount, plan.depositedAfter())));
        effects.dispatch();
        return BankResult.fromWithdrawPlan(plan, effects);
    }

    public static ExchangeResult exchange(ServerPlayer player, String fromCurrencyId, String toCurrencyId, double amount) {
        OperationFailure failure = EconomyPolicyService.canExchange(player, fromCurrencyId, toCurrencyId, amount);
        if (failure != null) {
            return ExchangeResult.failed(failure);
        }
        double rate = EconomySnapshotReadService.exchangeRate(fromCurrencyId, toCurrencyId);
        ExchangePlan plan = buildExchangePlan(player.getUUID(), fromCurrencyId, toCurrencyId, amount, rate, io.zicteam.zeconomy.config.EconomyConfig.EXCHANGE_FEE_RATE.get());
        if (!applyBalanceDeltas(plan.deltas())) {
            return ExchangeResult.failed(OperationFailure.OPERATION_FAILED);
        }
        EconomyOperationEffects effects = new EconomyOperationEffects()
            .touchPlayer(player)
            .requestSave();
        effects
            .afterCommit(() -> EconomyLogService.record("EXCHANGE", player.getUUID(), null, toCurrencyId, plan.quote().net(), plan.quote().fee(), fromCurrencyId + "->" + toCurrencyId + " amount=" + amount))
            .afterCommit(() -> ZEconomyApiEvents.post(new ExchangeEvent(player.getUUID(), fromCurrencyId, toCurrencyId, amount, plan.quote().net(), plan.quote().fee())));
        effects.dispatch();
        return ExchangeResult.fromPlan(plan, CurrencyHelper.getPlayerCurrencyServerData().getBalance(player, toCurrencyId).value, effects);
    }

    public static VaultResult depositVault(ServerPlayer player, String pin, String currencyId, double amount) {
        OperationFailure failure = EconomyPolicyService.canAccessVault(player, pin, currencyId, amount, false);
        if (failure != null) {
            return VaultResult.failed(failure, amount);
        }
        VaultDepositPlan plan = buildVaultDepositPlan(player.getUUID(), currencyId, amount);
        if (!applyBalanceDeltas(plan.deltas())) {
            return VaultResult.failed(OperationFailure.OPERATION_FAILED, amount);
        }
        double vaultBalanceAfter = EconomyStateMutationService.addVaultBalance(player.getUUID(), currencyId, plan.amount());
        EconomyOperationEffects effects = new EconomyOperationEffects().touchPlayer(player).requestSave();
        effects
            .afterCommit(() -> EconomyLogService.record("VAULT_DEPOSIT", player.getUUID(), null, currencyId, plan.amount(), 0.0D, ""))
            .afterCommit(() -> ZEconomyApiEvents.post(new VaultTransactionEvent(VaultTransactionEvent.Action.DEPOSIT, player.getUUID(), currencyId, plan.amount(), vaultBalanceAfter)));
        effects.dispatch();
        return VaultResult.fromDepositPlan(plan, vaultBalanceAfter, effects);
    }

    public static VaultResult withdrawVault(ServerPlayer player, String pin, String currencyId, double amount) {
        OperationFailure failure = EconomyPolicyService.canAccessVault(player, pin, currencyId, amount, true);
        if (failure != null) {
            return VaultResult.failed(failure, amount);
        }
        double vaultBefore = EconomySnapshotReadService.vaultBalance(player.getUUID(), currencyId);
        VaultWithdrawPlan plan = buildVaultWithdrawPlan(player.getUUID(), currencyId, vaultBefore, amount);
        EconomyStateMutationService.setVaultBalance(player.getUUID(), currencyId, plan.vaultBalanceAfter());
        if (!applyBalanceDeltas(plan.deltas())) {
            EconomyStateMutationService.setVaultBalance(player.getUUID(), currencyId, vaultBefore);
            return VaultResult.failed(OperationFailure.OPERATION_FAILED, amount);
        }
        EconomyOperationEffects effects = new EconomyOperationEffects().touchPlayer(player).requestSave();
        effects
            .afterCommit(() -> EconomyLogService.record("VAULT_WITHDRAW", player.getUUID(), null, currencyId, amount, 0.0D, ""))
            .afterCommit(() -> ZEconomyApiEvents.post(new VaultTransactionEvent(VaultTransactionEvent.Action.WITHDRAW, player.getUUID(), currencyId, amount, plan.vaultBalanceAfter())));
        effects.dispatch();
        return VaultResult.fromWithdrawPlan(plan, amount, effects);
    }

    public static DailyOperationResult claimDaily(ServerPlayer player) {
        OperationFailure failure = EconomyPolicyService.canClaimDaily(player);
        if (failure != null) {
            int streak = player == null ? 0 : EconomySnapshotReadService.dailyStreak(player.getUUID());
            return DailyOperationResult.failed(failure, new ExtraEconomyData.DailyClaimResult(false, 0.0D, 0.0D, streak));
        }
        EconomyPolicyService.DailyRewardPolicy rewardPolicy = EconomyPolicyService.dailyRewardPolicy(player.getUUID());
        DailyClaimPlan plan = buildDailyClaimPlan(player.getUUID(), rewardPolicy);
        if (!applyBalanceDeltas(plan.deltas())) {
            return DailyOperationResult.failed(OperationFailure.OPERATION_FAILED, new ExtraEconomyData.DailyClaimResult(false, 0.0D, 0.0D, EconomySnapshotReadService.dailyStreak(player.getUUID())));
        }
        ExtraEconomyData.DailyClaimResult result = EconomyStateMutationService.applyDailyClaim(player.getUUID(), plan.rewardPolicy());
        EconomyOperationEffects effects = new EconomyOperationEffects()
            .touchPlayer(player)
            .requestSave()
            .afterCommit(() -> EconomyLogService.record("DAILY_REWARD", player.getUUID(), null, "mixed", result.zReward(), 0.0D, ZEconomy.SECONDARY_CURRENCY_ID + "=" + result.bReward() + " streak=" + result.streak()))
            .afterCommit(() -> ZEconomyApiEvents.post(new DailyRewardEvent(player.getUUID(), result.zReward(), result.bReward(), result.streak())));
        effects.dispatch();
        return DailyOperationResult.succeeded(result, effects);
    }

    public static OperationResult setVaultPin(UUID playerId, String pin) {
        OperationFailure failure = EconomyValidationService.requirePlayerId(playerId);
        if (failure == null) {
            failure = EconomyValidationService.requireValidPin(pin);
        }
        if (failure != null) {
            return OperationResult.failed(failure);
        }
        if (!EconomyStateMutationService.setVaultPin(playerId, pin)) {
            return OperationResult.failed(OperationFailure.INVALID_PIN);
        }
        EconomyOperationEffects effects = new EconomyOperationEffects()
            .touchPlayer(playerId)
            .requestSave()
            .afterCommit(() -> ZEconomyApiEvents.post(new VaultTransactionEvent(VaultTransactionEvent.Action.PIN_SET, playerId, "", 0.0D, 0.0D)));
        effects.dispatch();
        return OperationResult.succeeded(effects);
    }

    public static MailSendResult sendMail(ServerPlayer player, ServerPlayer target) {
        return sendMail(player, target, player == null ? ItemStack.EMPTY : player.getMainHandItem(), true);
    }

    public static MailSendResult sendMail(ServerPlayer player, ServerPlayer target, ItemStack stack, boolean consumeSourceStack) {
        OperationFailure failure = EconomyValidationService.requirePlayer(player);
        if (failure == null) {
            failure = EconomyValidationService.requirePlayer(target);
        }
        if (failure == null) {
            failure = EconomyValidationService.evaluateMailSendValidation(
                io.zicteam.zeconomy.config.EconomyConfig.ENABLE_MAILBOX.get(),
                player.getUUID().equals(target.getUUID()),
                stack != null && !stack.isEmpty()
            );
        }
        if (failure != null) {
            return MailSendResult.failed(failure);
        }
        ItemStack mailStack = stack.copy();
        int pendingAfter = EconomyStateMutationService.sendMail(target.getUUID(), mailStack);
        if (consumeSourceStack) {
            stack.setCount(0);
        }
        EconomyOperationEffects effects = new EconomyOperationEffects()
            .touchPlayers(player, target)
            .requestSave()
            .afterCommit(() -> EconomyLogService.record("MAIL_SEND", player.getUUID(), target.getUUID(), "item", mailStack.getCount(), 0.0D, mailStack.getItem().toString()));
        effects.dispatch();
        return MailSendResult.succeeded(mailStack.copy(), pendingAfter, effects);
    }

    public static MailClaimResult claimMail(ServerPlayer player) {
        OperationFailure failure = EconomyValidationService.requirePlayer(player);
        if (failure != null) {
            return MailClaimResult.failed(failure);
        }
        List<ItemStack> items = EconomyStateMutationService.claimMail(player.getUUID());
        if (items.isEmpty()) {
            return MailClaimResult.failed(OperationFailure.EMPTY_MAILBOX);
        }
        EconomyOperationEffects effects = new EconomyOperationEffects()
            .touchPlayer(player)
            .requestSave()
            .afterCommit(() -> EconomyLogService.record("MAIL_CLAIM", player.getUUID(), null, "item", items.size(), 0.0D, ""));
        effects.dispatch();
        return MailClaimResult.succeeded(List.copyOf(items), effects);
    }

    public static RateMutationResult setRate(String from, String to, double rate) {
        OperationFailure failure = EconomyValidationService.requirePositiveRate(rate);
        if (failure != null) {
            return RateMutationResult.failed(failure);
        }
        EconomyRateMutationService.setRate(from, to, rate);
        EconomyOperationEffects effects = new EconomyOperationEffects()
            .requestSave()
            .afterCommit(() -> ZEconomyApiEvents.post(new RateChangeEvent(RateChangeEvent.Action.SET, from, to, Math.max(0.0D, rate))));
        effects.dispatch();
        return RateMutationResult.succeeded(from, to, rate, effects);
    }

    public static RatePairMutationResult setRatePair(String from, String to, double rate) {
        OperationFailure failure = EconomyValidationService.requirePositiveRate(rate);
        if (failure != null) {
            return RatePairMutationResult.failed(failure);
        }
        double reverse = 1.0D / rate;
        EconomyRateMutationService.setRate(from, to, rate);
        EconomyRateMutationService.setRate(to, from, reverse);
        EconomyOperationEffects effects = new EconomyOperationEffects()
            .requestSave()
            .afterCommit(() -> ZEconomyApiEvents.post(new RateChangeEvent(RateChangeEvent.Action.SET, from, to, Math.max(0.0D, rate))))
            .afterCommit(() -> ZEconomyApiEvents.post(new RateChangeEvent(RateChangeEvent.Action.SET, to, from, Math.max(0.0D, reverse))));
        effects.dispatch();
        return RatePairMutationResult.succeeded(from, to, rate, reverse, effects);
    }

    public static RateMutationResult clearRate(String from, String to) {
        if (!EconomyRateMutationService.removeRate(from, to)) {
            return RateMutationResult.failed(OperationFailure.RATE_NOT_FOUND);
        }
        EconomyOperationEffects effects = new EconomyOperationEffects()
            .requestSave()
            .afterCommit(() -> ZEconomyApiEvents.post(new RateChangeEvent(RateChangeEvent.Action.REMOVE, from, to, 0.0D)));
        effects.dispatch();
        return RateMutationResult.succeeded(from, to, 0.0D, effects);
    }

    public static RateResetResult resetRates() {
        EconomyRateMutationService.clearRates();
        EconomyRateMutationService.ensureDefaultRates();
        EconomyOperationEffects effects = new EconomyOperationEffects()
            .requestSave()
            .afterCommit(() -> ZEconomyApiEvents.post(new RateChangeEvent(RateChangeEvent.Action.RESET, "", "", 0.0D)));
        effects.dispatch();
        return RateResetResult.succeeded(EconomySnapshotReadService.exchangeRateCount(), effects);
    }

    public static BalanceMutationResult addBalance(UUID playerId, String currencyId, double amount) {
        OperationFailure failure = EconomyValidationService.requirePlayerId(playerId);
        if (failure == null) {
            failure = EconomyValidationService.requireNonZeroAmount(amount);
        }
        if (failure != null) {
            return BalanceMutationResult.failed(failure, 0.0D);
        }
        CurrencyHelper.getPlayerCurrencyServerData().newPlayer(playerId);
        double current = CurrencyHelper.getPlayerCurrencyServerData().getBalance(playerId, currencyId).value;
        if (amount < 0.0D && EconomyValidationService.requireSufficientWallet(playerId, currencyId, Math.abs(amount)) != null) {
            return BalanceMutationResult.failed(OperationFailure.INSUFFICIENT_FUNDS, current);
        }
        if (!CurrencyHelper.getPlayerCurrencyServerData().addCurrencyValue(playerId, currencyId, amount).isSuccess()) {
            return BalanceMutationResult.failed(OperationFailure.OPERATION_FAILED, current);
        }
        OperationResult operation = afterBalanceSuccess(playerId);
        return BalanceMutationResult.succeeded(CurrencyHelper.getPlayerCurrencyServerData().getBalance(playerId, currencyId).value, operation.effects());
    }

    public static BalanceMutationResult setBalance(UUID playerId, String currencyId, double amount) {
        OperationFailure failure = EconomyValidationService.requirePlayerId(playerId);
        if (failure == null) {
            failure = EconomyValidationService.requireNonNegativeAmount(amount);
        }
        if (failure != null) {
            return BalanceMutationResult.failed(failure, 0.0D);
        }
        CurrencyHelper.getPlayerCurrencyServerData().newPlayer(playerId);
        double current = CurrencyHelper.getPlayerCurrencyServerData().getBalance(playerId, currencyId).value;
        if (!CurrencyHelper.getPlayerCurrencyServerData().setCurrencyValue(playerId, currencyId, amount).isSuccess()) {
            return BalanceMutationResult.failed(OperationFailure.OPERATION_FAILED, current);
        }
        OperationResult operation = afterBalanceSuccess(playerId);
        return BalanceMutationResult.succeeded(CurrencyHelper.getPlayerCurrencyServerData().getBalance(playerId, currencyId).value, operation.effects());
    }

    public static boolean applyBalanceDeltas(BalanceDelta... deltas) {
        if (deltas == null || deltas.length == 0) {
            return true;
        }
        List<BalanceDelta> effectiveDeltas = new ArrayList<>();
        Set<UUID> playerIds = new LinkedHashSet<>();
        for (BalanceDelta delta : deltas) {
            if (delta == null || delta.playerId() == null || delta.currencyId() == null || delta.currencyId().isBlank() || delta.amount() == 0.0D) {
                continue;
            }
            effectiveDeltas.add(delta);
            playerIds.add(delta.playerId());
        }
        if (effectiveDeltas.isEmpty()) {
            return true;
        }
        for (UUID playerId : playerIds) {
            CurrencyHelper.getPlayerCurrencyServerData().newPlayer(playerId);
        }
        Map<UUID, Map<String, Double>> projectedBalances = new HashMap<>();
        for (BalanceDelta delta : effectiveDeltas) {
            Map<String, Double> playerProjection = projectedBalances.computeIfAbsent(delta.playerId(), ignored -> new HashMap<>());
            Double currentBalance = playerProjection.get(delta.currencyId());
            if (currentBalance == null) {
                io.zicteam.zeconomy.utils.ErrorCodeStruct<Double> current = CurrencyHelper.getPlayerCurrencyServerData().getBalance(delta.playerId(), delta.currencyId());
                if (!current.codes.isSuccess()) {
                    return false;
                }
                currentBalance = current.value;
            }
            double nextBalance = currentBalance + delta.amount();
            if (nextBalance < 0.0D) {
                return false;
            }
            playerProjection.put(delta.currencyId(), nextBalance);
        }
        final boolean[] ok = {true};
        CurrencyHelper.getPlayerCurrencyServerData().runBulkUpdate(() -> {
            for (BalanceDelta delta : effectiveDeltas) {
                if (!CurrencyHelper.getPlayerCurrencyServerData().addCurrencyValue(delta.playerId(), delta.currencyId(), delta.amount()).isSuccess()) {
                    ok[0] = false;
                    return;
                }
            }
        });
        return ok[0];
    }

    public static void syncIfOnline(UUID playerId) {
        ServerPlayer player = CurrencyHelper.isServerAccount(playerId) ? null : onlinePlayer(playerId);
        if (player != null) {
            CurrencyHelper.refreshPlayerState(player);
            return;
        }
        ServerPlayer anyPlayer = onlinePlayer(playerId);
        if (anyPlayer != null) {
            CurrencyHelper.syncPlayer(anyPlayer);
        }
    }

    private static ServerPlayer onlinePlayer(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        return net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer() == null
            ? null
            : net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerId);
    }

    private static OperationResult run(OperationFailure failure, BooleanSupplier action, ServerPlayer... players) {
        if (!action.getAsBoolean()) {
            return OperationResult.failed(failure);
        }
        EconomyOperationEffects effects = new EconomyOperationEffects().touchPlayers(players).requestSave();
        effects.dispatch();
        return OperationResult.succeeded(effects);
    }

    private static OperationResult afterBalanceSuccess(UUID playerId) {
        EconomyOperationEffects effects = new EconomyOperationEffects().touchPlayer(playerId).requestSave();
        effects.dispatch();
        return OperationResult.succeeded(effects);
    }

    private static EconomyOperationEffects scheduleCurrentServerSave() {
        EconomyOperationEffects effects = new EconomyOperationEffects().requestSave();
        effects.dispatch();
        return effects;
    }

    static TransferQuote computeTransferQuote(double amount, double feeRate) {
        double fee = amount * feeRate;
        return new TransferQuote(amount, fee, amount + fee);
    }

    static ExchangeQuote computeExchangeQuote(double amount, double rate, double feeRate) {
        double gross = amount * rate;
        double fee = gross * feeRate;
        return new ExchangeQuote(amount, rate, gross, fee, gross - fee);
    }

    static double computeRemainingDeposit(double depositedBefore, double amount) {
        return Math.max(0.0D, depositedBefore - amount);
    }

    static TransferPlan buildTransferPlan(UUID fromPlayerId, UUID toPlayerId, String currencyId, double amount, double feeRate) {
        TransferQuote quote = computeTransferQuote(amount, feeRate);
        return new TransferPlan(quote, buildTransferDeltas(fromPlayerId, toPlayerId, currencyId, quote));
    }

    static BalanceDelta[] buildTransferDeltas(UUID fromPlayerId, UUID toPlayerId, String currencyId, TransferQuote quote) {
        return new BalanceDelta[] {
            new BalanceDelta(fromPlayerId, currencyId, -quote.total()),
            new BalanceDelta(toPlayerId, currencyId, quote.amount()),
            quote.fee() > 0.0D ? new BalanceDelta(CurrencyHelper.getServerAccountUUID(), currencyId, quote.fee()) : null
        };
    }

    static BankWithdrawPlan buildBankWithdrawPlan(UUID playerId, String currencyId, double depositedBefore, double amount) {
        return new BankWithdrawPlan(computeRemainingDeposit(depositedBefore, amount), buildBankWithdrawDeltas(playerId, currencyId, amount));
    }

    static BankDepositPlan buildBankDepositPlan(UUID playerId, String currencyId, double amount) {
        return new BankDepositPlan(amount, new BalanceDelta[] {
            new BalanceDelta(playerId, currencyId, -amount),
            new BalanceDelta(CurrencyHelper.getServerAccountUUID(), currencyId, amount)
        });
    }

    static BalanceDelta[] buildBankWithdrawDeltas(UUID playerId, String currencyId, double amount) {
        return new BalanceDelta[] {
            new BalanceDelta(CurrencyHelper.getServerAccountUUID(), currencyId, -amount),
            new BalanceDelta(playerId, currencyId, amount)
        };
    }

    static ExchangePlan buildExchangePlan(UUID playerId, String fromCurrencyId, String toCurrencyId, double amount, double rate, double feeRate) {
        ExchangeQuote quote = computeExchangeQuote(amount, rate, feeRate);
        return new ExchangePlan(quote, buildExchangeDeltas(playerId, fromCurrencyId, toCurrencyId, quote));
    }

    static DailyClaimPlan buildDailyClaimPlan(UUID playerId, EconomyPolicyService.DailyRewardPolicy rewardPolicy) {
        return new DailyClaimPlan(rewardPolicy, new BalanceDelta[] {
            new BalanceDelta(CurrencyHelper.getServerAccountUUID(), ZEconomy.PRIMARY_CURRENCY_ID, -rewardPolicy.zReward()),
            new BalanceDelta(CurrencyHelper.getServerAccountUUID(), ZEconomy.SECONDARY_CURRENCY_ID, -rewardPolicy.bReward()),
            new BalanceDelta(playerId, ZEconomy.PRIMARY_CURRENCY_ID, rewardPolicy.zReward()),
            new BalanceDelta(playerId, ZEconomy.SECONDARY_CURRENCY_ID, rewardPolicy.bReward())
        });
    }

    static VaultDepositPlan buildVaultDepositPlan(UUID playerId, String currencyId, double amount) {
        return new VaultDepositPlan(amount, new BalanceDelta[] {
            new BalanceDelta(playerId, currencyId, -amount)
        });
    }

    static VaultWithdrawPlan buildVaultWithdrawPlan(UUID playerId, String currencyId, double vaultBefore, double amount) {
        return new VaultWithdrawPlan(Math.max(0.0D, vaultBefore - amount), new BalanceDelta[] {
            new BalanceDelta(playerId, currencyId, amount)
        });
    }

    static BalanceDelta[] buildExchangeDeltas(UUID playerId, String fromCurrencyId, String toCurrencyId, ExchangeQuote quote) {
        return new BalanceDelta[] {
            new BalanceDelta(playerId, fromCurrencyId, -quote.amount()),
            new BalanceDelta(CurrencyHelper.getServerAccountUUID(), fromCurrencyId, quote.amount()),
            new BalanceDelta(CurrencyHelper.getServerAccountUUID(), toCurrencyId, -quote.gross()),
            new BalanceDelta(playerId, toCurrencyId, quote.net())
        };
    }

    public enum OperationFailure {
        INVALID_PLAYER,
        INVALID_AMOUNT,
        INVALID_PIN,
        INSUFFICIENT_FUNDS,
        FEATURE_DISABLED,
        SELF_TARGET,
        EMPTY_HAND,
        EMPTY_MAILBOX,
        RATE_NOT_FOUND,
        ALREADY_CLAIMED,
        OPERATION_FAILED
    }

    public record OperationResult(boolean success, OperationFailure failure, EconomyOperationEffects effects) {
        public static OperationResult succeeded() {
            return new OperationResult(true, null, EconomyOperationEffects.none());
        }

        public static OperationResult failed(OperationFailure failure) {
            return new OperationResult(false, failure, EconomyOperationEffects.none());
        }

        public static OperationResult succeeded(EconomyOperationEffects effects) {
            return new OperationResult(true, null, effects == null ? EconomyOperationEffects.none() : effects);
        }
    }

    public record DailyOperationResult(OperationResult operation, ExtraEconomyData.DailyClaimResult reward) {
        public static DailyOperationResult succeeded(ExtraEconomyData.DailyClaimResult reward, EconomyOperationEffects effects) {
            return new DailyOperationResult(OperationResult.succeeded(effects), reward);
        }

        public static DailyOperationResult failed(OperationFailure failure, ExtraEconomyData.DailyClaimResult reward) {
            return new DailyOperationResult(OperationResult.failed(failure), reward);
        }

        public boolean success() {
            return operation.success();
        }

        public OperationFailure failure() {
            return operation.failure();
        }
    }

    public record BalanceMutationResult(boolean success, OperationFailure failure, double balanceAfter, EconomyOperationEffects effects) {
        public static BalanceMutationResult succeeded(double balanceAfter, EconomyOperationEffects effects) {
            return new BalanceMutationResult(true, null, balanceAfter, effects == null ? EconomyOperationEffects.none() : effects);
        }

        public static BalanceMutationResult failed(OperationFailure failure, double balanceAfter) {
            return new BalanceMutationResult(false, failure, balanceAfter, EconomyOperationEffects.none());
        }
    }

    public record BalanceDelta(UUID playerId, String currencyId, double amount) {
    }

    record TransferQuote(double amount, double fee, double total) {
    }

    record TransferPlan(TransferQuote quote, BalanceDelta[] deltas) {
    }

    record BankDepositPlan(double amount, BalanceDelta[] deltas) {
    }

    record ExchangeQuote(double amount, double rate, double gross, double fee, double net) {
    }

    record ExchangePlan(ExchangeQuote quote, BalanceDelta[] deltas) {
    }

    record BankWithdrawPlan(double depositedAfter, BalanceDelta[] deltas) {
    }

    record DailyClaimPlan(EconomyPolicyService.DailyRewardPolicy rewardPolicy, BalanceDelta[] deltas) {
    }

    record VaultDepositPlan(double amount, BalanceDelta[] deltas) {
    }

    record VaultWithdrawPlan(double vaultBalanceAfter, BalanceDelta[] deltas) {
    }

    public record MailSendResult(OperationResult operation, ItemStack sentStack, int targetPendingMail) {
        public static MailSendResult succeeded(ItemStack sentStack, int targetPendingMail, EconomyOperationEffects effects) {
            return new MailSendResult(OperationResult.succeeded(effects), sentStack, targetPendingMail);
        }

        public static MailSendResult failed(OperationFailure failure) {
            return new MailSendResult(OperationResult.failed(failure), ItemStack.EMPTY, 0);
        }

        public boolean success() {
            return operation.success();
        }

        public OperationFailure failure() {
            return operation.failure();
        }
    }

    public record MailClaimResult(OperationResult operation, List<ItemStack> items) {
        public static MailClaimResult succeeded(List<ItemStack> items, EconomyOperationEffects effects) {
            return new MailClaimResult(OperationResult.succeeded(effects), items);
        }

        public static MailClaimResult failed(OperationFailure failure) {
            return new MailClaimResult(OperationResult.failed(failure), List.of());
        }

        public boolean success() {
            return operation.success();
        }

        public OperationFailure failure() {
            return operation.failure();
        }

        public int itemCount() {
            return items == null ? 0 : items.size();
        }
    }

    public record RateMutationResult(OperationResult operation, String fromCurrencyId, String toCurrencyId, double rate) {
        public static RateMutationResult succeeded(String fromCurrencyId, String toCurrencyId, double rate, EconomyOperationEffects effects) {
            return new RateMutationResult(OperationResult.succeeded(effects), fromCurrencyId, toCurrencyId, rate);
        }

        public static RateMutationResult failed(OperationFailure failure) {
            return new RateMutationResult(OperationResult.failed(failure), "", "", 0.0D);
        }

        public boolean success() {
            return operation.success();
        }

        public OperationFailure failure() {
            return operation.failure();
        }
    }

    public record RatePairMutationResult(OperationResult operation, String fromCurrencyId, String toCurrencyId, double rate, double reverseRate) {
        public static RatePairMutationResult succeeded(String fromCurrencyId, String toCurrencyId, double rate, double reverseRate, EconomyOperationEffects effects) {
            return new RatePairMutationResult(OperationResult.succeeded(effects), fromCurrencyId, toCurrencyId, rate, reverseRate);
        }

        public static RatePairMutationResult failed(OperationFailure failure) {
            return new RatePairMutationResult(OperationResult.failed(failure), "", "", 0.0D, 0.0D);
        }

        public boolean success() {
            return operation.success();
        }

        public OperationFailure failure() {
            return operation.failure();
        }
    }

    public record RateResetResult(OperationResult operation, int rateCountAfterReset) {
        public static RateResetResult succeeded(int rateCountAfterReset, EconomyOperationEffects effects) {
            return new RateResetResult(OperationResult.succeeded(effects), rateCountAfterReset);
        }

        public boolean success() {
            return operation.success();
        }
    }

    public record TransferResult(OperationResult operation, double transferred, double fee, double senderBalanceAfter) {
        public static TransferResult succeeded(double transferred, double fee, double senderBalanceAfter, EconomyOperationEffects effects) {
            return new TransferResult(OperationResult.succeeded(effects), transferred, fee, senderBalanceAfter);
        }

        public static TransferResult fromPlan(TransferPlan plan, double senderBalanceAfter, EconomyOperationEffects effects) {
            return succeeded(plan.quote().amount(), plan.quote().fee(), senderBalanceAfter, effects);
        }

        public static TransferResult failed(OperationFailure failure) {
            return new TransferResult(OperationResult.failed(failure), 0.0D, 0.0D, 0.0D);
        }

        public boolean success() {
            return operation.success();
        }

        public OperationFailure failure() {
            return operation.failure();
        }
    }

    public record BankResult(OperationResult operation, double amount, double depositedAfter) {
        public static BankResult succeeded(double amount, double depositedAfter, EconomyOperationEffects effects) {
            return new BankResult(OperationResult.succeeded(effects), amount, depositedAfter);
        }

        public static BankResult fromDepositPlan(BankDepositPlan plan, double depositedAfter, EconomyOperationEffects effects) {
            return succeeded(plan.amount(), depositedAfter, effects);
        }

        public static BankResult fromWithdrawPlan(BankWithdrawPlan plan, EconomyOperationEffects effects) {
            double amount = plan.deltas().length > 1 && plan.deltas()[1] != null ? plan.deltas()[1].amount() : 0.0D;
            return succeeded(amount, plan.depositedAfter(), effects);
        }

        public static BankResult failed(OperationFailure failure) {
            return new BankResult(OperationResult.failed(failure), 0.0D, 0.0D);
        }

        public static BankResult failed(OperationFailure failure, double amount) {
            return new BankResult(OperationResult.failed(failure), amount, 0.0D);
        }

        public boolean success() {
            return operation.success();
        }

        public OperationFailure failure() {
            return operation.failure();
        }
    }

    public record VaultResult(OperationResult operation, double amount, double vaultBalanceAfter) {
        public static VaultResult succeeded(double amount, double vaultBalanceAfter, EconomyOperationEffects effects) {
            return new VaultResult(OperationResult.succeeded(effects), amount, vaultBalanceAfter);
        }

        public static VaultResult fromDepositPlan(VaultDepositPlan plan, double vaultBalanceAfter, EconomyOperationEffects effects) {
            return succeeded(plan.amount(), vaultBalanceAfter, effects);
        }

        public static VaultResult fromWithdrawPlan(VaultWithdrawPlan plan, double amount, EconomyOperationEffects effects) {
            return succeeded(amount, plan.vaultBalanceAfter(), effects);
        }

        public static VaultResult failed(OperationFailure failure) {
            return new VaultResult(OperationResult.failed(failure), 0.0D, 0.0D);
        }

        public static VaultResult failed(OperationFailure failure, double amount) {
            return new VaultResult(OperationResult.failed(failure), amount, 0.0D);
        }

        public boolean success() {
            return operation.success();
        }

        public OperationFailure failure() {
            return operation.failure();
        }
    }

    public record ExchangeResult(OperationResult operation, double rate, double gross, double net, double fee, double targetBalanceAfter) {
        public static ExchangeResult succeeded(double rate, double gross, double net, double fee, double targetBalanceAfter, EconomyOperationEffects effects) {
            return new ExchangeResult(OperationResult.succeeded(effects), rate, gross, net, fee, targetBalanceAfter);
        }

        public static ExchangeResult fromPlan(ExchangePlan plan, double targetBalanceAfter, EconomyOperationEffects effects) {
            return succeeded(plan.quote().rate(), plan.quote().gross(), plan.quote().net(), plan.quote().fee(), targetBalanceAfter, effects);
        }

        public static ExchangeResult failed(OperationFailure failure) {
            return new ExchangeResult(OperationResult.failed(failure), 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        public boolean success() {
            return operation.success();
        }

        public OperationFailure failure() {
            return operation.failure();
        }
    }
}
