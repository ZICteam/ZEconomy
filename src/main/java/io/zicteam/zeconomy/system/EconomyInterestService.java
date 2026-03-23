package io.zicteam.zeconomy.system;

import io.zicteam.zeconomy.ZEconomy;
import io.zicteam.zeconomy.api.event.BankTransactionEvent;
import io.zicteam.zeconomy.api.event.ZEconomyApiEvents;
import io.zicteam.zeconomy.config.EconomyConfig;
import io.zicteam.zeconomy.utils.CurrencyHelper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class EconomyInterestService {
    private EconomyInterestService() {
    }

    public static void tickHourlyInterest(MinecraftServer server) {
        long now = Instant.now().getEpochSecond();
        if (!ZEconomy.EXTRA_DATA.beginInterestSweep(now)) {
            return;
        }
        boolean mutated = false;
        long interval = 3600L;
        double rate = EconomyConfig.HOURLY_INTEREST_RATE.get();
        if (rate <= 0.0D) {
            return;
        }
        Map<String, Double> spendableByCurrency = new HashMap<>();
        EconomyOperationEffects effects = new EconomyOperationEffects().useServer(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID playerId = player.getUUID();
            long lastPayout = EconomySnapshotReadService.bankLastHourlyPayout(playerId, now);
            if (now - lastPayout < interval) {
                continue;
            }
            Map<String, Double> account = EconomySnapshotReadService.bankAccountDeposits(playerId);
            Map<String, Double> payouts = new HashMap<>();
            for (Map.Entry<String, Double> entry : account.entrySet()) {
                if (entry.getValue() <= 0.0D) {
                    continue;
                }
                double payout = entry.getValue() * rate;
                if (payout <= 0.0D) {
                    continue;
                }
                double spendable = spendableByCurrency.computeIfAbsent(entry.getKey(), EconomySnapshotReadService::treasurySpendable);
                if (spendable < payout) {
                    continue;
                }
                payouts.put(entry.getKey(), payout);
                spendableByCurrency.put(entry.getKey(), spendable - payout);
            }
            if (!payouts.isEmpty()) {
                List<EconomyOperationService.BalanceDelta> deltas = new ArrayList<>();
                for (Map.Entry<String, Double> payoutEntry : payouts.entrySet()) {
                    deltas.add(new EconomyOperationService.BalanceDelta(CurrencyHelper.getServerAccountUUID(), payoutEntry.getKey(), -payoutEntry.getValue()));
                    deltas.add(new EconomyOperationService.BalanceDelta(playerId, payoutEntry.getKey(), payoutEntry.getValue()));
                }
                if (!EconomyOperationService.applyBalanceDeltas(deltas.toArray(EconomyOperationService.BalanceDelta[]::new))) {
                    continue;
                }
                for (Map.Entry<String, Double> payoutEntry : payouts.entrySet()) {
                    String currencyId = payoutEntry.getKey();
                    double payoutAmount = payoutEntry.getValue();
                    double depositedAfter = EconomySnapshotReadService.bankDeposited(playerId, currencyId);
                    effects.afterCommit(() -> EconomyLogService.record("BANK_INTEREST", playerId, null, currencyId, payoutAmount, 0.0D, ""));
                    effects.afterCommit(() -> ZEconomyApiEvents.post(new BankTransactionEvent(BankTransactionEvent.Action.INTEREST, playerId, currencyId, payoutAmount, depositedAfter)));
                }
                effects.touchPlayer(player);
                mutated = true;
            }
            EconomyStateMutationService.setBankLastHourlyPayout(playerId, now);
            mutated = true;
        }
        if (mutated) {
            DataStorageManager.markDirty();
            effects.requestSave().dispatch();
        }
    }
}
