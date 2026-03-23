package io.zicteam.zeconomy.system;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import io.zicteam.zeconomy.config.EconomyConfig;
import io.zicteam.zeconomy.utils.CurrencyHelper;

public final class EconomyValidationService {
    private EconomyValidationService() {
    }

    public static EconomyOperationService.OperationFailure requirePlayer(ServerPlayer player) {
        return player == null ? EconomyOperationService.OperationFailure.INVALID_PLAYER : null;
    }

    public static EconomyOperationService.OperationFailure requirePlayerId(java.util.UUID playerId) {
        return playerId == null ? EconomyOperationService.OperationFailure.INVALID_PLAYER : null;
    }

    public static EconomyOperationService.OperationFailure requirePositiveAmount(double amount) {
        return amount <= 0.0D ? EconomyOperationService.OperationFailure.INVALID_AMOUNT : null;
    }

    public static EconomyOperationService.OperationFailure requireNonNegativeAmount(double amount) {
        return amount < 0.0D ? EconomyOperationService.OperationFailure.INVALID_AMOUNT : null;
    }

    public static EconomyOperationService.OperationFailure requireNonZeroAmount(double amount) {
        return amount == 0.0D ? EconomyOperationService.OperationFailure.INVALID_AMOUNT : null;
    }

    public static EconomyOperationService.OperationFailure requireValidPin(String pin) {
        if (pin == null || pin.length() < 4 || pin.length() > 12) {
            return EconomyOperationService.OperationFailure.INVALID_PIN;
        }
        for (char c : pin.toCharArray()) {
            if (!Character.isDigit(c)) {
                return EconomyOperationService.OperationFailure.INVALID_PIN;
            }
        }
        return null;
    }

    public static EconomyOperationService.OperationFailure requireExchangeEnabled() {
        return EconomyConfig.ENABLE_BCOIN_EXCHANGE.get() ? null : EconomyOperationService.OperationFailure.FEATURE_DISABLED;
    }

    public static EconomyOperationService.OperationFailure requireMailboxEnabled() {
        return EconomyConfig.ENABLE_MAILBOX.get() ? null : EconomyOperationService.OperationFailure.FEATURE_DISABLED;
    }

    public static EconomyOperationService.OperationFailure requireNotSelf(ServerPlayer actor, ServerPlayer target) {
        if (actor == null || target == null) {
            return EconomyOperationService.OperationFailure.INVALID_PLAYER;
        }
        return actor.getUUID().equals(target.getUUID()) ? EconomyOperationService.OperationFailure.SELF_TARGET : null;
    }

    public static EconomyOperationService.OperationFailure requireHeldItem(ItemStack stack) {
        return stack == null || stack.isEmpty() ? EconomyOperationService.OperationFailure.EMPTY_HAND : null;
    }

    static EconomyOperationService.OperationFailure evaluateMailSendValidation(boolean mailboxEnabled, boolean selfTarget, boolean hasItem) {
        if (!mailboxEnabled) {
            return EconomyOperationService.OperationFailure.FEATURE_DISABLED;
        }
        if (selfTarget) {
            return EconomyOperationService.OperationFailure.SELF_TARGET;
        }
        return hasItem ? null : EconomyOperationService.OperationFailure.EMPTY_HAND;
    }

    public static EconomyOperationService.OperationFailure requirePositiveRate(double rate) {
        return rate <= 0.0D ? EconomyOperationService.OperationFailure.INVALID_AMOUNT : null;
    }

    public static EconomyOperationService.OperationFailure requireSufficientWallet(ServerPlayer player, String currencyId, double amount) {
        double balance = CurrencyHelper.getPlayerCurrencyServerData().getBalance(player, currencyId).value;
        return balance < amount ? EconomyOperationService.OperationFailure.INSUFFICIENT_FUNDS : null;
    }

    public static EconomyOperationService.OperationFailure requireSufficientWallet(java.util.UUID playerId, String currencyId, double amount) {
        double balance = CurrencyHelper.getPlayerCurrencyServerData().getBalance(playerId, currencyId).value;
        return balance < amount ? EconomyOperationService.OperationFailure.INSUFFICIENT_FUNDS : null;
    }

    public static EconomyOperationService.OperationFailure requireSufficientTreasurySpendable(String currencyId, double amount) {
        return EconomySnapshotReadService.treasurySpendable(currencyId) < amount
            ? EconomyOperationService.OperationFailure.INSUFFICIENT_FUNDS
            : null;
    }
}
