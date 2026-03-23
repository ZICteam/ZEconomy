package io.zicteam.zeconomy.system;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import io.zicteam.zeconomy.ZEconomy;
import io.zicteam.zeconomy.config.EconomyConfig;
import io.zicteam.zeconomy.utils.CurrencyHelper;

public final class EconomyPolicyService {
    private EconomyPolicyService() {
    }

    public static EconomyOperationService.OperationFailure canTransfer(ServerPlayer from, ServerPlayer to, String currencyId, double amount) {
        EconomyOperationService.OperationFailure failure = EconomyValidationService.requirePlayer(from);
        if (failure == null) {
            failure = EconomyValidationService.requirePlayer(to);
        }
        if (failure == null) {
            failure = EconomyValidationService.requirePositiveAmount(amount);
        }
        if (failure == null) {
            double total = amount + (amount * EconomyConfig.TRANSFER_FEE_RATE.get());
            failure = EconomyValidationService.requireSufficientWallet(from, currencyId, total);
        }
        return failure;
    }

    public static EconomyOperationService.OperationFailure canDepositBank(ServerPlayer player, String currencyId, double amount) {
        EconomyOperationService.OperationFailure failure = EconomyValidationService.requirePlayer(player);
        if (failure == null) {
            failure = EconomyValidationService.requirePositiveAmount(amount);
        }
        if (failure == null) {
            failure = EconomyValidationService.requireSufficientWallet(player, currencyId, amount);
        }
        return failure;
    }

    public static EconomyOperationService.OperationFailure canWithdrawBank(ServerPlayer player, String currencyId, double amount) {
        EconomyOperationService.OperationFailure failure = EconomyValidationService.requirePlayer(player);
        if (failure == null) {
            failure = EconomyValidationService.requirePositiveAmount(amount);
        }
        if (failure == null) {
            failure = evaluateBankWithdrawPolicy(
                EconomySnapshotReadService.bankDeposited(player.getUUID(), currencyId),
                EconomySnapshotReadService.treasurySpendable(currencyId),
                amount
            );
        }
        return failure;
    }

    public static EconomyOperationService.OperationFailure canExchange(ServerPlayer player, String fromCurrencyId, String toCurrencyId, double amount) {
        EconomyOperationService.OperationFailure failure = EconomyValidationService.requirePlayer(player);
        if (failure == null) {
            failure = EconomyValidationService.requirePositiveAmount(amount);
        }
        if (failure == null) {
            failure = EconomyValidationService.requireExchangeEnabled();
        }
        double rate = EconomySnapshotReadService.exchangeRate(fromCurrencyId, toCurrencyId);
        if (failure == null) {
            double walletBalance = CurrencyHelper.getPlayerCurrencyServerData().getBalance(player, fromCurrencyId).value;
            double serverTargetBalance = CurrencyHelper.getPlayerCurrencyServerData().getBalance(CurrencyHelper.getServerAccountUUID(), toCurrencyId).value;
            failure = evaluateExchangePolicy(amount, rate, walletBalance, serverTargetBalance);
        }
        return failure;
    }

    public static EconomyOperationService.OperationFailure canAccessVault(ServerPlayer player, String pin, String currencyId, double amount, boolean withdraw) {
        EconomyOperationService.OperationFailure failure = EconomyValidationService.requirePlayer(player);
        if (failure == null) {
            failure = EconomyValidationService.requirePositiveAmount(amount);
        }
        if (failure == null) {
            failure = EconomyValidationService.requireValidPin(pin);
        }
        if (failure == null && !EconomySnapshotReadService.hasVaultPin(player.getUUID())) {
            failure = EconomyOperationService.OperationFailure.INVALID_PIN;
        }
        if (failure == null && !EconomySnapshotReadService.matchesVaultPin(player.getUUID(), pin)) {
            failure = EconomyOperationService.OperationFailure.INVALID_PIN;
        }
        if (failure == null && withdraw) {
            if (EconomySnapshotReadService.vaultBalance(player.getUUID(), currencyId) < amount) {
                failure = EconomyOperationService.OperationFailure.INSUFFICIENT_FUNDS;
            }
        }
        if (failure == null && !withdraw) {
            failure = EconomyValidationService.requireSufficientWallet(player, currencyId, amount);
        }
        return failure;
    }

