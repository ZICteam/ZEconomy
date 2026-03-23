package io.zicteam.zeconomy.system;

import io.zicteam.zeconomy.currencies.data.CurrencyPlayerData;
import io.zicteam.zeconomy.utils.CurrencyHelper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class EconomyReadService {
    private EconomyReadService() {
    }

    public static List<ExtraEconomyData.RichEntry> getTopRich(MinecraftServer server, String currency, int limit, boolean includeVault) {
        List<ExtraEconomyData.RichEntry> entries = new ArrayList<>();
        for (Map.Entry<UUID, java.util.LinkedList<CurrencyPlayerData.PlayerCurrency>> entry : CurrencyHelper.getPlayerCurrencyServerData().playersCurrencyMap.entrySet()) {
            UUID playerId = entry.getKey();
            if (CurrencyHelper.isServerAccount(playerId)) {
                continue;
            }
            double wallet = CurrencyHelper.getPlayerCurrencyServerData().getBalance(playerId, currency).value;
            double vault = includeVault ? EconomySnapshotReadService.vaultBalance(playerId, currency) : 0.0D;
            double total = wallet + vault;
            String name = playerId.toString();
            ServerPlayer online = server.getPlayerList().getPlayer(playerId);
            if (online != null) {
                name = online.getName().getString();
            }
            entries.add(new ExtraEconomyData.RichEntry(playerId, name, currency, total));
        }
        entries.sort(Comparator.comparingDouble((ExtraEconomyData.RichEntry value) -> value.amount()).reversed());
        return entries.subList(0, Math.min(limit, entries.size()));
    }
}
