package io.zicteam.zeconomy.system;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class EconomyOperationPlansTest {
    @Test
    void transferPlanCombinesQuoteAndDeltas() {
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();

        EconomyOperationService.TransferPlan plan = EconomyOperationService.buildTransferPlan(from, to, "z_coin", 100.0D, 0.05D);

        assertEquals(100.0D, plan.quote().amount());
        assertEquals(5.0D, plan.quote().fee());
        assertEquals(105.0D, plan.quote().total());
        assertEquals(-105.0D, plan.deltas()[0].amount());
        assertEquals(100.0D, plan.deltas()[1].amount());
        assertEquals(5.0D, plan.deltas()[2].amount());
    }

    @Test
    void bankWithdrawPlanCarriesRemainingDepositAndTreasuryMove() {
        UUID playerId = UUID.randomUUID();

        EconomyOperationService.BankWithdrawPlan plan = EconomyOperationService.buildBankWithdrawPlan(playerId, "b_coin", 80.0D, 25.0D);

        assertEquals(55.0D, plan.depositedAfter());
        assertEquals(-25.0D, plan.deltas()[0].amount());
        assertEquals(25.0D, plan.deltas()[1].amount());
    }

    @Test
    void exchangePlanCombinesQuoteAndCrossCurrencyDeltas() {
        UUID playerId = UUID.randomUUID();

        EconomyOperationService.ExchangePlan plan = EconomyOperationService.buildExchangePlan(playerId, "z_coin", "b_coin", 20.0D, 4.0D, 0.10D);

        assertEquals(80.0D, plan.quote().gross());
        assertEquals(8.0D, plan.quote().fee());
        assertEquals(72.0D, plan.quote().net());
        assertEquals(-20.0D, plan.deltas()[0].amount());
        assertEquals(20.0D, plan.deltas()[1].amount());
        assertEquals(-80.0D, plan.deltas()[2].amount());
        assertEquals(72.0D, plan.deltas()[3].amount());
    }
}
