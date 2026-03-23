package io.zicteam.zeconomy.system;

import io.zicteam.zeconomy.ZEconomy;
import io.zicteam.zeconomy.CustomPlayerData;
import io.zicteam.zeconomy.config.EconomyConfig;
import io.zicteam.zeconomy.currencies.compat.VaultBridge;
import io.zicteam.zeconomy.currencies.data.CurrencyPlayerData;
import io.zicteam.zeconomy.utils.CurrencyHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class AdminOperationService {
    private AdminOperationService() {
    }

    public static OperationResult setPlayerBalance(ServerPlayer player, String currency, double amount) {
        EconomyOperationService.BalanceMutationResult result = EconomyOperationService.setBalance(player == null ? null : player.getUUID(), currency, amount);
        if (result.success()) {
            return OperationResult.succeeded(result.effects());
        }
        return switch (result.failure()) {
            case INVALID_PLAYER -> OperationResult.failed(OperationFailure.INVALID_PLAYER);
            case INVALID_AMOUNT -> OperationResult.failed(OperationFailure.INVALID_AMOUNT);
            case INSUFFICIENT_FUNDS -> OperationResult.failed(OperationFailure.INSUFFICIENT_FUNDS);
            case INVALID_PIN, FEATURE_DISABLED, SELF_TARGET, EMPTY_HAND, EMPTY_MAILBOX, RATE_NOT_FOUND, ALREADY_CLAIMED, OPERATION_FAILED -> OperationResult.failed(OperationFailure.OPERATION_FAILED);
        };
    }

    public static TreasuryMutationResult setServerBalance(String currency, double amount) {
        if (EconomyValidationService.requireNonNegativeAmount(amount) != null) {
            return TreasuryMutationResult.failed(OperationFailure.INVALID_AMOUNT, 0.0D, 0.0D);
        }
        if (!CurrencyHelper.getPlayerCurrencyServerData().setCurrencyValue(CurrencyHelper.getServerAccountUUID(), currency, amount).isSuccess()) {
            return TreasuryMutationResult.failed(OperationFailure.OPERATION_FAILED, 0.0D, 0.0D);
        }
        double balance = CurrencyHelper.getPlayerCurrencyServerData().getBalance(CurrencyHelper.getServerAccountUUID(), currency).value;
        EconomyOperationEffects effects = dispatchCurrentServerSave();
        return TreasuryMutationResult.succeeded(balance, EconomySnapshotReadService.treasurySpendable(currency), effects);
    }

    public static TreasuryMutationResult giveServerBalance(String currency, double amount) {
        if (EconomyValidationService.requirePositiveAmount(amount) != null) {
            return TreasuryMutationResult.failed(OperationFailure.INVALID_AMOUNT, 0.0D, 0.0D);
        }
        if (!CurrencyHelper.getPlayerCurrencyServerData().addCurrencyValue(CurrencyHelper.getServerAccountUUID(), currency, amount).isSuccess()) {
            return TreasuryMutationResult.failed(OperationFailure.OPERATION_FAILED, 0.0D, 0.0D);
        }
        double balance = CurrencyHelper.getPlayerCurrencyServerData().getBalance(CurrencyHelper.getServerAccountUUID(), currency).value;
        EconomyOperationEffects effects = dispatchCurrentServerSave();
        return TreasuryMutationResult.succeeded(balance, EconomySnapshotReadService.treasurySpendable(currency), effects);
    }

    public static TreasuryMutationResult takeServerBalance(String currency, double amount) {
        if (EconomyValidationService.requirePositiveAmount(amount) != null) {
            return TreasuryMutationResult.failed(OperationFailure.INVALID_AMOUNT, 0.0D, 0.0D);
        }
        double spendable = EconomySnapshotReadService.treasurySpendable(currency);
        if (EconomyValidationService.requireSufficientTreasurySpendable(currency, amount) != null) {
            return TreasuryMutationResult.failed(OperationFailure.INSUFFICIENT_FUNDS, spendable, spendable);
        }
        if (!CurrencyHelper.getPlayerCurrencyServerData().addCurrencyValue(CurrencyHelper.getServerAccountUUID(), currency, -amount).isSuccess()) {
            return TreasuryMutationResult.failed(OperationFailure.OPERATION_FAILED, spendable, spendable);
        }
        double balance = CurrencyHelper.getPlayerCurrencyServerData().getBalance(CurrencyHelper.getServerAccountUUID(), currency).value;
        EconomyOperationEffects effects = dispatchCurrentServerSave();
        return TreasuryMutationResult.succeeded(balance, spendable, effects);
    }

    public static int reloadStorage(MinecraftServer server) {
        DataStorageManager.loadAll(server);
        CurrencyHelper.ensureDefaultCurrency();
        EconomyRateMutationService.ensureDefaultRates();
        EconomyOperationEffects effects = new EconomyOperationEffects().useServer(server).requestCurrencyDataSync();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            CurrencyPlayerData.SERVER.newPlayer(player);
            CustomPlayerData.SERVER.createData(player);
            effects.touchPlayer(player);
        }
        effects.dispatch();
        return server.getPlayerList().getPlayerCount();
    }

    public static OperationResult reconcilePlayer(ServerPlayer player, boolean saveAfter) {
        if (player == null) {
            return OperationResult.failed(OperationFailure.INVALID_PLAYER);
        }
        CurrencyHelper.ensureDefaultCurrency();
        CurrencyPlayerData.SERVER.newPlayer(player);
        CustomPlayerData.SERVER.createData(player);
        EconomyRateMutationService.ensureDefaultRates();
        EconomyOperationEffects effects = new EconomyOperationEffects().touchPlayer(player);
        if (saveAfter) {
            effects.requestSave();
        }
        effects.dispatch();
        return OperationResult.succeeded(effects);
    }

    public static DoctorFixResult doctorFixPlayer(ServerPlayer player, boolean saveAfter) {
        if (player == null) {
            return DoctorFixResult.failed(OperationFailure.INVALID_PLAYER, false, false);
        }
        OperationResult reconcile = reconcilePlayer(player, false);
        if (!reconcile.success()) {
            return DoctorFixResult.failed(reconcile.failure(), false, false);
        }
        boolean vaultAttempted = false;
        boolean vaultOk = false;
        if (EconomyConfig.ENABLE_VAULT_BRIDGE.get()) {
            vaultAttempted = true;
            CurrencyHelper.initVaultBridge(player.server);
            if (VaultBridge.isAvailable()) {
                vaultOk = CurrencyHelper.syncFromVaultOnJoin(player);
            }
        }
        EconomyOperationEffects effects = new EconomyOperationEffects().touchPlayer(player);
        if (saveAfter) {
            effects.requestSave();
        }
        effects.dispatch();
        return DoctorFixResult.succeeded(vaultAttempted, vaultOk, effects);
    }

    public static VaultSyncResult syncVault(ServerPlayer player) {
        if (player == null) {
            return VaultSyncResult.failed(false, OperationFailure.INVALID_PLAYER);
        }
        if (!EconomyConfig.ENABLE_VAULT_BRIDGE.get()) {
            return VaultSyncResult.failed(false, OperationFailure.FEATURE_DISABLED);
        }
        CurrencyHelper.initVaultBridge(player.server);
        if (!VaultBridge.isAvailable()) {
            return VaultSyncResult.failed(true, OperationFailure.SERVICE_UNAVAILABLE);
        }
        boolean ok = CurrencyHelper.syncFromVaultOnJoin(player);
        if (ok) {
            EconomyOperationEffects effects = new EconomyOperationEffects().touchPlayer(player).requestSave();
            effects.dispatch();
            return VaultSyncResult.succeeded(true, effects);
        }
        return VaultSyncResult.failed(true, OperationFailure.OPERATION_FAILED);
    }

    private static EconomyOperationEffects dispatchCurrentServerSave() {
        EconomyOperationEffects effects = new EconomyOperationEffects().requestSave();
        effects.dispatch();
        return effects;
    }

    public enum OperationFailure {
        INVALID_PLAYER,
        INVALID_AMOUNT,
        INSUFFICIENT_FUNDS,
        FEATURE_DISABLED,
        SERVICE_UNAVAILABLE,
        OPERATION_FAILED
    }

    public record OperationResult(boolean success, OperationFailure failure, EconomyOperationEffects effects) {
        public static OperationResult succeeded() {
            return new OperationResult(true, null, EconomyOperationEffects.none());
        }

        public static OperationResult succeeded(EconomyOperationEffects effects) {
            return new OperationResult(true, null, effects == null ? EconomyOperationEffects.none() : effects);
        }

        public static OperationResult failed(OperationFailure failure) {
            return new OperationResult(false, failure, EconomyOperationEffects.none());
        }
    }

    public record TreasuryMutationResult(boolean success, OperationFailure failure, double balanceAfter, double spendableBefore, EconomyOperationEffects effects) {
        public static TreasuryMutationResult succeeded(double balanceAfter, double spendableBefore, EconomyOperationEffects effects) {
            return new TreasuryMutationResult(true, null, balanceAfter, spendableBefore, effects == null ? EconomyOperationEffects.none() : effects);
        }

        public static TreasuryMutationResult failed(OperationFailure failure, double balanceAfter, double spendableBefore) {
            return new TreasuryMutationResult(false, failure, balanceAfter, spendableBefore, EconomyOperationEffects.none());
        }
    }

    public record DoctorFixResult(boolean success, OperationFailure failure, boolean vaultAttempted, boolean vaultOk, EconomyOperationEffects effects) {
        public static DoctorFixResult succeeded(boolean vaultAttempted, boolean vaultOk, EconomyOperationEffects effects) {
            return new DoctorFixResult(true, null, vaultAttempted, vaultOk, effects == null ? EconomyOperationEffects.none() : effects);
        }

        public static DoctorFixResult failed(OperationFailure failure, boolean vaultAttempted, boolean vaultOk) {
            return new DoctorFixResult(false, failure, vaultAttempted, vaultOk, EconomyOperationEffects.none());
        }
    }

    public record VaultSyncResult(boolean available, boolean success, OperationFailure failure, EconomyOperationEffects effects) {
        public static VaultSyncResult succeeded(boolean available, EconomyOperationEffects effects) {
            return new VaultSyncResult(available, true, null, effects == null ? EconomyOperationEffects.none() : effects);
        }

        public static VaultSyncResult failed(boolean available, OperationFailure failure) {
            return new VaultSyncResult(available, false, failure, EconomyOperationEffects.none());
        }
    }
}
