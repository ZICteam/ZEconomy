package io.zicteam.zeconomy.system;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import io.zicteam.zeconomy.ZEconomy;
import io.zicteam.zeconomy.utils.CurrencyHelper;

public final class EconomyOperationService {
    private EconomyOperationService() {
    }

    public static boolean transfer(ServerPlayer from, ServerPlayer to, String currencyId, double amount) {
        if (from == null || to == null) {
            return false;
        }
        boolean ok = ZEconomy.EXTRA_DATA.transferWithFee(from, to, currencyId, amount);
        if (ok) {
            CurrencyHelper.refreshPlayersState(from, to);
        }
        return ok;
    }

    public static boolean depositBank(ServerPlayer player, String currencyId, double amount) {
        if (player == null) {
            return false;
        }
        boolean ok = ZEconomy.EXTRA_DATA.depositToBank(player, currencyId, amount);
        if (ok) {
            CurrencyHelper.refreshPlayerState(player);
        }
        return ok;
    }

    public static boolean withdrawBank(ServerPlayer player, String currencyId, double amount) {
        if (player == null) {
            return false;
        }
        boolean ok = ZEconomy.EXTRA_DATA.withdrawFromBank(player, currencyId, amount);
        if (ok) {
            CurrencyHelper.refreshPlayerState(player);
        }
        return ok;
    }

    public static boolean exchange(ServerPlayer player, String fromCurrencyId, String toCurrencyId, double amount) {
        if (player == null) {
            return false;
        }
        boolean ok = ZEconomy.EXTRA_DATA.exchangeCurrency(player, fromCurrencyId, toCurrencyId, amount);
        if (ok) {
            CurrencyHelper.refreshPlayerState(player);
        }
        return ok;
    }

    public static boolean depositVault(ServerPlayer player, String pin, String currencyId, double amount) {
        if (player == null) {
            return false;
        }
        boolean ok = ZEconomy.EXTRA_DATA.vaultDeposit(player, pin, currencyId, amount);
        if (ok) {
            CurrencyHelper.refreshPlayerState(player);
        }
        return ok;
    }

    public static boolean withdrawVault(ServerPlayer player, String pin, String currencyId, double amount) {
        if (player == null) {
            return false;
        }
        boolean ok = ZEconomy.EXTRA_DATA.vaultWithdraw(player, pin, currencyId, amount);
        if (ok) {
            CurrencyHelper.refreshPlayerState(player);
        }
        return ok;
    }

    public static ExtraEconomyData.DailyClaimResult claimDaily(ServerPlayer player) {
        if (player == null) {
            return new ExtraEconomyData.DailyClaimResult(false, 0.0D, 0.0D, 0);
        }
        ExtraEconomyData.DailyClaimResult result = ZEconomy.EXTRA_DATA.claimDaily(player);
        if (result.success()) {
            CurrencyHelper.refreshPlayerState(player);
        }
        return result;
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
}
