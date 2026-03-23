package io.zicteam.zeconomy.system;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class EconomyOperationVaultPlansTest {
    @Test
    void vaultDepositPlanWithdrawsFundsFromWalletBeforeStateDeposit() {
        UUID playerId = UUID.randomUUID();

        EconomyOperationService.VaultDepositPlan plan = EconomyOperationService.buildVaultDepositPlan(playerId, "z_coin", 30.0D);

        assertEquals(30.0D, plan.amount());
        assertEquals(playerId, plan.deltas()[0].playerId());
        assertEquals("z_coin", plan.deltas()[0].currencyId());
        assertEquals(-30.0D, plan.deltas()[0].amount());
    }

    @Test
    void vaultWithdrawPlanCarriesRemainingVaultBalanceAndWalletCredit() {
        UUID playerId = UUID.randomUUID();

        EconomyOperationService.VaultWithdrawPlan plan = EconomyOperationService.buildVaultWithdrawPlan(playerId, "b_coin", 80.0D, 25.0D);

        assertEquals(55.0D, plan.vaultBalanceAfter());
        assertEquals(playerId, plan.deltas()[0].playerId());
        assertEquals("b_coin", plan.deltas()[0].currencyId());
        assertEquals(25.0D, plan.deltas()[0].amount());
    }

    @Test
    void vaultWithdrawPlanDoesNotGoBelowZero() {
        EconomyOperationService.VaultWithdrawPlan plan = EconomyOperationService.buildVaultWithdrawPlan(UUID.randomUUID(), "z_coin", 10.0D, 25.0D);

        assertEquals(0.0D, plan.vaultBalanceAfter());
    }
}
