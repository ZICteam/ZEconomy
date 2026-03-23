package io.zicteam.zeconomy.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class EconomyOperationFailureFactoriesTest {
    @Test
    void bankFailureFactoryCanPreserveRequestedAmount() {
        EconomyOperationService.BankResult result = EconomyOperationService.BankResult.failed(
            EconomyOperationService.OperationFailure.INSUFFICIENT_FUNDS,
            25.0D
        );

        assertFalse(result.success());
        assertEquals(EconomyOperationService.OperationFailure.INSUFFICIENT_FUNDS, result.failure());
        assertEquals(25.0D, result.amount());
        assertEquals(0.0D, result.depositedAfter());
    }

    @Test
    void vaultFailureFactoryCanPreserveRequestedAmount() {
        EconomyOperationService.VaultResult result = EconomyOperationService.VaultResult.failed(
            EconomyOperationService.OperationFailure.INVALID_PIN,
            15.0D
        );

        assertFalse(result.success());
        assertEquals(EconomyOperationService.OperationFailure.INVALID_PIN, result.failure());
        assertEquals(15.0D, result.amount());
        assertEquals(0.0D, result.vaultBalanceAfter());
    }

    @Test
    void dailyFailureFactoryPreservesRewardPayload() {
        ExtraEconomyData.DailyClaimResult reward = new ExtraEconomyData.DailyClaimResult(false, 0.0D, 0.0D, 6);

        EconomyOperationService.DailyOperationResult result = EconomyOperationService.DailyOperationResult.failed(
            EconomyOperationService.OperationFailure.ALREADY_CLAIMED,
            reward
        );

        assertFalse(result.success());
        assertEquals(EconomyOperationService.OperationFailure.ALREADY_CLAIMED, result.failure());
        assertEquals(6, result.reward().streak());
    }
}
