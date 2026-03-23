package io.zicteam.zeconomy.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExtraEconomyDataTest {
    @Test
    void serializeAndDeserializePreserveFacadeState() {
        UUID playerId = UUID.randomUUID();
        ExtraEconomyData data = new ExtraEconomyData();

        data.setRate("z_coin", "b_coin", 4.25D);
        data.addBankDepositState(playerId, "z_coin", 80.0D);
        data.setBankLastHourlyPayout(playerId, 321L);
        data.applyDailyClaimState(playerId, 500L, 5, 300.0D, 1.0D);
        data.markExported(999L);
        data.recordTransaction("TRANSFER", playerId, null, "z_coin", 12.0D, 0.5D, "note");

        ExtraEconomyData restored = ExtraEconomyData.deserialize(data.serialize());

        assertEquals(4.25D, restored.getRate("z_coin", "b_coin"));
        assertEquals(80.0D, restored.getDeposited(playerId, "z_coin"));
        assertEquals(321L, restored.getBankLastHourlyPayout(playerId, 0L));
        assertEquals(5, restored.getDailyStreak(playerId));
        assertEquals(500L, restored.getDailyLastClaimDay(playerId));
        assertEquals(999L, restored.getLastExportEpochSec());
        assertEquals(1, restored.getLogCount());
        assertEquals("TRANSFER", restored.getRecentLogs(1).get(0).type());
    }

    @Test
    void saveAndLoadRoundTripPreserveDataOnDisk(@TempDir Path tempDir) {
        UUID playerId = UUID.randomUUID();
        Path file = tempDir.resolve("extra_data.nbt");
        ExtraEconomyData data = new ExtraEconomyData();

        data.setRate("b_coin", "z_coin", 0.5D);
        data.addVaultDepositState(playerId, "b_coin", 15.0D);
        data.recordTransaction("BANK_DEPOSIT", playerId, null, "b_coin", 15.0D, 0.0D, "");
        data.save(file);

        assertTrue(Files.exists(file));

        ExtraEconomyData loaded = ExtraEconomyData.load(file);

        assertEquals(0.5D, loaded.getRate("b_coin", "z_coin"));
        assertEquals(15.0D, loaded.getVaultBalance(playerId, "b_coin"));
        assertEquals(1, loaded.getLogCount());
    }

    @Test
    void beginInterestSweepUsesWindowedScheduling() {
        ExtraEconomyData data = new ExtraEconomyData();

        assertTrue(data.beginInterestSweep(100L));
        assertFalse(data.beginInterestSweep(120L));
        assertTrue(data.beginInterestSweep(130L));
    }
}
