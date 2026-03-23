package io.zicteam.zeconomy.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class EconomyStateRootTest {
    @Test
    void writeAndReadPreserveBankRateAndDailyState() {
        UUID playerId = UUID.randomUUID();
        EconomyStateRoot root = new EconomyStateRoot();

        root.addBankDepositState(playerId, "z_coin", 125.0D);
        root.setBankLastHourlyPayout(playerId, 12345L);
        root.setRate("z_coin", "b_coin", 4.5D);
        root.applyDailyClaimState(playerId, 200L, 3, 280.0D, 1.0D);

        CompoundTag serialized = root.write();

        EconomyStateRoot restored = new EconomyStateRoot();
        restored.read(serialized);

        assertEquals(125.0D, restored.getDeposited(playerId, "z_coin"));
        assertEquals(125.0D, restored.getTotalDeposited("z_coin"));
        assertEquals(12345L, restored.getBankLastHourlyPayout(playerId, 0L));
        assertEquals(4.5D, restored.getRate("z_coin", "b_coin"));
        assertEquals(3, restored.getDailyStreak(playerId));
        assertEquals(200L, restored.getDailyLastClaimDay(playerId));
    }

    @Test
    void serverSpendableUsesCachedReservedTotalsAcrossReadWrite() {
        UUID playerA = UUID.randomUUID();
        UUID playerB = UUID.randomUUID();
        EconomyStateRoot root = new EconomyStateRoot();

        root.addBankDepositState(playerA, "z_coin", 40.0D);
        root.addBankDepositState(playerB, "z_coin", 60.0D);

        assertEquals(150.0D, root.getServerSpendable(250.0D, "z_coin"));

        EconomyStateRoot restored = new EconomyStateRoot();
        restored.read(root.write());

        assertEquals(150.0D, restored.getServerSpendable(250.0D, "z_coin"));
    }

    @Test
    void interestSweepSchedulingAllowsOnlyElapsedWindows() {
        EconomyStateRoot root = new EconomyStateRoot();

        assertTrue(root.beginInterestSweep(100L));
        assertFalse(root.beginInterestSweep(110L));
        assertTrue(root.beginInterestSweep(130L));
    }
}
