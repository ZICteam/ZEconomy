package io.zicteam.zeconomy.system;

import io.zicteam.zeconomy.CustomPlayerData;
import io.zicteam.zeconomy.ZEconomy;
import io.zicteam.zeconomy.network.ZEconomyNetwork;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

public final class EconomyPlayerSyncService {
    private EconomyPlayerSyncService() {
    }

    public static boolean syncPlayerMirror(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        EconomySnapshotReadService.PlayerSnapshot snapshot = EconomySnapshotReadService.player(player.getUUID());
        CustomPlayerData.Data data = CustomPlayerData.SERVER.getPlayerCustomData(player);
        Map<String, Double> bank = snapshot.bankBalances();
        Map<String, Double> vault = snapshot.vaultBalances();
        data.nbt.putDouble("bank_z_coin", bank.getOrDefault(ZEconomy.PRIMARY_CURRENCY_ID, 0.0D));
        data.nbt.putDouble("bank_b_coin", bank.getOrDefault(ZEconomy.SECONDARY_CURRENCY_ID, 0.0D));
        data.nbt.putDouble("vault_z_coin", vault.getOrDefault(ZEconomy.PRIMARY_CURRENCY_ID, 0.0D));
        data.nbt.putDouble("vault_b_coin", vault.getOrDefault(ZEconomy.SECONDARY_CURRENCY_ID, 0.0D));
        data.nbt.putInt("mail_pending", snapshot.pendingMail());
        data.nbt.putInt("daily_streak", snapshot.dailyStreak());
        data.nbt.putBoolean("vault_has_pin", snapshot.hasVaultPin());
        DataStorageManager.markDirty();
        return true;
    }

    public static void syncCustomData(ServerPlayer player) {
        if (player == null) {
            return;
        }
        ZEconomyNetwork.syncPlayer(player);
    }
}
