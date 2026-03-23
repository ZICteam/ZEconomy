package io.zicteam.zeconomy.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class EconomyPolicyServiceTest {
    @Test
    void dailyRewardPolicyIncrementsStreakAfterConsecutiveClaim() {
        EconomyPolicyService.DailyRewardPolicy policy = EconomyPolicyService.computeDailyRewardPolicy(100L, 99L, 3, 250.0D, 1.0D);

        assertEquals(100L, policy.claimDay());
        assertEquals(4, policy.nextStreak());
        assertEquals(290.0D, policy.zReward());
        assertEquals(1.0D, policy.bReward());
    }

    @Test
    void dailyRewardPolicyResetsAfterGap() {
        EconomyPolicyService.DailyRewardPolicy policy = EconomyPolicyService.computeDailyRewardPolicy(100L, 97L, 6, 250.0D, 1.0D);

        assertEquals(1, policy.nextStreak());
        assertEquals(260.0D, policy.zReward());
        assertEquals(1.0D, policy.bReward());
    }

    @Test
    void dailyRewardPolicyCapsBonusAtSevenDayStreak() {
        EconomyPolicyService.DailyRewardPolicy policy = EconomyPolicyService.computeDailyRewardPolicy(100L, 99L, 9, 250.0D, 1.0D);

        assertEquals(10, policy.nextStreak());
        assertEquals(320.0D, policy.zReward());
        assertEquals(2.0D, policy.bReward());
    }

    @Test
    void bankWithdrawPolicyFailsWhenDepositIsTooSmall() {
        assertEquals(
            EconomyOperationService.OperationFailure.INSUFFICIENT_FUNDS,
            EconomyPolicyService.evaluateBankWithdrawPolicy(40.0D, 500.0D, 50.0D)
        );
    }

    @Test
    void bankWithdrawPolicyFailsWhenTreasurySpendableIsTooSmall() {
        assertEquals(
            EconomyOperationService.OperationFailure.INSUFFICIENT_FUNDS,
            EconomyPolicyService.evaluateBankWithdrawPolicy(500.0D, 40.0D, 50.0D)
        );
    }

    @Test
    void bankWithdrawPolicyPassesWhenDepositAndTreasuryCoverAmount() {
        assertNull(EconomyPolicyService.evaluateBankWithdrawPolicy(500.0D, 500.0D, 50.0D));
    }

    @Test
    void exchangePolicyFailsWhenRateIsInvalid() {
        assertEquals(
            EconomyOperationService.OperationFailure.OPERATION_FAILED,
            EconomyPolicyService.evaluateExchangePolicy(10.0D, 0.0D, 100.0D, 1000.0D)
        );
    }

    @Test
    void exchangePolicyFailsWhenWalletIsTooSmall() {
        assertEquals(
            EconomyOperationService.OperationFailure.INSUFFICIENT_FUNDS,
            EconomyPolicyService.evaluateExchangePolicy(10.0D, 2.0D, 5.0D, 1000.0D)
        );
    }

    @Test
    void exchangePolicyFailsWhenTreasuryCannotCoverGrossTargetAmount() {
        assertEquals(
            EconomyOperationService.OperationFailure.INSUFFICIENT_FUNDS,
            EconomyPolicyService.evaluateExchangePolicy(10.0D, 2.0D, 100.0D, 15.0D)
        );
    }

    @Test
    void exchangePolicyPassesWhenAllInputsAreSufficient() {
        assertNull(EconomyPolicyService.evaluateExchangePolicy(10.0D, 2.0D, 100.0D, 25.0D));
    }

    @Test
    void dailyClaimPolicyFailsWhenAlreadyClaimed() {
        EconomyPolicyService.DailyRewardPolicy rewardPolicy = new EconomyPolicyService.DailyRewardPolicy(100L, 3, 280.0D, 1.0D);

        assertEquals(
            EconomyOperationService.OperationFailure.ALREADY_CLAIMED,
            EconomyPolicyService.evaluateDailyClaimPolicy(true, 1000.0D, 1000.0D, rewardPolicy)
        );
    }

    @Test
    void dailyClaimPolicyFailsWhenPrimaryTreasuryCannotCoverReward() {
        EconomyPolicyService.DailyRewardPolicy rewardPolicy = new EconomyPolicyService.DailyRewardPolicy(100L, 3, 280.0D, 1.0D);

        assertEquals(
            EconomyOperationService.OperationFailure.INSUFFICIENT_FUNDS,
            EconomyPolicyService.evaluateDailyClaimPolicy(false, 200.0D, 1000.0D, rewardPolicy)
        );
    }

    @Test
    void dailyClaimPolicyFailsWhenSecondaryTreasuryCannotCoverReward() {
        EconomyPolicyService.DailyRewardPolicy rewardPolicy = new EconomyPolicyService.DailyRewardPolicy(100L, 7, 320.0D, 2.0D);

        assertEquals(
            EconomyOperationService.OperationFailure.INSUFFICIENT_FUNDS,
            EconomyPolicyService.evaluateDailyClaimPolicy(false, 1000.0D, 1.0D, rewardPolicy)
        );
    }

    @Test
    void dailyClaimPolicyPassesWhenTreasuryCoversBothRewards() {
        EconomyPolicyService.DailyRewardPolicy rewardPolicy = new EconomyPolicyService.DailyRewardPolicy(100L, 7, 320.0D, 2.0D);

        assertNull(EconomyPolicyService.evaluateDailyClaimPolicy(false, 1000.0D, 10.0D, rewardPolicy));
    }
}