    public static EconomyOperationService.OperationFailure canClaimDaily(ServerPlayer player) {
        EconomyOperationService.OperationFailure failure = EconomyValidationService.requirePlayer(player);
        if (failure == null) {
            DailyRewardPolicy rewardPolicy = dailyRewardPolicy(player.getUUID());
            failure = evaluateDailyClaimPolicy(
                EconomySnapshotReadService.hasClaimedDailyToday(player.getUUID()),
                EconomySnapshotReadService.treasurySpendable(ZEconomy.PRIMARY_CURRENCY_ID),
                EconomySnapshotReadService.treasurySpendable(ZEconomy.SECONDARY_CURRENCY_ID),
                rewardPolicy
            );
        }
        return failure;
    }

    public static DailyRewardPolicy dailyRewardPolicy(UUID playerId) {
        long today = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toEpochDay();
        long lastClaimDay = EconomySnapshotReadService.dailyLastClaimDay(playerId);
        int currentStreak = EconomySnapshotReadService.dailyStreak(playerId);
        return computeDailyRewardPolicy(today, lastClaimDay, currentStreak, EconomyConfig.DAILY_REWARD_Z.get(), EconomyConfig.DAILY_REWARD_B.get());
    }

    static DailyRewardPolicy computeDailyRewardPolicy(long claimDay, long lastClaimDay, int currentStreak, double baseZReward, double baseBReward) {
        int nextStreak = lastClaimDay == claimDay - 1 ? currentStreak + 1 : 1;
        double zReward = baseZReward + Math.min(7, nextStreak) * 10.0D;
        double bReward = baseBReward + (nextStreak >= 7 ? 1.0D : 0.0D);
        return new DailyRewardPolicy(claimDay, nextStreak, zReward, bReward);
    }

    static EconomyOperationService.OperationFailure evaluateBankWithdrawPolicy(double deposited, double treasurySpendable, double amount) {
        if (deposited < amount) {
            return EconomyOperationService.OperationFailure.INSUFFICIENT_FUNDS;
        }
        return treasurySpendable < amount ? EconomyOperationService.OperationFailure.INSUFFICIENT_FUNDS : null;
    }

    static EconomyOperationService.OperationFailure evaluateExchangePolicy(double amount, double rate, double walletBalance, double serverTargetBalance) {
        if (EconomyValidationService.requirePositiveRate(rate) != null) {
            return EconomyOperationService.OperationFailure.OPERATION_FAILED;
        }
        if (walletBalance < amount) {
            return EconomyOperationService.OperationFailure.INSUFFICIENT_FUNDS;
        }
        double gross = amount * rate;
        return serverTargetBalance < gross ? EconomyOperationService.OperationFailure.INSUFFICIENT_FUNDS : null;
    }

    static EconomyOperationService.OperationFailure evaluateDailyClaimPolicy(
        boolean alreadyClaimedToday,
        double primaryTreasurySpendable,
        double secondaryTreasurySpendable,
        DailyRewardPolicy rewardPolicy
    ) {
        if (alreadyClaimedToday) {
            return EconomyOperationService.OperationFailure.ALREADY_CLAIMED;
        }
        if (primaryTreasurySpendable < rewardPolicy.zReward()) {
            return EconomyOperationService.OperationFailure.INSUFFICIENT_FUNDS;
        }
        return secondaryTreasurySpendable < rewardPolicy.bReward()
            ? EconomyOperationService.OperationFailure.INSUFFICIENT_FUNDS
            : null;
    }

    public record DailyRewardPolicy(long claimDay, int nextStreak, double zReward, double bReward) {
    }
}
