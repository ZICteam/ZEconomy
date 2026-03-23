package io.zicteam.zeconomy.system;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.zicteam.zeconomy.ZEconomy;
import io.zicteam.zeconomy.utils.CurrencyHelper;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EconomyOperationExtendedPlansTest {
    @Test
    void bankDepositPlanMovesFundsFromPlayerToTreasury() {
        UUID playerId = UUID.randomUUID();

        EconomyOperationService.BankDepositPlan plan = EconomyOperationService.buildBankDepositPlan(playerId, "z_coin", 40.0D);

        assertEquals(40.0D, plan.amount());
        assertEquals(playerId, plan.deltas()[0].playerId());
        assertEquals("z_coin", plan.deltas()[0].currencyId());
        assertEquals(-40.0D, plan.deltas()[0].amount());
        assertEquals(CurrencyHelper.getServerAccountUUID(), plan.deltas()[1].playerId());
        assertEquals(40.0D, plan.deltas()[1].amount());
    }

    @Test
    void dailyClaimPlanMovesBothRewardsFromTreasuryToPlayer() {
        UUID playerId = UUID.randomUUID();
        EconomyPolicyService.DailyRewardPolicy rewardPolicy = new EconomyPolicyService.DailyRewardPolicy(700L, 6, 310.0D, 1.0D);

        EconomyOperationService.DailyClaimPlan plan = EconomyOperationService.buildDailyClaimPlan(playerId, rewardPolicy);

        assertEquals(rewardPolicy, plan.rewardPolicy());
        assertEquals(CurrencyHelper.getServerAccountUUID(), plan.deltas()[0].playerId());
        assertEquals(ZEconomy.PRIMARY_CURRENCY_ID, plan.deltas()[0].currencyId());
        assertEquals(-310.0D, plan.deltas()[0].amount());
        assertEquals(CurrencyHelper.getServerAccountUUID(), plan.deltas()[1].playerId());
        assertEquals(ZEconomy.SECONDARY_CURRENCY_ID, plan.deltas()[1].currencyId());
        assertEquals(-1.0D, plan.deltas()[1].amount());
        assertEquals(playerId, plan.deltas()[2].playerId());
        assertEquals(310.0D, plan.deltas()[2].amount());
        assertEquals(playerId, plan.deltas()[3].playerId());
        assertEquals(1.0D, plan.deltas()[3].amount());
    }
}
