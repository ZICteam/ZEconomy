package io.zicteam.zeconomy.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zicteam.zeconomy.ZEconomy;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EconomyStateServicesTest {
    private ExtraEconomyData previousData;

    @BeforeEach
    void setUp() {
        previousData = ZEconomy.EXTRA_DATA;
        ZEconomy.EXTRA_DATA = new ExtraEconomyData();
    }

    @AfterEach
    void tearDown() {
        ZEconomy.EXTRA_DATA = previousData;
    }

    @Test
    void bankMutationServiceUpdatesReadServiceBalancesAndHourlyPayout() {
        UUID playerId = UUID.randomUUID();

        assertEquals(25.0D, EconomyStateMutationService.addBankDeposit(playerId, "z_coin", 25.0D));
        assertEquals(60.0D, EconomyStateMutationService.addBankDeposit(playerId, "z_coin", 35.0D));
        EconomyStateMutationService.setBankLastHourlyPayout(playerId, 777L);

        assertEquals(60.0D, EconomySnapshotReadService.bankDeposited(playerId, "z_coin"));
        assertEquals(777L, EconomySnapshotReadService.bankLastHourlyPayout(playerId, 0L));
        assertEquals(60.0D, EconomySnapshotReadService.bankAccountDeposits(playerId).get("z_coin"));
    }

    @Test
    void vaultMutationAndPinFlowAppearInPlayerSnapshot() {
        UUID playerId = UUID.randomUUID();

        assertTrue(EconomyStateMutationService.setVaultPin(playerId, "1234"));
        assertEquals(15.0D, EconomyStateMutationService.addVaultBalance(playerId, "b_coin", 15.0D));

        EconomySnapshotReadService.PlayerSnapshot snapshot = EconomySnapshotReadService.player(playerId);

        assertTrue(EconomySnapshotReadService.hasVaultPin(playerId));
        assertTrue(EconomySnapshotReadService.matchesVaultPin(playerId, "1234"));
        assertEquals(15.0D, EconomySnapshotReadService.vaultBalance(playerId, "b_coin"));
        assertTrue(snapshot.hasVaultPin());
        assertEquals(15.0D, snapshot.vaultBalances().get("b_coin"));
    }

    @Test
    void dailyClaimMutationUpdatesDailyReadStateAndPlayerSnapshot() {
        UUID playerId = UUID.randomUUID();
        EconomyPolicyService.DailyRewardPolicy rewardPolicy = new EconomyPolicyService.DailyRewardPolicy(250L, 4, 290.0D, 1.0D);

        ExtraEconomyData.DailyClaimResult result = EconomyStateMutationService.applyDailyClaim(playerId, rewardPolicy);
        EconomySnapshotReadService.PlayerSnapshot snapshot = EconomySnapshotReadService.player(playerId);

        assertTrue(result.success());
        assertEquals(4, result.streak());
        assertEquals(250L, EconomySnapshotReadService.dailyLastClaimDay(playerId));
        assertEquals(4, EconomySnapshotReadService.dailyStreak(playerId));
        assertEquals(4, snapshot.dailyStreak());
    }

    @Test
    void freshPlayerSnapshotStartsEmpty() {
        EconomySnapshotReadService.PlayerSnapshot snapshot = EconomySnapshotReadService.player(UUID.randomUUID());

        assertEquals(0, snapshot.pendingMail());
        assertEquals(0, snapshot.dailyStreak());
        assertFalse(snapshot.hasVaultPin());
        assertTrue(snapshot.bankBalances().isEmpty());
        assertTrue(snapshot.vaultBalances().isEmpty());
    }
}
