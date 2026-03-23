package io.zicteam.zeconomy.system;

import io.zicteam.zeconomy.ZEconomy;
import java.util.List;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;

public final class EconomyStateMutationService {
    private EconomyStateMutationService() {
    }

    public static double addBankDeposit(UUID playerId, String currencyId, double amount) {
        ZEconomy.EXTRA_DATA.addBankDepositState(playerId, currencyId, amount);
        return EconomySnapshotReadService.bankDeposited(playerId, currencyId);
    }

    public static double setBankDeposit(UUID playerId, String currencyId, double amount) {
        ZEconomy.EXTRA_DATA.setBankDepositState(playerId, currencyId, amount);
        return EconomySnapshotReadService.bankDeposited(playerId, currencyId);
    }

    public static void setBankLastHourlyPayout(UUID playerId, long epochSec) {
        ZEconomy.EXTRA_DATA.setBankLastHourlyPayout(playerId, epochSec);
    }

    public static double addVaultBalance(UUID playerId, String currencyId, double amount) {
        ZEconomy.EXTRA_DATA.addVaultDepositState(playerId, currencyId, amount);
        return EconomySnapshotReadService.vaultBalance(playerId, currencyId);
    }

    public static double setVaultBalance(UUID playerId, String currencyId, double amount) {
        ZEconomy.EXTRA_DATA.setVaultDepositState(playerId, currencyId, amount);
        return EconomySnapshotReadService.vaultBalance(playerId, currencyId);
    }

    public static boolean setVaultPin(UUID playerId, String pin) {
        return ZEconomy.EXTRA_DATA.setVaultPin(playerId, pin);
    }

    public static ExtraEconomyData.DailyClaimResult applyDailyClaim(UUID playerId, EconomyPolicyService.DailyRewardPolicy rewardPolicy) {
        return ZEconomy.EXTRA_DATA.applyDailyClaimState(
            playerId,
            rewardPolicy.claimDay(),
            rewardPolicy.nextStreak(),
            rewardPolicy.zReward(),
            rewardPolicy.bReward()
        );
    }

    public static int sendMail(UUID targetPlayerId, ItemStack stack) {
        ZEconomy.EXTRA_DATA.sendMail(targetPlayerId, stack);
        return EconomySnapshotReadService.pendingMail(targetPlayerId);
    }

    public static List<ItemStack> claimMail(UUID playerId) {
        return ZEconomy.EXTRA_DATA.claimMail(playerId);
    }
}
