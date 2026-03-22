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

    public static void setPlayerBalance(ServerPlayer player, String currency, double amount) {
        CurrencyHelper.getPlayerCurrencyServerData().setCurrencyValue(player, currency, amount);
        CurrencyHelper.refreshPlayerState(player);
    }

    public static void setServerBalance(String currency, double amount) {
        CurrencyHelper.getPlayerCurrencyServerData().setCurrencyValue(CurrencyHelper.getServerAccountUUID(), currency, amount);
    }

    public static double giveServerBalance(String currency, double amount) {
        CurrencyHelper.getPlayerCurrencyServerData().addCurrencyValue(CurrencyHelper.getServerAccountUUID(), currency, amount);
        return CurrencyHelper.getPlayerCurrencyServerData().getBalance(CurrencyHelper.getServerAccountUUID(), currency).value;
    }

    public static TreasuryTakeResult takeServerBalance(String currency, double amount) {
        double spendable = ZEconomy.EXTRA_DATA.getServerSpendable(currency);
        if (spendable < amount) {
            return new TreasuryTakeResult(false, spendable, spendable);
        }
        CurrencyHelper.getPlayerCurrencyServerData().addCurrencyValue(CurrencyHelper.getServerAccountUUID(), currency, -amount);
        double balance = CurrencyHelper.getPlayerCurrencyServerData().getBalance(CurrencyHelper.getServerAccountUUID(), currency).value;
        return new TreasuryTakeResult(true, spendable, balance);
    }

    public static int reloadStorage(MinecraftServer server) {
        DataStorageManager.loadAll(server);
        CurrencyHelper.ensureDefaultCurrency();
        ZEconomy.EXTRA_DATA.ensureDefaultRates();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            CurrencyPlayerData.SERVER.newPlayer(player);
            CustomPlayerData.SERVER.createData(player);
            CurrencyHelper.refreshPlayerState(player);
        }
        CurrencyHelper.syncCurrencyData(server);
        return server.getPlayerList().getPlayerCount();
    }

    public static void reconcilePlayer(ServerPlayer player, boolean saveAfter) {
        CurrencyHelper.ensureDefaultCurrency();
        CurrencyPlayerData.SERVER.newPlayer(player);
        CustomPlayerData.SERVER.createData(player);
        ZEconomy.EXTRA_DATA.ensureDefaultRates();
        CurrencyHelper.refreshPlayerState(player);
        if (saveAfter && player.server != null) {
            CurrencyHelper.saveAll(player.server);
        }
    }

    public static DoctorFixResult doctorFixPlayer(ServerPlayer player, boolean saveAfter) {
        reconcilePlayer(player, false);
        boolean vaultAttempted = false;
        boolean vaultOk = false;
        if (EconomyConfig.ENABLE_VAULT_BRIDGE.get()) {
            vaultAttempted = true;
            CurrencyHelper.initVaultBridge(player.server);
            if (VaultBridge.isAvailable()) {
                vaultOk = CurrencyHelper.syncFromVaultOnJoin(player);
                if (vaultOk) {
                    CurrencyHelper.refreshPlayerState(player);
                }
            }
        }
        if (saveAfter && player.server != null) {
            CurrencyHelper.saveAll(player.server);
        }
        return new DoctorFixResult(vaultAttempted, vaultOk);
    }

    public static VaultSyncResult syncVault(ServerPlayer player) {
        if (!EconomyConfig.ENABLE_VAULT_BRIDGE.get()) {
            return new VaultSyncResult(false, false);
        }
        CurrencyHelper.initVaultBridge(player.server);
        if (!VaultBridge.isAvailable()) {
            return new VaultSyncResult(true, false);
        }
        boolean ok = CurrencyHelper.syncFromVaultOnJoin(player);
        if (ok) {
            CurrencyHelper.refreshPlayerState(player);
        }
        return new VaultSyncResult(true, ok);
    }

    public record TreasuryTakeResult(boolean success, double spendableBefore, double balanceAfter) {
    }

    public record DoctorFixResult(boolean vaultAttempted, boolean vaultOk) {
    }

    public record VaultSyncResult(boolean available, boolean success) {
    }
}
