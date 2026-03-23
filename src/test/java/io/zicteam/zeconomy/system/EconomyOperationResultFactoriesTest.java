package io.zicteam.zeconomy.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class EconomyOperationResultFactoriesTest {
    @Test
    void transferResultFromPlanUsesQuoteValues() {
        EconomyOperationService.TransferPlan plan = EconomyOperationService.buildTransferPlan(UUID.randomUUID(), UUID.randomUUID(), "z_coin", 100.0D, 0.05D);

        EconomyOperationService.TransferResult result = EconomyOperationService.TransferResult.fromPlan(plan, 250.0D, EconomyOperationEffects.none());

        assertTrue(result.success());
        assertEquals(100.0D, result.transferred());
        assertEquals(5.0D, result.fee());
        assertEquals(250.0D, result.senderBalanceAfter());
    }

    @Test
    void bankResultFactoriesUsePlanBookkeeping() {
        EconomyOperationService.BankDepositPlan depositPlan = EconomyOperationService.buildBankDepositPlan(UUID.randomUUID(), "z_coin", 40.0D);
        EconomyOperationService.BankWithdrawPlan withdrawPlan = EconomyOperationService.buildBankWithdrawPlan(UUID.randomUUID(), "z_coin", 90.0D, 25.0D);

        EconomyOperationService.BankResult depositResult = EconomyOperationService.BankResult.fromDepositPlan(depositPlan, 140.0D, EconomyOperationEffects.none());
        EconomyOperationService.BankResult withdrawResult = EconomyOperationService.BankResult.fromWithdrawPlan(withdrawPlan, EconomyOperationEffects.none());

        assertEquals(40.0D, depositResult.amount());
        assertEquals(140.0D, depositResult.depositedAfter());
        assertEquals(25.0D, withdrawResult.amount());
        assertEquals(65.0D, withdrawResult.depositedAfter());
    }

    @Test
    void exchangeResultFromPlanUsesQuoteValues() {
        EconomyOperationService.ExchangePlan plan = EconomyOperationService.buildExchangePlan(UUID.randomUUID(), "z_coin", "b_coin", 20.0D, 4.0D, 0.10D);

        EconomyOperationService.ExchangeResult result = EconomyOperationService.ExchangeResult.fromPlan(plan, 500.0D, EconomyOperationEffects.none());

        assertTrue(result.success());
        assertEquals(4.0D, result.rate());
        assertEquals(80.0D, result.gross());
        assertEquals(8.0D, result.fee());
        assertEquals(72.0D, result.net());
        assertEquals(500.0D, result.targetBalanceAfter());
    }

    @Test
    void vaultAndDailyResultFactoriesExposePlannedState() {
        EconomyOperationService.VaultDepositPlan vaultDepositPlan = EconomyOperationService.buildVaultDepositPlan(UUID.randomUUID(), "b_coin", 15.0D);
        EconomyOperationService.VaultWithdrawPlan vaultWithdrawPlan = EconomyOperationService.buildVaultWithdrawPlan(UUID.randomUUID(), "b_coin", 50.0D, 20.0D);
        ExtraEconomyData.DailyClaimResult reward = new ExtraEconomyData.DailyClaimResult(true, 280.0D, 1.0D, 4);

        EconomyOperationService.VaultResult depositResult = EconomyOperationService.VaultResult.fromDepositPlan(vaultDepositPlan, 65.0D, EconomyOperationEffects.none());
        EconomyOperationService.VaultResult withdrawResult = EconomyOperationService.VaultResult.fromWithdrawPlan(vaultWithdrawPlan, 20.0D, EconomyOperationEffects.none());
        EconomyOperationService.DailyOperationResult dailyResult = EconomyOperationService.DailyOperationResult.succeeded(reward, EconomyOperationEffects.none());

        assertEquals(15.0D, depositResult.amount());
        assertEquals(65.0D, depositResult.vaultBalanceAfter());
        assertEquals(20.0D, withdrawResult.amount());
        assertEquals(30.0D, withdrawResult.vaultBalanceAfter());
        assertTrue(dailyResult.success());
        assertEquals(280.0D, dailyResult.reward().zReward());
        assertEquals(1.0D, dailyResult.reward().bReward());
        assertEquals(4, dailyResult.reward().streak());
    }
}
